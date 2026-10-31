package com.fsosuke.montevideohub.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.fsosuke.montevideohub.BuildConfig
import com.fsosuke.montevideohub.data.Http
import com.fsosuke.montevideohub.data.objects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException

data class UpdateInfo(
    val versionCode: Int,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

/**
 * In-App-Updates wie bei ReVanced Manager / NewPipe:
 * neueste GitHub-Release abfragen → APK laden → per PackageInstaller installieren.
 * Releases heißen "b<Build-Nummer>" und werden von der CI erstellt.
 */
object Updater {
    private val latestUrl = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"

    val installedBuild: Int get() = BuildConfig.VERSION_CODE

    /** null = schon aktuell. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val json = Http.json(
            Request.Builder()
                .url(latestUrl)
                .header("Accept", "application/vnd.github+json")
                .build()
        )
        val code = json.optString("tag_name").removePrefix("b").toIntOrNull()
            ?: return@withContext null
        if (code <= installedBuild) return@withContext null
        val asset = json.optJSONArray("assets").objects()
            .firstOrNull { it.optString("name").endsWith(".apk") }
            ?: return@withContext null
        UpdateInfo(
            versionCode = code,
            title = json.optString("name").ifBlank { "Build $code" },
            notes = json.optString("body"),
            apkUrl = asset.getString("browser_download_url"),
            sizeBytes = asset.optLong("size"),
        )
    }

    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply {
                mkdirs()
                listFiles()?.forEach { it.delete() }
            }
            val file = File(dir, "MonteVideoHub-b${info.versionCode}.apk")
            Http.client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download fehlgeschlagen (HTTP ${response.code})")
                val body = response.body ?: throw IOException("Leere Antwort")
                val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            done += n
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            file
        }

    /** Darf diese App APKs installieren? (Einstellung „Unbekannte Apps installieren“) */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    suspend fun install(context: Context, apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, UpdateReceiver::class.java).setPackage(context.packageName),
                flags,
            )
            session.commit(pending.intentSender)
        }
    }
}
