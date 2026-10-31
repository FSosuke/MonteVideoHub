package com.fsosuke.montevideohub.ui

import java.util.Locale

fun formatDuration(seconds: Long): String {
    if (seconds < 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%d:%02d", m, s)
}

/** „vor 3 Std.“ usw. */
fun formatAge(epochMs: Long): String {
    if (epochMs <= 0) return ""
    val s = ((System.currentTimeMillis() - epochMs) / 1000).coerceAtLeast(0)
    return when {
        s < 3600 -> "vor ${(s / 60).coerceAtLeast(1)} Min."
        s < 86_400 -> "vor ${s / 3600} Std."
        s < 86_400L * 30 -> "vor ${s / 86_400} Tg."
        s < 86_400L * 365 -> "vor ${s / (86_400L * 30)} Mon."
        else -> "vor ${s / (86_400L * 365)} J."
    }
}

fun formatMs(ms: Long): String = formatDuration(ms / 1000)

fun formatViews(views: Long): String = when {
    views < 0 -> ""
    views >= 1_000_000 -> String.format(Locale.GERMANY, "%.1f Mio. Aufrufe", views / 1_000_000.0)
    views >= 1_000 -> String.format(Locale.GERMANY, "%.1f Tsd. Aufrufe", views / 1_000.0)
    else -> "$views Aufrufe"
}
