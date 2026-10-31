package com.fsosuke.montevideohub.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DownloadFormat(val label: String) { AUDIO("Audio (.m4a)"), VIDEO("Video (.mp4)") }

enum class Region(val label: String, val language: String, val country: String) {
    JP("日本 (Japan)", "ja", "JP"),
    DE("Deutschland", "de", "DE"),
}

data class AppSettings(
    val videoMode: Boolean = true,
    val maxVideoHeight: Int = 1080,
    val sponsorBlock: Boolean = true,
    /** SAF-Ordner (content://…/tree/…) für Downloads, null = noch nicht gewählt */
    val downloadTreeUri: String? = null,
    val askFolderEachTime: Boolean = true,
    /** Japanisches YouTube als Standard (Trends, Suche, Empfehlungen) */
    val region: Region = Region.JP,
    val autoUpdateCheck: Boolean = true,
)

/** App-Einstellungen, gespeichert in SharedPreferences, beobachtbar als StateFlow. */
object Settings {
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val value: AppSettings get() = _state.value

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        // Version 2: Region auf Japan umstellen (vorher Deutschland als Standard)
        if (prefs.getInt("version", 1) < 2) {
            prefs.edit().putString("region", Region.JP.name).putInt("version", 2).apply()
        }
        val d = AppSettings()
        _state.value = AppSettings(
            videoMode = prefs.getBoolean("video_mode", d.videoMode),
            maxVideoHeight = prefs.getInt("max_video_height", d.maxVideoHeight),
            sponsorBlock = prefs.getBoolean("sponsor_block", d.sponsorBlock),
            downloadTreeUri = prefs.getString("download_tree_uri", null),
            askFolderEachTime = prefs.getBoolean("ask_folder", d.askFolderEachTime),
            region = enumOr(prefs.getString("region", null), d.region),
            autoUpdateCheck = prefs.getBoolean("auto_update", d.autoUpdateCheck),
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val s = transform(_state.value)
        _state.value = s
        prefs.edit()
            .putBoolean("video_mode", s.videoMode)
            .putInt("max_video_height", s.maxVideoHeight)
            .putBoolean("sponsor_block", s.sponsorBlock)
            .putString("download_tree_uri", s.downloadTreeUri)
            .putBoolean("ask_folder", s.askFolderEachTime)
            .putString("region", s.region.name)
            .putBoolean("auto_update", s.autoUpdateCheck)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}
