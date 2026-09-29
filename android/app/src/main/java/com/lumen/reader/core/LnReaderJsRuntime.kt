package com.lumen.reader.core

import android.content.Context
import java.io.File

/**
 * LNReader plugin disk storage.
 * Evaluation is performed by [LnReaderSourceAdapter] (Rhino + host bindings).
 */
class LnReaderJsRuntime(private val context: Context) {
    private val pluginsDir: File
        get() = File(context.filesDir, "plugins").also { if (!it.exists()) it.mkdirs() }

    fun listPlugins(): List<File> =
        pluginsDir.listFiles()?.filter { it.extension == "js" }?.toList().orEmpty()

    fun pluginPath(id: String): File {
        val safe = id.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(pluginsDir, "$safe.js")
    }

    fun savePlugin(id: String, js: String) {
        pluginPath(id).writeText(js)
    }

    fun hasPlugin(id: String): Boolean = pluginPath(id).exists() && pluginPath(id).length() > 20

    fun readPlugin(id: String): String? {
        val f = pluginPath(id)
        return if (f.exists()) f.readText() else null
    }
}
