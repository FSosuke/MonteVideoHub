package com.fsosuke.montevideohub.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Ein gemeinsamer HTTP-Client für Suche, Stream-Auflösung UND den Player.
 * Wichtig für niconico: Das Cookie `domand_bid` aus der Freigabe-Anfrage muss
 * auch bei den HLS-Segmenten mitgeschickt werden – daher ein geteilter CookieJar.
 */
object Http {
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    // User-Agents passend zum YouTube-Client, der den Stream-Link erzeugt hat (Parameter `c=`).
    private const val YT_ANDROID_UA =
        "com.google.android.youtube/19.28.35 (Linux; U; Android 15; DE) gzip"
    private const val YT_ANDROID_VR_UA =
        "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
    private const val YT_IOS_UA =
        "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)"

    private val cookies = mutableListOf<Cookie>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(this@Http.cookies) {
                for (new in cookies) {
                    this@Http.cookies.removeAll {
                        it.name == new.name && it.domain == new.domain && it.path == new.path
                    }
                    this@Http.cookies.add(new)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            synchronized(cookies) {
                val now = System.currentTimeMillis()
                cookies.removeAll { it.expiresAt < now }
                cookies.filter { it.matches(url) }
            }
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            val url = request.url
            val builder = request.newBuilder()
            if (request.header("User-Agent") == null) {
                val ua = if (url.host.endsWith("googlevideo.com")) {
                    when (url.queryParameter("c")?.uppercase()) {
                        "ANDROID", "ANDROID_MUSIC" -> YT_ANDROID_UA
                        "ANDROID_VR" -> YT_ANDROID_VR_UA
                        "IOS" -> YT_IOS_UA
                        else -> USER_AGENT
                    }
                } else {
                    USER_AGENT
                }
                builder.header("User-Agent", ua)
            }
            if (url.host.endsWith("nicovideo.jp") && request.header("Referer") == null) {
                builder.header("Referer", "https://www.nicovideo.jp/")
                builder.header("Origin", "https://www.nicovideo.jp")
            }
            chain.proceed(builder.build())
        }
        .build()

    /** Blockierender JSON-Request – nur aus Dispatchers.IO aufrufen. */
    fun json(request: Request): JSONObject =
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} von ${request.url.host}")
            }
            JSONObject(body)
        }
}
