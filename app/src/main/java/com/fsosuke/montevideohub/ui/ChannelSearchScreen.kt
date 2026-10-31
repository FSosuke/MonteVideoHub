package com.fsosuke.montevideohub.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fsosuke.montevideohub.data.Subscription
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow
import java.util.Locale

/** YouTube-Kanäle suchen und abonnieren. */
@Composable
fun ChannelSearchScreen(
    state: ChannelSearchState,
    subscriptions: List<Subscription>,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onSubscribe: (Subscription) -> Unit,
    onUnsubscribe: (Subscription) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Navy900)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Schließen", tint = Snow) }
            Text("YouTube-Kanal hinzufügen", color = MontedioYellow, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = { Text("Kanalname, z. B. YOASOBI") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MontedioYellow, cursorColor = MontedioYellow),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(color = MontedioYellow, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.error?.let { item { Text("Fehler: $it", color = MaterialTheme.colorScheme.error) } }
                    items(state.results, key = { it.key }) { sub ->
                        val subscribed = subscriptions.any { it.key == sub.key }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChannelAvatar(sub.name, sub.avatar, Modifier.size(48.dp))
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(sub.name, color = Snow, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (sub.subscribers >= 0) {
                                    Text(
                                        formatSubscribers(sub.subscribers),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (subscribed) {
                                OutlinedButton(onClick = { onUnsubscribe(sub) }) { Text("Abonniert", color = Snow) }
                            } else {
                                Button(
                                    onClick = { onSubscribe(sub) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MontedioYellow, contentColor = Navy900),
                                ) { Text("Abonnieren", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSubscribers(n: Long): String = when {
    n >= 1_000_000 -> String.format(Locale.GERMANY, "%.1f Mio. Abonnenten", n / 1_000_000.0)
    n >= 1_000 -> String.format(Locale.GERMANY, "%.1f Tsd. Abonnenten", n / 1_000.0)
    else -> "$n Abonnenten"
}
