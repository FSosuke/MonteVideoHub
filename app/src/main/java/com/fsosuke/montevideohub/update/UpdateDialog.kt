package com.fsosuke.montevideohub.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy800
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow
import java.util.Locale

@Composable
fun UpdateDialog(state: UpdateUiState, installedBuild: Int, onInstall: () -> Unit, onDismiss: () -> Unit) {
    val info = state.available ?: return
    val downloading = state.progress != null
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        containerColor = Navy800,
        title = { Text("Update verfügbar", fontWeight = FontWeight.Black, color = MontedioYellow) },
        text = {
            Column {
                Text("${info.title}\nInstalliert: Build $installedBuild", color = Snow)
                if (info.sizeBytes > 0) {
                    Text(
                        String.format(Locale.GERMANY, "%.1f MB", info.sizeBytes / 1_048_576.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (info.notes.isNotBlank()) {
                    Text(
                        info.notes.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
                state.progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p },
                        color = MontedioYellow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onInstall,
                enabled = !downloading,
                colors = ButtonDefaults.buttonColors(containerColor = MontedioYellow, contentColor = Navy900),
            ) { Text(if (downloading) "Lädt …" else "Installieren", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            if (!downloading) TextButton(onClick = onDismiss) { Text("Später", color = Snow) }
        },
    )
}
