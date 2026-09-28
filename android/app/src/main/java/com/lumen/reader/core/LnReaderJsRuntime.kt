package com.lumen.reader.core

import android.content.Context
import java.io.File

/**
 * LNReader-level plugin storage + evaluation shell.
 * Plugins are downloaded as .js into filesDir/plugins/.
 * Full QuickJS/Rhino evaluation can be plugged in here; catalog today uses site URL
 * from the plugin manifest the same way LNReader indexes sites.
 */
class LnReaderJsRuntime(private val context: Context) {
    private val pluginsDir: File
        get() = File(context.filesDir, "plugins").also { if (!it.exists()) it.mkdirs() }

    fun listPlugins(): List<File> =
        pluginsDir.listFiles()?.filter { it.extension == "js" }?.toList().orEmpty()

    fun pluginPath(id: String): File = File(pluginsDir, "$id.js")

    fun savePlugin(id: String, js: String) {
        pluginPath(id).writeText(js)
    }

    fun hasPlugin(id: String): Boolean = pluginPath(id).exists()

    /**
     * Evaluate a plugin function name if a JS engine is available.
     * Returns null when engine is not linked — callers fall back to site scrape.
     */
    fun evalPluginFunction(id: String, functionName: String, argsJson: String): String? {
        val file = pluginPath(id)
        if (!file.exists()) return null
        return null
    }
}
