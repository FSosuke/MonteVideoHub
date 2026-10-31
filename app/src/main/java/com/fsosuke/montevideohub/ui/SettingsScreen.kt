package com.fsosuke.montevideohub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fsosuke.montevideohub.data.AppSettings
import com.fsosuke.montevideohub.data.NicoUser
import com.fsosuke.montevideohub.data.Region
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow

@Composable
fun SettingsScreen(
    settings: AppSettings,
    nicoUser: NicoUser?,
    nicoLoggedIn: Boolean,
    installedBuild: Int,
    contentPadding: PaddingValues,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onRegion: (Region) -> Unit,
    onNicoLogin: () -> Unit,
    onNicoLogout: () -> Unit,
    onChangeFolder: () -> Unit,
    onClearHistory: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        item { ScreenTitle("Einstellungen") }

        item {
            Group("Konten") {
                Label("niconico")
                if (nicoLoggedIn) {
                    Text(
                        "Angemeldet als ${nicoUser?.nickname ?: "…"}" + if (nicoUser?.premium == true) " · Premium" else "",
                        color = Snow,
                    )
                    Hint("Login-Videos und (mit Premium) höhere Qualität sind freigeschaltet.")
                    OutlinedButton(onClick = onNicoLogout, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Abmelden", color = Snow)
                    }
                } else {
                    Hint("Für Videos, die nur angemeldet abspielbar sind, und Premium-Qualität.")
                    YellowButton("Bei niconico anmelden", onNicoLogin)
                }
                Label("YouTube", top = 16.dp)
                Hint("Anmeldung mit Google-Konto ist noch nicht eingebaut.")
            }
        }

        item {
            Group("Wiedergabe") {
                SwitchRow("Video anzeigen", "Aus = nur Audio, spart Daten", settings.videoMode) { v ->
                    onChange { it.copy(videoMode = v) }
                }
                Label("Maximale Videoqualität", top = 12.dp)
                ChipRow(listOf(360, 480, 720, 1080), settings.maxVideoHeight, { "${it}p" }) { h ->
                    onChange { it.copy(maxVideoHeight = h) }
                }
                SwitchRow("SponsorBlock", "Überspringt bei YouTube Werbeblöcke & Nicht-Musik-Teile", settings.sponsorBlock) { v ->
                    onChange { it.copy(sponsorBlock = v) }
                }
            }
        }

        item {
            Group("Downloads") {
                Label("Speicherort")
                Text(DownloadsViewModel.folderLabel(settings.downloadTreeUri), color = Snow)
                YellowButton(if (settings.downloadTreeUri == null) "Ordner wählen" else "Ordner ändern", onChangeFolder)
                SwitchRow("Jedes Mal nach Ordner fragen", "Vor jedem Download den Speicherort wählen", settings.askFolderEachTime) { v ->
                    onChange { it.copy(askFolderEachTime = v) }
                }
            }
        }

        item {
            Group("Inhalte") {
                Label("Region für YouTube (Trends & Suche)")
                ChipRow(Region.entries, settings.region, { it.label }, onRegion)
                OutlinedButton(onClick = onClearHistory, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Verlauf „Zuletzt gespielt“ löschen", color = Snow)
                }
            }
        }

        item {
            Group("Updates") {
                SwitchRow("Beim Start nach Updates suchen", "Installiert: Build $installedBuild", settings.autoUpdateCheck) { v ->
                    onChange { it.copy(autoUpdateCheck = v) }
                }
                YellowButton("Jetzt nach Updates suchen", onCheckUpdate)
            }
        }

        item {
            Group("Über") {
                Text("MonteVideoHub · Build $installedBuild", color = Snow)
                Hint("Privates Fan-Projekt im Design von Montedio Yamagata. Schrift: Noto Sans JP (SIL OFL).")
                Hint("Quellcode: github.com/FSosuke/MonteVideoHub")
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Navy800.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MontedioYellow, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Label(text: String, top: androidx.compose.ui.unit.Dp = 8.dp) {
    Text(text, fontWeight = FontWeight.Bold, color = Snow, modifier = Modifier.padding(top = top, bottom = 2.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun YellowButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MontedioYellow, contentColor = Navy900),
        modifier = Modifier.padding(top = 8.dp),
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Snow, fontWeight = FontWeight.Bold)
            Hint(subtitle)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Navy900,
                checkedTrackColor = MontedioYellow,
            ),
        )
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Navy900.copy(alpha = 0.6f),
                    labelColor = Snow,
                    selectedContainerColor = MontedioYellow,
                    selectedLabelColor = Navy900,
                ),
            )
        }
    }
}
