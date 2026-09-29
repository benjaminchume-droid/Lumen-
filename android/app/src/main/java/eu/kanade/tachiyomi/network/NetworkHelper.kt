package eu.kanade.tachiyomi.network

import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Host NetworkHelper for Keiyoushi / Mihon extension parsers. */
class NetworkHelper {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val key = url.host
            val list = cookieStore.getOrPut(key) { mutableListOf() }
            synchronized(list) {
                for (c in cookies) {
                    list.removeAll { it.name == c.name }
                    list.add(c)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = cookieStore[url.host] ?: return emptyList()
            synchronized(list) {
                return list.filter { it.matches(url) }
            }
        }
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        if (original.header("User-Agent").isNullOrBlank()) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        }
        chain.proceed(builder.build())
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(10, 2, TimeUnit.MINUTES))
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .cookieJar(cookieJar)
        .addInterceptor(userAgentInterceptor)
        .build()

    val cloudflareClient: OkHttpClient = client.newBuilder()
        .addInterceptor(CloudflareInterceptor())
        .build()
}
