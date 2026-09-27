package com.lumen.reader.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dalvik.system.PathClassLoader

/**
 * Mihon-style extension discovery for installed Keiyoushi packages.
 * Discovers packages with tachiyomi.extension metadata and builds PathClassLoader.
 * Live listings use CatalogService + homeUrl until full Source API stubs are shipped.
 */
class ExtensionHost(private val context: Context) {

    data class LoadedExt(
        val pkg: String,
        val name: String,
        val sourceDir: String,
        val classNames: List<String>,
        val classLoader: PathClassLoader?
    )

    fun findInstalledExtensions(): List<LoadedExt> {
        val pm = context.packageManager
        val out = ArrayList<LoadedExt>()
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }
        for (pi in packages) {
            val app = pi.applicationInfo ?: continue
            val meta = app.metaData ?: continue
            val cls = meta.getString("tachiyomi.extension.class")
                ?: meta.getString("tachiyomi.extension.factory")
                ?: continue
            val name = try {
                pm.getApplicationLabel(app).toString()
            } catch (_: Exception) {
                pi.packageName
            }
            val classes = cls.split(';').map { it.trim() }.filter { it.isNotBlank() }.map {
                if (it.startsWith(".")) pi.packageName + it else it
            }
            val loader = try {
                PathClassLoader(app.sourceDir, context.classLoader)
            } catch (_: Exception) {
                null
            }
            out.add(
                LoadedExt(
                    pkg = pi.packageName,
                    name = name,
                    sourceDir = app.sourceDir.orEmpty(),
                    classNames = classes,
                    classLoader = loader
                )
            )
        }
        return out
    }

    fun isTachiyomiExtensionInstalled(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        return try {
            val pi = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            }
            val meta = pi.applicationInfo?.metaData
            meta?.containsKey("tachiyomi.extension.class") == true ||
                meta?.containsKey("tachiyomi.extension.factory") == true
        } catch (_: Exception) {
            false
        }
    }
}
