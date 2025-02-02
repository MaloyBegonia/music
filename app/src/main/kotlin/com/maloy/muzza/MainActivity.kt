package com.maloy.muzza

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.defaultShimmerTheme
import com.maloy.compose.persist.PersistMap
import com.maloy.compose.persist.PersistMapOwner
import com.maloy.innertube.Innertube
import com.maloy.innertube.models.bodies.BrowseBody
import com.maloy.innertube.requests.playlistPage
import com.maloy.innertube.requests.song
import com.maloy.muzza.enums.ColorPaletteMode
import com.maloy.muzza.enums.ColorPaletteName
import com.maloy.muzza.enums.ThumbnailRoundness
import com.maloy.muzza.service.PlayerService
import com.maloy.muzza.ui.components.BottomSheetMenu
import com.maloy.muzza.ui.components.LocalMenuState
import com.maloy.muzza.ui.components.rememberBottomSheetState
import com.maloy.muzza.ui.screens.albumRoute
import com.maloy.muzza.ui.screens.artistRoute
import com.maloy.muzza.ui.screens.home.HomeScreen
import com.maloy.muzza.ui.screens.player.Player
import com.maloy.muzza.ui.screens.playlistRoute
import com.maloy.muzza.ui.styling.Appearance
import com.maloy.muzza.ui.styling.Dimensions
import com.maloy.muzza.ui.styling.LocalAppearance
import com.maloy.muzza.ui.styling.colorPaletteOf
import com.maloy.muzza.ui.styling.dynamicColorPaletteOf
import com.maloy.muzza.ui.styling.typographyOf
import com.maloy.muzza.utils.applyFontPaddingKey
import com.maloy.muzza.utils.asMediaItem
import com.maloy.muzza.utils.colorPaletteModeKey
import com.maloy.muzza.utils.colorPaletteNameKey
import com.maloy.muzza.utils.forcePlay
import com.maloy.muzza.utils.getEnum
import com.maloy.muzza.utils.intent
import com.maloy.muzza.utils.isAtLeastAndroid6
import com.maloy.muzza.utils.isAtLeastAndroid8
import com.maloy.muzza.utils.preferences
import com.maloy.muzza.utils.thumbnailRoundnessKey
import com.maloy.muzza.utils.useSystemFontKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity(), PersistMapOwner {

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) {
                this@MainActivity.binder = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    private var binder by mutableStateOf<PlayerService.Binder?>(null)

    override lateinit var persistMap: PersistMap

    override fun onStart() {
        super.onStart()
        bindService(intent<PlayerService>(), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        persistMap = lastCustomNonConfigurationInstance as? PersistMap ?: PersistMap()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val launchedFromNotification = intent?.extras?.getBoolean("expandPlayerBottomSheet") == true

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val isSystemInDarkTheme = isSystemInDarkTheme()

            var appearance by rememberSaveable(
                isSystemInDarkTheme,
                stateSaver = Appearance.Companion
            ) {
                with(preferences) {
                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.PureBlack)
                    val colorPaletteMode = getEnum(colorPaletteModeKey, ColorPaletteMode.System)
                    val thumbnailRoundness =
                        getEnum(thumbnailRoundnessKey, ThumbnailRoundness.Heavy)

                    val useSystemFont = getBoolean(useSystemFontKey, false)
                    val applyFontPadding = getBoolean(applyFontPaddingKey, false)

                    val colorPalette =
                        colorPaletteOf(colorPaletteName, colorPaletteMode, isSystemInDarkTheme)

                    setSystemBarAppearance(colorPalette.isDark)

                    mutableStateOf(
                        Appearance(
                            colorPalette = colorPalette,
                            typography = typographyOf(colorPalette.text, useSystemFont, applyFontPadding),
                            thumbnailShape = thumbnailRoundness.shape()
                        )
                    )
                }
            }

            DisposableEffect(binder, isSystemInDarkTheme) {
                var bitmapListenerJob: Job? = null

                fun setDynamicPalette(colorPaletteMode: ColorPaletteMode) {
                    val isDark =
                        colorPaletteMode == ColorPaletteMode.Dark || (colorPaletteMode == ColorPaletteMode.System && isSystemInDarkTheme)

                    binder?.setBitmapListener { bitmap: Bitmap? ->
                        if (bitmap == null) {
                            val colorPalette =
                                colorPaletteOf(
                                    ColorPaletteName.Dynamic,
                                    colorPaletteMode,
                                    isSystemInDarkTheme
                                )

                            setSystemBarAppearance(colorPalette.isDark)

                            appearance = appearance.copy(
                                colorPalette = colorPalette,
                                typography = appearance.typography.copy(colorPalette.text)
                            )

                            return@setBitmapListener
                        }

                        bitmapListenerJob = coroutineScope.launch(Dispatchers.IO) {
                            dynamicColorPaletteOf(bitmap, isDark)?.let {
                                withContext(Dispatchers.Main) {
                                    setSystemBarAppearance(it.isDark)
                                }
                                appearance = appearance.copy(
                                    colorPalette = it,
                                    typography = appearance.typography.copy(it.text)
                                )
                            }
                        }
                    }
                }

                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        when (key) {
                            colorPaletteNameKey, colorPaletteModeKey -> {
                                val colorPaletteName =
                                    sharedPreferences.getEnum(
                                        colorPaletteNameKey,
                                        ColorPaletteName.Dynamic
                                    )

                                val colorPaletteMode =
                                    sharedPreferences.getEnum(
                                        colorPaletteModeKey,
                                        ColorPaletteMode.System
                                    )

                                if (colorPaletteName == ColorPaletteName.Dynamic) {
                                    setDynamicPalette(colorPaletteMode)
                                } else {
                                    bitmapListenerJob?.cancel()
                                    binder?.setBitmapListener(null)

                                    val colorPalette = colorPaletteOf(
                                        colorPaletteName,
                                        colorPaletteMode,
                                        isSystemInDarkTheme
                                    )

                                    setSystemBarAppearance(colorPalette.isDark)

                                    appearance = appearance.copy(
                                        colorPalette = colorPalette,
                                        typography = appearance.typography.copy(colorPalette.text),
                                    )
                                }
                            }

                            thumbnailRoundnessKey -> {
                                val thumbnailRoundness =
                                    sharedPreferences.getEnum(key, ThumbnailRoundness.Heavy)

                                appearance = appearance.copy(
                                    thumbnailShape = thumbnailRoundness.shape()
                                )
                            }

                            useSystemFontKey, applyFontPaddingKey -> {
                                val useSystemFont = sharedPreferences.getBoolean(useSystemFontKey, false)
                                val applyFontPadding = sharedPreferences.getBoolean(applyFontPaddingKey, false)

                                appearance = appearance.copy(
                                    typography = typographyOf(appearance.colorPalette.text, useSystemFont, applyFontPadding),
                                )
                            }
                        }
                    }

                with(preferences) {
                    registerOnSharedPreferenceChangeListener(listener)

                    val colorPaletteName = getEnum(colorPaletteNameKey, ColorPaletteName.PureBlack)
                    if (colorPaletteName == ColorPaletteName.Dynamic) {
                        setDynamicPalette(getEnum(colorPaletteModeKey, ColorPaletteMode.System))
                    }

                    onDispose {
                        bitmapListenerJob?.cancel()
                        binder?.setBitmapListener(null)
                        unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }
            }

            val rippleTheme =
                remember(appearance.colorPalette.text, appearance.colorPalette.isDark) {
                    object : RippleTheme {
                        @Composable
                        override fun defaultColor(): Color = RippleTheme.defaultRippleColor(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )

                        @Composable
                        override fun rippleAlpha(): RippleAlpha = RippleTheme.defaultRippleAlpha(
                            contentColor = appearance.colorPalette.text,
                            lightTheme = !appearance.colorPalette.isDark
                        )
                    }
                }

            val shimmerTheme = remember {
                defaultShimmerTheme.copy(
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 800,
                            easing = LinearEasing,
                            delayMillis = 250,
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    shaderColors = listOf(
                        Color.Unspecified.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.50f),
                        Color.Unspecified.copy(alpha = 0.25f),
                    ),
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appearance.colorPalette.background0)
            ) {
                val density = LocalDensity.current
                val windowsInsets = WindowInsets.systemBars
                val bottomDp = with(density) { windowsInsets.getBottom(density).toDp() }

                val playerBottomSheetState = rememberBottomSheetState(
                    dismissedBound = 0.dp,
                    collapsedBound = Dimensions.collapsedPlayer + bottomDp,
                    expandedBound = maxHeight,
                )

                val playerAwareWindowInsets by remember(bottomDp, playerBottomSheetState.value) {
                    derivedStateOf {
                        val bottom = playerBottomSheetState.value.coerceIn(bottomDp, playerBottomSheetState.collapsedBound)

                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(bottom = bottom))
                    }
                }

                CompositionLocalProvider(
                    LocalAppearance provides appearance,
                    LocalIndication provides rememberRipple(bounded = true),
                    LocalRippleTheme provides rippleTheme,
                    LocalShimmerTheme provides shimmerTheme,
                    LocalPlayerServiceBinder provides binder,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    HomeScreen(
                        onPlaylistUrl = { url ->
                            onNewIntent(Intent.parseUri(url, 0))
                        }
                    )

                    Player(
                        layoutState = playerBottomSheetState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )
                }

                DisposableEffect(binder?.player) {
                    val player = binder?.player ?: return@DisposableEffect onDispose { }

                    if (player.currentMediaItem == null) {
                        if (!playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.dismiss()
                        }
                    } else {
                        if (playerBottomSheetState.isDismissed) {
                            if (launchedFromNotification) {
                                intent.replaceExtras(Bundle())
                                playerBottomSheetState.expand(tween(700))
                            } else {
                                playerBottomSheetState.collapse(tween(700))
                            }
                        }
                    }

                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null) {
                                if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) {
                                    playerBottomSheetState.expand(tween(500))
                                } else {
                                    playerBottomSheetState.collapse(tween(700))
                                }
                            }
                        }
                    }

                    player.addListener(listener)

                    onDispose { player.removeListener(listener) }
                }
            }
        }

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val uri = intent.getStringExtra("android.intent.extra.TEXT")?.toUri() ?: return

        Toast.makeText(this, "${"Muzza "}${getString(R.string.opening_url)}", Toast.LENGTH_LONG).show()

        lifecycleScope.launch(Dispatchers.IO) {
            when (val path = uri.pathSegments.firstOrNull()) {
                "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                    val browseId = "VL$playlistId"

                    if (playlistId.startsWith("OLAK5uy_")) {
                        Innertube.playlistPage(BrowseBody(browseId = browseId))?.getOrNull()?.let {
                            it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                albumRoute.ensureGlobal(browseId)
                            }
                        }
                    } else {
                        playlistRoute.ensureGlobal(browseId)
                    }
                }

                "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                    artistRoute.ensureGlobal(channelId)
                }

                else -> when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtu.be" -> path
                    else -> null
                }?.let { videoId ->
                    Innertube.song(videoId)?.getOrNull()?.let { song ->
                        val binder = snapshotFlow { binder }.filterNotNull().first()
                        withContext(Dispatchers.Main) {
                            binder.player.forcePlay(song.asMediaItem)
                        }
                    }
                }
            }
        }
    }

    override fun onRetainCustomNonConfigurationInstance() = persistMap

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            persistMap.clear()
        }

        super.onDestroy()
    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        if (!isAtLeastAndroid6) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }

        if (!isAtLeastAndroid8) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

}

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }

val LocalPlayerAwareWindowInsets = staticCompositionLocalOf<WindowInsets> { TODO() }