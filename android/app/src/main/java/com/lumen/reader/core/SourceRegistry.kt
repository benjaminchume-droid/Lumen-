package com.lumen.reader.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry: maps source id → ContentSource.
 * Binding order per entry:
 *  1. MihonSourceAdapter (installed APK, reflection)
 *  2. LnReaderSourceAdapter (JS plugin eval)
 *  3. HostFallbackSource (site scrape)
 *
 * UI and downloads must only go through this registry.
 */
class SourceRegistry(private val context: Context) {

    private val sources = ConcurrentHashMap<String, ContentSource>()
    private val healthMap = ConcurrentHashMap<String, SourceHealth>()
    private val _bound = MutableStateFlow<List<ExtensionManifest>>(emptyList())
    val bound: StateFlow<List<ExtensionManifest>> = _bound

    fun get(id: String): ContentSource? = sources[id]

    fun all(): List<ContentSource> = sources.values.toList()

    fun health(id: String): SourceHealth? = healthMap[id]

    fun listHealth(): List<SourceHealth> = healthMap.values.toList()

    /**
     * Bind installed index entries. Prefer Mihon/LN runtimes; scrape is last resort.
     */
    suspend fun bindInstalled(entries: List<IndexEntry>) = withContext(Dispatchers.IO) {
        val boundList = ArrayList<ExtensionManifest>()
        for (entry in entries) {
            val existing = sources[entry.id]
            if (existing != null) {
                boundList.add(existing.manifest)
                continue
            }
            val source = resolveSource(entry) ?: continue
            sources[entry.id] = source
            healthMap.putIfAbsent(entry.id, SourceHealth(sourceId = entry.id))
            boundList.add(source.manifest)
        }
        _bound.value = boundList
    }

    private fun resolveSource(entry: IndexEntry): ContentSource? {
        // 1) Mihon extension APK
        if (entry.kind == MediaKind.MANGA && !entry.pkg.isNullOrBlank()) {
            val mihon = MihonSourceAdapter.tryCreate(context, entry)
            if (mihon != null) {
                // Probe: if popular returns anything, keep; else still keep for details
                return mihon
            }
        }
        // 2) LNReader JS plugin
        if (entry.kind == MediaKind.NOVEL) {
            val ln = LnReaderSourceAdapter.tryCreate(context, entry)
            if (ln != null) return ln
        }
        // 3) Host fallback only if we have a site URL
        if (!entry.site.isNullOrBlank() && entry.site!!.startsWith("http")) {
            return HostFallbackSource(entry)
        }
        return null
    }

    suspend fun popularFrom(
        sourceId: String,
        page: Int = 1
    ): SearchPage = withContext(Dispatchers.IO) {
        val src = sources[sourceId] ?: return@withContext SearchPage(emptyList(), false)
        runTracked(sourceId) { src.getPopular(page) }
    }

    /**
     * Progressive popular feed: yield one source at a time.
     */
    suspend fun progressivePopular(
        sourceIds: List<String>,
        perSource: Int = 16,
        onBatch: suspend (sourceId: String, items: List<SeriesMeta>) -> Unit
    ) {
        for (id in sourceIds) {
            val page = try {
                popularFrom(id, 1)
            } catch (t: Throwable) {
                recordFailure(id, t.message)
                SearchPage(emptyList(), false)
            }
            val batch = page.items.take(perSource)
            if (batch.isNotEmpty()) onBatch(id, batch)
        }
    }

    suspend fun details(sourceId: String, seriesUrl: String): SeriesMeta? =
        withContext(Dispatchers.IO) {
            val src = sources[sourceId] ?: return@withContext null
            runTracked(sourceId) { src.getDetails(seriesUrl) }
        }

    suspend fun chapters(sourceId: String, seriesUrl: String): List<ChapterMeta> =
        withContext(Dispatchers.IO) {
            val src = sources[sourceId] ?: return@withContext emptyList()
            runTracked(sourceId) { src.getChapters(seriesUrl) }
        }

    suspend fun content(sourceId: String, chapterUrl: String): ContentPayload? =
        withContext(Dispatchers.IO) {
            val src = sources[sourceId] ?: return@withContext null
            runTracked(sourceId) { src.getContent(chapterUrl) }
        }

    private suspend fun <T> runTracked(sourceId: String, block: suspend () -> T): T {
        return try {
            val result = block()
            recordSuccess(sourceId)
            result
        } catch (t: Throwable) {
            recordFailure(sourceId, t.message)
            throw t
        }
    }

    private fun recordSuccess(sourceId: String) {
        val h = healthMap[sourceId] ?: SourceHealth(sourceId)
        healthMap[sourceId] = h.copy(
            lastSuccessAt = System.currentTimeMillis(),
            successCount = h.successCount + 1,
            lastError = null
        )
    }

    private fun recordFailure(sourceId: String, error: String?) {
        val h = healthMap[sourceId] ?: SourceHealth(sourceId)
        healthMap[sourceId] = h.copy(
            lastFailureAt = System.currentTimeMillis(),
            failureCount = h.failureCount + 1,
            lastError = error?.take(200)
        )
    }
}
