package com.fsosuke.montevideohub.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fsosuke.montevideohub.ui.components.MontedioBackground
import com.fsosuke.montevideohub.update.UpdateDialog
import com.fsosuke.montevideohub.update.UpdateViewModel
import kotlinx.coroutines.flow.merge

@Composable
fun MonteVideoHubApp(
    searchVm: SearchViewModel = viewModel(),
    playerVm: PlayerViewModel = viewModel(),
    updateVm: UpdateViewModel = viewModel(),
) {
    val update by updateVm.state.collectAsStateWithLifecycle()
    val search by searchVm.state.collectAsStateWithLifecycle()
    val player by playerVm.state.collectAsStateWithLifecycle()
    val resolving by playerVm.resolving.collectAsStateWithLifecycle()
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        merge(playerVm.messages, updateVm.messages).collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    BackHandler(enabled = playerOpen) { playerOpen = false }

    Box(Modifier.fillMaxSize()) {
        // Ohne Ergebnisse ist das Wappen gut sichtbar, mit Liste stärker abgedunkelt.
        MontedioBackground(dim = if (search.results.isEmpty() && !search.loading) 0.25f else 0.78f)

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (player.current != null) {
                    MiniPlayer(
                        state = player,
                        onOpen = { playerOpen = true },
                        onTogglePlay = playerVm::togglePlayPause,
                        onNext = playerVm::next,
                    )
                } else {
                    Spacer(Modifier.navigationBarsPadding())
                }
            },
        ) { padding ->
            SearchScreen(
                state = search,
                resolving = resolving,
                contentPadding = padding,
                onQueryChange = searchVm::onQueryChange,
                onSearch = searchVm::search,
                onFilter = searchVm::setFilter,
                onMusicOnly = searchVm::setMusicOnly,
                onPlay = playerVm::play,
                onPlayNext = playerVm::playNext,
                onEnqueue = playerVm::enqueue,
                updateAvailable = update.available != null,
                checkingUpdate = update.checking,
                onUpdateClick = {
                    if (update.available != null) updateVm.openDialog() else updateVm.check(manual = true)
                },
            )
        }

        AnimatedVisibility(
            visible = playerOpen && player.current != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            PlayerScreen(state = player, vm = playerVm, onClose = { playerOpen = false })
        }
    }

    if (update.showDialog) {
        UpdateDialog(
            state = update,
            installedBuild = updateVm.installedBuild,
            onInstall = updateVm::install,
            onDismiss = updateVm::dismiss,
        )
    }
}
