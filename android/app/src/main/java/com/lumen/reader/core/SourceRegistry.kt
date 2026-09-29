package com.lumen.reader.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry: maps source id → ContentSource.
 * Binding: Mihon → LN → HostFallback
 * Chapters: merge primary + site fallback so lists are complete (no missing chapters).
 */
class SourceRegistry(private val context: Context) {

    private val sources = ConcurrentHashMap<String, ContentSource>()
    private val fallbacks = ConcurrentHashMap<String, HostFallbackSource>()
    private val entriesById = ConcurrentHashMap<String, IndexEntry>()
    private val healthMap = ConcurrentHashMap<String, SourceHealth>()
    private val _bound = MutableStateFlow<List<ExtensionManifest>>(emptyList())
    val bound: StateFlow<List<ExtensionManifest>> = _bound

    fun get(id: String): ContentSource? = sources[id]

    fun all(): List<ContentSource> = sources.values.toList()

    fun health(id: String): SourceHealth? = healthMap[id]

    fun listHealth(): List<SourceHealth> = healthMap.values.toList()

    suspend fun bindInstalled(entries: List<IndexEntry>) = withContext(Dispatchers.IO) {
        val boundList = ArrayList<ExtensionManifest>()
        for (entry in entries) {
            entriesById[entry.id] = entry
            val existing = sources[entry.id]
            if (existing != null) {
                boundList.add(existing.manifest)
                ensureFallback(entry)
                continue
            }
            val source = resolveSource(entry) ?: continue
            sources[entry.id] = source
            ensureFallback(entry)
            healthMap.putIfAbsent(entry.id, SourceHealth(sourceId = entry.id))
            boundList.add(source.manifest)
        }
        _bound.value = boundList
    }

    private fun ensureFallback(entry: IndexEntry) {
        if (!entry.site.isNullOrBlank() && entry.site!!.startsWith("http")) {
            fallbacks.putIfAbsent(entry.id, HostFallbackSource(entry))
        }
    }

    private fun resolveSource(entry: IndexEntry): ContentSource? {
        if (entry.kind == MediaKind.MANGA && !entry.pkg.isNullOrBlank()) {
            MihonSourceAdapter.tryCreate(context, entry)?.let { return it }
        }
        if (entry.kind == MediaKind.NOVEL) {
            LnReaderSourceAdapter.tryCreate(context, entry)?.let { return it }
        }
        if (!entry.site.isNullOrBlank() && entry.site!!.startsWith("http")) {
            return HostFallbackSource(entry)
        }
        return null
    }

    suspend fun popularFrom(sourceId: String, page: Int = 1): SearchPage =
        withContext(Dispatchers.IO) {
            val src = sources[sourceId] ?: return@withContext SearchPage(emptyList(), false)
            val primary = runCatching {
                withTimeoutOrNull(12_000L) { runTracked(sourceId) { src.getPopular(page) } }
            }.getOrNull()
            if (primary != null && primary.items.isNotEmpty()) return@withContext primary

            val fb = fallbacks[sourceId]
            if (fb != null && fb !== src) {
                return@withContext runCatching {
                    withTimeoutOrNull(12_000L) { fb.getPopular(page) }
                }.getOrNull() ?: SearchPage(emptyList(), false)
            }
            primary ?: SearchPage(emptyList(), false)
        }

    /**
     * Progressive feed with limited parallelism (faster Home without waiting on all).
     */
    suspend fun progressivePopular(
        sourceIds: List<String>,
        perSource: Int = 16,
        maxConcurrent: Int = 3,
        onBatch: suspend (sourceId: String, items: List<SeriesMeta>) -> Unit
    ) = coroutineScope {
        val sem = Semaphore(maxConcurrent)
        sourceIds.map { id ->
            async(Dispatchers.IO) {
                sem.withPermit {
                    val page = try {
                        popularFrom(id, 1)
                    } catch (t: Throwable) {
                        recordFailure(id, t.message)
                        SearchPage(emptyList(), false)
                    }
                    val batch = page.items.take(perSource)
                    if (batch.isNotEmpty()) {
                        onBatch(id, batch)
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun details(sourceId: String, seriesUrl: String): SeriesMeta? =
        withContext(Dispatchers.IO) {
            val src = sources[sourceId]
            val primary = src?.let {
                runCatching { withTimeoutOrNull(15_000L) { runTracked(sourceId) { it.getDetails(seriesUrl) } } }.getOrNull()
            }
            if (primary != null && !primary.title.isNullOrBlank() && primary.title != "Unknown") {
                return@withContext primary
            }
            fallbacks[sourceId]?.let { fb ->
                runCatching { withTimeoutOrNull(15_000L) { fb.getDetails(seriesUrl) } }.getOrNull()
            } ?: primary
        }

    /**
     * Merge chapters from primary runtime + host fallback so lists are complete.
     */
    suspend fun chapters(sourceId: String, seriesUrl: String): List<ChapterMeta> =
        withContext(Dispatchers.IO) {
            val lists = ArrayList<List<ChapterMeta>>()
            val src = sources[sourceId]
            if (src != null) {
                val a = runCatching {
                    withTimeoutOrNull(20_000L) { runTracked(sourceId) { src.getChapters(seriesUrl) } }
                }.getOrNull().orEmpty()
                if (a.isNotEmpty()) lists.add(a)
            }
            val fb = fallbacks[sourceId]
            if (fb != null && fb !== src) {
                val b = runCatching {
                    withTimeoutOrNull(20_000L) { fb.getChapters(seriesUrl) }
                }.getOrNull().orEmpty()
                if (b.isNotEmpty()) lists.add(b)
            }
            if (lists.isEmpty()) return@withContext emptyList()
            if (lists.size == 1) return@withContext lists[0].sortedBy { it.number }

            val priorities = listOf(
                SourcePriority(sourceId, priority = 10, qualityScore = 90),
                SourcePriority(sourceId + "_web", priority = 5, qualityScore = 70)
            )
            val tagged = lists.mapIndexed { idx, list ->
                if (idx == 0) list
                else list.map { it.copy(sourceId = sourceId + "_web") }
            }
            Aggregator.mergeChapters(tagged, priorities).map { it.primary.copy(sourceId = sourceId) }
                .sortedBy { it.number }
        }

    suspend fun content(sourceId: String, chapterUrl: String): ContentPayload? =
        withContext(Dispatchers.IO) {
            val src = sources[sourceId]
            val primary = src?.let {
                runCatching {
                    withTimeoutOrNull(25_000L) { runTracked(sourceId) { it.getContent(chapterUrl) } }
                }.getOrNull()
            }
            val nonEmpty = when (primary) {
                is ContentPayload.Images -> primary.pages.isNotEmpty()
                is ContentPayload.Text -> primary.blocks.isNotEmpty()
                is ContentPayload.Epub -> true
                null -> false
            }
            if (nonEmpty) return@withContext primary
            fallbacks[sourceId]?.let { fb ->
                runCatching { withTimeoutOrNull(25_000L) { fb.getContent(chapterUrl) } }.getOrNull()
            } ?: primary
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
