package com.lumen.reader.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class DownloadStatus {
    QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
}

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val seriesTitle: String,
    val chapterName: String,
    val chapterUrl: String,
    val sourceId: String,
    val totalPages: Int = 0,
    val pagesDone: Int = 0,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L,
    val speedBps: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val error: String? = null
) {
    val progressPercent: Int
        get() = when {
            totalPages > 0 -> ((pagesDone.toFloat() / totalPages) * 100).toInt().coerceIn(0, 100)
            bytesTotal > 0 -> ((bytesDone.toFloat() / bytesTotal) * 100).toInt().coerceIn(0, 100)
            else -> 0
        }
}

/**
 * Simple Mihon-style chapter download queue:
 * - enqueue chapters
 * - pause / resume / cancel
 * - progress % + speed
 *
 * Page fetching is stubbed until a concrete Source adapter streams pages;
 * the queue/UI contract is production-ready.
 */
class DownloadQueue(private val scope: CoroutineScope) {
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val jobs = ConcurrentHashMap<String, Job>()
    private val paused = ConcurrentHashMap.newKeySet<String>()

    fun enqueue(
        seriesTitle: String,
        chapterName: String,
        chapterUrl: String,
        sourceId: String,
        totalPages: Int = 20
    ): String {
        val task = DownloadTask(
            seriesTitle = seriesTitle,
            chapterName = chapterName,
            chapterUrl = chapterUrl,
            sourceId = sourceId,
            totalPages = totalPages.coerceAtLeast(1)
        )
        _tasks.update { it + task }
        start(task.id)
        return task.id
    }

    fun pause(id: String) {
        paused.add(id)
        jobs[id]?.cancel()
        jobs.remove(id)
        update(id) { it.copy(status = DownloadStatus.PAUSED) }
    }

    fun resume(id: String) {
        paused.remove(id)
        val task = _tasks.value.find { it.id == id } ?: return
        if (task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.CANCELLED) return
        start(id)
    }

    fun cancel(id: String) {
        paused.remove(id)
        jobs[id]?.cancel()
        jobs.remove(id)
        update(id) { it.copy(status = DownloadStatus.CANCELLED) }
    }

    private fun start(id: String) {
        if (jobs.containsKey(id)) return
        val job = scope.launch(Dispatchers.IO) {
            update(id) { it.copy(status = DownloadStatus.RUNNING) }
            try {
                var task = _tasks.value.find { it.id == id } ?: return@launch
                var done = task.pagesDone
                val total = task.totalPages.coerceAtLeast(1)
                val t0 = System.currentTimeMillis()
                while (done < total && isActive && !paused.contains(id)) {
                    // Simulate sequential page fetch (data-saver friendly concurrency = 1)
                    delay(180)
                    done++
                    val elapsed = (System.currentTimeMillis() - t0).coerceAtLeast(1)
                    val bytes = done * 85_000L
                    val speed = (bytes * 1000L) / elapsed
                    update(id) {
                        it.copy(
                            pagesDone = done,
                            bytesDone = bytes,
                            bytesTotal = total * 85_000L,
                            speedBps = speed,
                            status = DownloadStatus.RUNNING
                        )
                    }
                }
                if (paused.contains(id)) {
                    update(id) { it.copy(status = DownloadStatus.PAUSED) }
                } else if (done >= total) {
                    update(id) { it.copy(status = DownloadStatus.COMPLETED, pagesDone = total) }
                }
            } catch (e: Exception) {
                if (!paused.contains(id)) {
                    update(id) { it.copy(status = DownloadStatus.FAILED, error = e.message) }
                }
            } finally {
                jobs.remove(id)
            }
        }
        jobs[id] = job
    }

    private fun update(id: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }
}
