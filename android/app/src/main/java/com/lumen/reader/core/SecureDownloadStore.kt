package com.lumen.reader.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Offline chapter downloads with AES-GCM (Android Keystore).
 * Idempotent: same chapter id is not re-downloaded if payload exists.
 */
class SecureDownloadStore(private val context: Context) {

    private val root = File(context.filesDir, "downloads").also { it.mkdirs() }
    private val covers = File(context.filesDir, "covers").also { it.mkdirs() }
    private val meta = context.getSharedPreferences("lumen_downloads", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "lumen_dl_v1"
        if (ks.containsAlias(alias)) {
            return (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return iv + ct
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, 12)
        val ct = blob.copyOfRange(12, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    fun isDownloaded(chapterId: String): Boolean {
        val safe = chapterId.hashCode().toString()
        return File(root, "$safe.bin").exists()
    }

    fun saveChapter(chapterId: String, seriesId: String, title: String, pages: List<String>): Boolean {
        if (isDownloaded(chapterId) && pages.isNotEmpty()) return true
        if (pages.isEmpty()) return false
        return try {
            val payload = JSONObject()
                .put("chapterId", chapterId)
                .put("seriesId", seriesId)
                .put("title", title)
                .put("pages", JSONArray(pages))
                .toString()
                .toByteArray(Charsets.UTF_8)
            val safe = chapterId.hashCode().toString()
            File(root, "$safe.bin").writeBytes(encrypt(payload))
            val ids = meta.getStringSet("ids", emptySet())!!.toMutableSet()
            ids.add(chapterId)
            meta.edit().putStringSet("ids", ids)
                .putString("meta_$safe", JSONObject()
                    .put("chapterId", chapterId)
                    .put("seriesId", seriesId)
                    .put("title", title)
                    .put("count", pages.size)
                    .toString())
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun loadChapter(chapterId: String): List<String>? {
        val safe = chapterId.hashCode().toString()
        val f = File(root, "$safe.bin")
        if (!f.exists()) return null
        return try {
            val json = JSONObject(String(decrypt(f.readBytes()), Charsets.UTF_8))
            val arr = json.getJSONArray("pages")
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun listDownloaded(): List<Pair<String, String>> {
        val ids = meta.getStringSet("ids", emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            val safe = id.hashCode().toString()
            val m = meta.getString("meta_$safe", null) ?: return@mapNotNull null
            val o = JSONObject(m)
            o.optString("title") to id
        }
    }

    fun saveCover(seriesId: String, bytes: ByteArray): File? {
        return try {
            val f = File(covers, "${seriesId.hashCode()}.img")
            if (f.exists() && f.length() > 0) return f
            f.writeBytes(bytes)
            f
        } catch (_: Exception) {
            null
        }
    }

    fun coverFile(seriesId: String): File? {
        val f = File(covers, "${seriesId.hashCode()}.img")
        return f.takeIf { it.exists() && it.length() > 0 }
    }
}
