package eu.kanade.tachiyomi.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Host NetworkHelper used by many Keiyoushi parsers. */
class NetworkHelper {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    val cloudflareClient: OkHttpClient = client
}
