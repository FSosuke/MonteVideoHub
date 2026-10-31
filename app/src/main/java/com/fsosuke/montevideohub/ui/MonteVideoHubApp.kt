package com.fsosuke.montevideohub.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fsosuke.montevideohub.data.History
import com.fsosuke.montevideohub.data.NicoAccount
import com.fsosuke.montevideohub.data.Settings
import com.fsosuke.montevideohub.download.DownloadState
import com.fsosuke.montevideohub.ui.components.MontedioBackground
import com.fsosuke.montevideohub.ui.theme.MontedioYellow
import com.fsosuke.montevideohub.ui.theme.Navy900
import com.fsosuke.montevideohub.ui.theme.Snow
import com.fsosuke.montevideohub.update.UpdateDialog
import com.fsosuke.montevideohub.update.UpdateViewModel
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Start", Icons.Rounded.Home),
    SEARCH("Suche", Icons.Rounded.Search),
    SUBS("Abos", Icons.Rounded.Subscriptions),
    DOWNLOADS("Downloads", Icons.Rounded.Download),
    SETTINGS("Einstellungen", Icons.Rounded.Settings),
}

@Composable
fun MonteVideoHubApp(
    searchVm: SearchViewModel = viewModel(),
    playerVm: PlayerViewModel = viewModel(),
    updateVm: UpdateViewModel = viewModel(),
    homeVm: HomeViewModel = viewModel(),
    downloadsVm: DownloadsViewModel = viewModel(),
    subsVm: SubscriptionsViewModel = viewModel(),
) {
    val subscriptions by subsVm.subscriptions.collectAsStateWithLifecycle()
    val feed by subsVm.feed.collectAsStateWithLifecycle()
    val selectedSub by subsVm.selected.collectAsStateWithLifecycle()
    val channelSearch by subsVm.search.collectAsStateWithLifecycle()
    var channelSearchOpen by rememberSaveable { mutableStateOf(false) }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        subsVm.importFrom(uri)
    }
    val launchImport = {
        importPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/json", "application/octet-stream"))
    }
    val update by updateVm.state.collectAsStateWithLifecycle()
    val search by searchVm.state.collectAsStateWithLifecycle()
    val home by homeVm.state.collectAsStateWithLifecycle()
    val history by History.items.collectAsStateWithLifecycle()
    val player by playerVm.state.collectAsStateWithLifecycle()
    val exoPlayer by playerVm.player.collectAsStateWithLifecycle()
    val resolving by playerVm.resolving.collectAsStateWithLifecycle()
    val tasks by downloadsVm.tasks.collectAsStateWithLifecycle()
    val entries by downloadsVm.entries.collectAsStateWithLifecycle()
    val settings by Settings.state.collectAsStateWithLifecycle()
    val nicoUser by NicoAccount.user.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var nicoLogin by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Ordnerauswahl für Downloads (Storage Access Framework)
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        downloadsVm.onFolderPicked(uri)
    }
    LaunchedEffect(Unit) { downloadsVm.pickFolder.collect { folderPicker.launch(it) } }

    LaunchedEffect(Unit) {
        merge(playerVm.messages, updateVm.messages, downloadsVm.messages, subsVm.messages).collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    BackHandler(enabled = playerOpen && !fullscreen && !nicoLogin) { playerOpen = false }
    BackHandler(enabled = !playerOpen && !nicoLogin && !channelSearchOpen && tab != Tab.HOME) { tab = Tab.HOME }

    // Wechsel auf „nur Audio“ oder Player leer → Vollbild verlassen
    LaunchedEffect(player.videoMode, player.current == null) {
        if (!player.videoMode || player.current == null) fullscreen = false
    }

    val onUpdateClick = {
        if (update.available != null) updateVm.openDialog() else updateVm.check(manual = true)
    }

    Box(Modifier.fillMaxSize()) {
        val dim = when (tab) {
            Tab.SEARCH -> if (search.results.isEmpty() && !search.loading) 0.25f else 0.78f
            Tab.HOME -> 0.72f
            else -> 0.85f
        }
        MontedioBackground(dim = dim)

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    if (player.current != null) {
                        MiniPlayer(
                            state = player,
                            onOpen = { playerOpen = true },
                            onTogglePlay = playerVm::togglePlayPause,
                            onNext = playerVm::next,
                        )
                    }
                    NavigationBar(containerColor = Navy900.copy(alpha = 0.97f)) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = {
                                    val running = t == Tab.DOWNLOADS && tasks.any { it.state != DownloadState.FAILED }
                                    val updateDot = t == Tab.SETTINGS && update.available != null
                                    BadgedBox(badge = { if (running || updateDot) Badge(containerColor = MontedioYellow) }) {
                                        Icon(t.icon, contentDescription = t.label)
                                    }
                                },
                                label = { Text(t.label, maxLines = 1, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Navy900,
                                    indicatorColor = MontedioYellow,
                                    selectedTextColor = MontedioYellow,
                                    unselectedIconColor = Snow.copy(alpha = 0.7f),
                                    unselectedTextColor = Snow.copy(alpha = 0.7f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            when (tab) {
                Tab.HOME -> HomeScreen(
                    state = home,
                    history = history,
                    resolving = resolving,
                    contentPadding = padding,
                    onFilter = homeVm::setFilter,
                    onRefresh = homeVm::refresh,
                    onPlay = playerVm::play,
                    onPlayNext = playerVm::playNext,
                    onEnqueue = playerVm::enqueue,
                    onDownload = downloadsVm::request,
                    onToggleSubscribe = subsVm::toggle,
                    feed = feed.items,
                    onShowSubscriptions = { tab = Tab.SUBS },
                    header = { Header(update.available != null, update.checking, onUpdateClick) },
                )

                Tab.SUBS -> SubscriptionsScreen(
                    subscriptions = subscriptions,
                    feed = feed,
                    selected = selectedSub,
                    resolving = resolving,
                    contentPadding = padding,
                    onSelect = subsVm::select,
                    onUnsubscribe = subsVm::unsubscribe,
                    onRefresh = subsVm::refresh,
                    onAddChannel = { channelSearchOpen = true },
                    onImport = launchImport,
                    onPlay = playerVm::play,
                    onPlayNext = playerVm::playNext,
                    onEnqueue = playerVm::enqueue,
                    onDownload = downloadsVm::request,
                    onToggleSubscribe = subsVm::toggle,
                )

                Tab.SEARCH -> SearchScreen(
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
                    onDownload = downloadsVm::request,
                    onToggleSubscribe = subsVm::toggle,
                    updateAvailable = update.available != null,
                    checkingUpdate = update.checking,
                    onUpdateClick = onUpdateClick,
                )

                Tab.DOWNLOADS -> DownloadsScreen(
                    tasks = tasks,
                    entries = entries,
                    folder = settings.downloadTreeUri,
                    contentPadding = padding,
                    onChangeFolder = downloadsVm::changeFolder,
                    onPlay = playerVm::playLocal,
                    onDelete = downloadsVm::delete,
                    onRetry = downloadsVm::retry,
                    onCancel = downloadsVm::cancel,
                )

                Tab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    nicoUser = nicoUser,
                    nicoLoggedIn = nicoUser != null || NicoAccount.isLoggedIn,
                    installedBuild = updateVm.installedBuild,
                    contentPadding = padding,
                    onChange = Settings::update,
                    onRegion = { region ->
                        Settings.update { it.copy(region = region) }
                        NewPipe.setupLocalization(
                            Localization(region.language, region.country),
                            ContentCountry(region.country),
                        )
                        homeVm.refresh()
                    },
                    onNicoLogin = { nicoLogin = true },
                    onNicoLogout = {
                        NicoAccount.logout()
                        scope.launch { snackbar.showSnackbar("Bei niconico abgemeldet") }
                    },
                    onChangeFolder = downloadsVm::changeFolder,
                    onClearHistory = {
                        History.clear()
                        scope.launch { snackbar.showSnackbar("Verlauf gelöscht") }
                    },
                    onCheckUpdate = { updateVm.check(manual = true) },
                )
            }
        }

        AnimatedVisibility(
            visible = playerOpen && player.current != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            PlayerScreen(
                state = player,
                player = exoPlayer,
                vm = playerVm,
                showVideo = !fullscreen,
                onFullscreen = { fullscreen = true },
                onDownload = downloadsVm::request,
                onToggleSubscribe = subsVm::toggle,
                onClose = { playerOpen = false },
            )
        }

        if (fullscreen) {
            FullscreenVideo(state = player, player = exoPlayer, vm = playerVm, onExit = { fullscreen = false })
        }

        if (channelSearchOpen) {
            ChannelSearchScreen(
                state = channelSearch,
                subscriptions = subscriptions,
                onQuery = subsVm::onSearchQuery,
                onSearch = subsVm::searchChannels,
                onSubscribe = subsVm::subscribe,
                onUnsubscribe = subsVm::unsubscribe,
                onClose = { channelSearchOpen = false },
            )
        }

        if (nicoLogin) {
            NicoLoginScreen(
                onSessionFound = {
                    scope.launch {
                        val ok = NicoAccount.completeLogin()
                        nicoLogin = false
                        snackbar.showSnackbar(
                            if (ok) "Bei niconico angemeldet als ${NicoAccount.user.value?.nickname}"
                            else "Anmeldung fehlgeschlagen – bitte erneut versuchen"
                        )
                    }
                },
                onClose = { nicoLogin = false },
            )
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
