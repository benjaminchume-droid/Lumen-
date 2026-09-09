package com.lumen.reader.core

/**
 * Universal Chapter Aggregator
 * Merges chapters from multiple extensions into one always-up-to-date series.
 * If a single source has ≥95% coverage and highest quality → use only that source.
 */
object Aggregator {

    private fun chapterKey(n: Float): String = "%.4f".format(n).trimEnd('0').trimEnd('.')

    fun mergeChapters(
        lists: List<List<ChapterMeta>>,
        priorities: List<SourcePriority>
    ): List<AggregatedChapter> {
        val prioMap = priorities.associateBy { it.sourceId }
        val groups = mutableMapOf<String, MutableList<ChapterMeta>>()

        for (list in lists) {
            for (ch in list) {
                val key = chapterKey(ch.number)
                groups.getOrPut(key) { mutableListOf() }.add(ch)
            }
        }

        return groups.values.map { mirrors ->
            val scored = mirrors.map { m ->
                val p = prioMap[m.sourceId]
                val priority = p?.priority ?: 0
                val quality = p?.qualityScore ?: 50
                val recency = m.uploadedAt ?: 0L
                val score = priority * 1000.0 + quality + recency / 1e12
                Triple(m, score, quality)
            }.sortedByDescending { it.second }

            val best = scored.first()
            AggregatedChapter(
                number = best.first.number,
                name = best.first.name,
                primary = best.first,
                mirrors = scored.map { it.first },
                qualityScore = best.third
            )
        }.sortedBy { it.number }
    }

    fun shouldUseSingleSource(
        chapters: List<AggregatedChapter>,
        priorities: List<SourcePriority>
    ): String? {
        if (chapters.isEmpty()) return null
        val coverage = mutableMapOf<String, Int>()
        for (ch in chapters) {
            for (m in ch.mirrors) {
                coverage[m.sourceId] = (coverage[m.sourceId] ?: 0) + 1
            }
        }
        val total = chapters.size
        var bestId: String? = null
        var bestQuality = -1
        for ((id, count) in coverage) {
            val ratio = count.toFloat() / total
            val q = priorities.find { it.sourceId == id }?.qualityScore ?: 50
            if (ratio >= 0.95f && q >= bestQuality) {
                bestId = id
                bestQuality = q
            }
        }
        if (bestId != null) {
            val othersHigher = priorities.any { it.sourceId != bestId && it.qualityScore > bestQuality }
            if (!othersHigher) return bestId
        }
        return null
    }

    fun buildAggregatedSeries(
        id: String,
        title: String,
        kind: MediaKind,
        linked: List<LinkedSource>,
        chapterLists: List<List<ChapterMeta>>,
        priorities: List<SourcePriority>,
        meta: SeriesMeta? = null
    ): AggregatedSeries {
        val chapters = mergeChapters(chapterLists, priorities)
        val sole = shouldUseSingleSource(chapters, priorities)
        val finalChapters = if (sole != null) {
            chapters.map { ch ->
                val primary = ch.mirrors.find { it.sourceId == sole } ?: ch.primary
                ch.copy(
                    primary = primary,
                    mirrors = listOf(primary),
                    qualityScore = priorities.find { it.sourceId == sole }?.qualityScore ?: ch.qualityScore
                )
            }
        } else chapters

        return AggregatedSeries(
            id = id,
            title = title,
            kind = kind,
            coverUrl = meta?.coverUrl,
            description = meta?.description,
            genres = meta?.genres ?: emptyList(),
            status = meta?.status ?: "unknown",
            linkedSources = linked,
            chapters = finalChapters,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
}
