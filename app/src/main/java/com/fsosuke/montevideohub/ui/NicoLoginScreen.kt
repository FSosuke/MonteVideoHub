package com.fsosuke.montevideohub.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow

/**
 * niconico-Login in einer WebView (offizielle Login-Seite, inkl. 2-Faktor).
 * Sobald das Sitzungs-Cookie da ist, wird [onSessionFound] aufgerufen.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NicoLoginScreen(onSessionFound: () -> Unit, onClose: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var reported by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)

    Column(
        Modifier
            .fillMaxSize()
            .background(Navy900)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Schließen", tint = Snow) }
            Text("Bei niconico anmelden", color = MontedioYellow, fontWeight = FontWeight.Bold)
        }
        if (loading) LinearProgressIndicator(color = MontedioYellow, modifier = Modifier.fillMaxWidth())
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            val cookies = CookieManager.getInstance().getCookie("https://www.nicovideo.jp").orEmpty()
                            if (!reported && cookies.contains("user_session=")) {
                                reported = true
                                CookieManager.getInstance().flush()
                                onSessionFound()
                            }
                        }
                    }
                    loadUrl("https://account.nicovideo.jp/login?site=niconico&next_url=%2F")
                }
            },
            onRelease = { it.destroy() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
