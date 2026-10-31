package com.fsosuke.montevideohub.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fsosuke.montevideohub.R
import com.fsosuke.montevideohub.data.Source
import com.fsosuke.montevideohub.ui.theme.Mist
import com.fsosuke.montevideohub.ui.theme.Navy700
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow
import com.fsosuke.montevideohub.ui.theme.YouTubeRed

/** Das Montedio-Bild als Hintergrund, mit Abdunklung für Lesbarkeit. */
@Composable
fun MontedioBackground(dim: Float, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(dim, label = "backgroundDim")
    Box(modifier.fillMaxSize().background(Navy900)) {
        Image(
            painter = painterResource(R.drawable.bg_montedio),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Navy900.copy(alpha = (alpha * 0.9f).coerceIn(0f, 1f)),
                        0.5f to Navy900.copy(alpha = (alpha * 0.55f).coerceIn(0f, 1f)),
                        1f to Navy900.copy(alpha = (alpha + 0.3f).coerceIn(0f, 1f)),
                    )
                )
        )
    }
}

@Composable
fun Artwork(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    Box(
        modifier.clip(shape).background(Navy700),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Mist.copy(alpha = 0.5f))
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun SourceBadge(source: Source?, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (source) {
        Source.YOUTUBE -> Triple("YouTube", YouTubeRed, Color.White)
        Source.NICONICO -> Triple("ニコニコ", Snow, Color.Black)
        null -> return
    }
    Text(
        text = label,
        color = fg,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Normal,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
