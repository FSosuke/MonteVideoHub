package com.fsosuke.montevideohub.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    val available: UpdateInfo? = null,
    val showDialog: Boolean = false,
    /** null = kein Download aktiv */
    val progress: Float? = null,
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val installedBuild = Updater.installedBuild

    init {
        check(manual = false)
    }

    fun check(manual: Boolean) {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true) }
            try {
                val info = Updater.check()
                _state.update { it.copy(available = info, showDialog = info != null) }
                if (info == null && manual) _messages.emit("Du hast die neueste Version (Build $installedBuild)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (manual) _messages.emit("Update-Prüfung fehlgeschlagen: ${e.message}")
            } finally {
                _state.update { it.copy(checking = false) }
            }
        }
    }

    fun openDialog() = _state.update { it.copy(showDialog = it.available != null) }
    fun dismiss() = _state.update { it.copy(showDialog = false) }

    fun install() {
        val info = _state.value.available ?: return
        if (_state.value.progress != null) return
        val context = getApplication<Application>()
        if (!Updater.canInstall(context)) {
            _messages.tryEmit("Bitte „Unbekannte Apps installieren“ erlauben und erneut tippen")
            Updater.openInstallPermissionSettings(context)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(progress = 0f) }
            try {
                val apk = Updater.download(context, info) { p -> _state.update { it.copy(progress = p) } }
                Updater.install(context, apk)
                _state.update { it.copy(showDialog = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.emit("Update fehlgeschlagen: ${e.message}")
            } finally {
                _state.update { it.copy(progress = null) }
            }
        }
    }
}
