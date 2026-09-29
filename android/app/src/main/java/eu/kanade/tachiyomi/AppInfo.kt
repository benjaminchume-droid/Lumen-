package eu.kanade.tachiyomi

/** Host AppInfo stub referenced by some Keiyoushi / multisrc extensions. */
object AppInfo {
    fun getVersionName(): String = "0.1.8"
    fun getVersionCode(): Int = 180
    /** User-Agent suffix some sources append. */
    fun getUserAgent(): String =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
