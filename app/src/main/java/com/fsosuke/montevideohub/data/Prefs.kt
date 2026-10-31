package com.fsosuke.montevideohub.data

import android.content.Context

object Prefs {
    private const val FILE = "settings"
    private const val KEY_VIDEO_MODE = "video_mode"

    fun videoMode(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_VIDEO_MODE, true)

    fun setVideoMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_VIDEO_MODE, enabled).apply()
    }
}
