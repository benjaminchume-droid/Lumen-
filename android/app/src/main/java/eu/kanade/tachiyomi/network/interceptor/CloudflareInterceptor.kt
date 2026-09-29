package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Lightweight Cloudflare-aware interceptor.
 * Full JS challenge solving requires a WebView path; this handles cookie challenges
 * and retries with browser-like headers when CF challenge HTML is detected.
 */
class CloudflareInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)

        if (isCloudflareChallenge(response)) {
            response.close()
            val retry = request.newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .removeHeader("X-Requested-With")
                .build()
            response = chain.proceed(retry)
            if (isCloudflareChallenge(response)) {
                // Still challenged — surface as IOException so callers can fall back to scrape
                response.close()
                throw IOException("Cloudflare challenge for ${request.url.host}")
            }
        }
        return response
    }

    private fun isCloudflareChallenge(response: Response): Boolean {
        if (response.code !in listOf(403, 503)) return false
        val server = response.header("Server").orEmpty()
        if (server.contains("cloudflare", ignoreCase = true)) return true
        val bodyPeek = try {
            response.peekBody(4096).string()
        } catch (_: Exception) {
            ""
        }
        return bodyPeek.contains("cf-browser-verification", ignoreCase = true) ||
            bodyPeek.contains("challenge-platform", ignoreCase = true) ||
            bodyPeek.contains("Just a moment", ignoreCase = true) ||
            bodyPeek.contains("cf-challenge", ignoreCase = true)
    }
}
