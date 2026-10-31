package com.fsosuke.montevideohub.ui.components

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/** Zeigt das Videobild des Players (ohne eigene Bedienelemente – die baut die App selbst). */
@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(
    player: Player?,
    keepScreenOn: Boolean,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShutterBackgroundColor(Color.BLACK)
                setKeepContentOnPlayerReset(true)
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
            view.resizeMode = resizeMode
            view.keepScreenOn = keepScreenOn
        },
        onRelease = { it.player = null },
        modifier = modifier,
    )
}
