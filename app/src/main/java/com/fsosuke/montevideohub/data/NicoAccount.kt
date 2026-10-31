package com.fsosuke.montevideohub.data

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request

data class NicoUser(val id: String, val nickname: String, val premium: Boolean)

/**
 * niconico-Anmeldung: Login läuft in einer WebView auf account.nicovideo.jp,
 * danach wird nur das Sitzungs-Cookie `user_session` (app-privat) gespeichert.
 */
object NicoAccount {
    private lateinit var prefs: SharedPreferences
    private val _user = MutableStateFlow<NicoUser?>(null)
    val user: StateFlow<NicoUser?> = _user.asStateFlow()

    val isLoggedIn: Boolean get() = ::prefs.isInitialized && session() != null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("accounts", Context.MODE_PRIVATE)
        session()?.let { Http.setCookie("nicovideo.jp", "user_session", it) }
        _user.value = prefs.getString("nico_nickname", null)?.let {
            NicoUser(prefs.getString("nico_id", "").orEmpty(), it, prefs.getBoolean("nico_premium", false))
        }
    }

    private fun session(): String? = prefs.getString("nico_session", null)

    /** Aus den WebView-Cookies übernehmen. true = erfolgreich angemeldet. */
    suspend fun completeLogin(): Boolean {
        val cookies = CookieManager.getInstance().getCookie("https://www.nicovideo.jp").orEmpty()
        val session = cookies.split(";").map { it.trim() }
            .firstOrNull { it.startsWith("user_session=") }
            ?.substringAfter("=")
            ?.takeIf { it.isNotBlank() && it != "deleted" }
            ?: return false
        prefs.edit().putString("nico_session", session).apply()
        Http.setCookie("nicovideo.jp", "user_session", session)
        return runCatching { refreshUser() }.isSuccess
    }

    suspend fun refreshUser() = withContext(Dispatchers.IO) {
        val json = Http.json(
            Request.Builder()
                .url("https://nvapi.nicovideo.jp/v1/users/me")
                .header("X-Frontend-Id", "6")
                .header("X-Frontend-Version", "0")
                .build()
        )
        val u = json.getJSONObject("data").getJSONObject("user")
        val user = NicoUser(
            id = u.optString("id"),
            nickname = u.optString("nickname").ifBlank { "niconico" },
            premium = u.optBoolean("isPremium"),
        )
        prefs.edit()
            .putString("nico_id", user.id)
            .putString("nico_nickname", user.nickname)
            .putBoolean("nico_premium", user.premium)
            .apply()
        _user.value = user
    }

    fun logout() {
        prefs.edit().clear().apply()
        Http.removeCookie("user_session")
        CookieManager.getInstance().removeAllCookies(null)
        _user.value = null
    }
}
