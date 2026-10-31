package com.fsosuke.montevideohub.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fsosuke.montevideohub.data.SubscriptionStore
import com.fsosuke.montevideohub.data.Track

/** Menüeintrag „Kanal abonnieren“ / „Abo beenden“ – nur wenn der Kanal bekannt ist. */
@Composable
fun SubscribeMenuItem(track: Track, onToggle: () -> Unit) {
    if (track.channelId == null) return
    val subs by SubscriptionStore.items.collectAsState()
    val subscribed = subs.any { it.source == track.source && it.id == track.channelId }
    DropdownMenuItem(
        text = { Text(if (subscribed) "Abo beenden" else "Kanal abonnieren") },
        leadingIcon = {
            Icon(if (subscribed) Icons.Rounded.NotificationsActive else Icons.Rounded.PersonAddAlt, null)
        },
        onClick = onToggle,
    )
}
