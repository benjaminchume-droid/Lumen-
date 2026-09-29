package eu.kanade.tachiyomi.source.model

import okhttp3.Headers

class Page(
    val index: Int,
    val url: String = "",
    var imageUrl: String? = null,
    @Transient var headers: Headers? = null
) {
    var status: Int = QUEUE
    var progress: Int = 0

    companion object {
        const val QUEUE = 0
        const val LOAD_PAGE = 1
        const val DOWNLOAD_IMAGE = 2
        const val READY = 3
        const val ERROR = 4
    }
}
