package com.chesko.x_streampro.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chesko.x_streampro.R
import com.chesko.x_streampro.data.model.LiveStream
import com.chesko.x_streampro.data.model.UserSession
import com.chesko.x_streampro.ui.theme.XStreamProTheme
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.app.PictureInPictureUiState

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun PlayerScreen(
    session: UserSession,
    initialChannel: LiveStream,
    channels: List<LiveStream>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentChannel by remember { mutableStateOf(initialChannel) }
    
    fun getStreamUrl(channel: LiveStream): String {
        val baseUrl = if (session.baseUrl.endsWith("/")) {
            session.baseUrl.dropLast(1)
        } else {
            session.baseUrl
        }
        return "$baseUrl/live/${session.username}/${session.password}/${channel.streamId}.ts"
    }

    var showControls by remember { mutableStateOf(true) }
    var showSideList by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var errorOccurred by remember { mutableStateOf<String?>(null) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    var isInPipMode by remember { mutableStateOf(false) }
    val isInspectionMode = LocalInspectionMode.current

    val handleBack = {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    var networkSpeed by remember { mutableStateOf("0 KB/s") }
    var lastBytes by remember { mutableLongStateOf(0L) }

    val enterPip = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity?.enterPictureInPictureMode(params)
        }
    }

    DisposableEffect(activity) {
        val listener = Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
            if (info.isInPictureInPictureMode) {
                showControls = false
                showSideList = false
            }
        }
        
        val activityWithPip = activity as? androidx.core.app.OnPictureInPictureModeChangedProvider
        activityWithPip?.addOnPictureInPictureModeChangedListener(listener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity?.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(true)
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }

        onDispose {
            activityWithPip?.removeOnPictureInPictureModeChangedListener(listener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(false)
                        .build()
                )
            }
        }
    }

    val exoPlayer = remember {
        if (isInspectionMode) null
        else {
            val loadControl = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, 16))
                .setBufferDurationsMs(
                    5000,
                    15000,
                    1500,
                    2000
                )
                .build()

            ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build().apply {
                    prepare()
                    playWhenReady = true
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
        }
    }

    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        val trafficStats = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
        lastBytes = if (trafficStats == -1L) 0 else trafficStats
        
        while (true) {
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying

            val currentBytes = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid())
            if (lastBytes > 0 && currentBytes > lastBytes) {
                val diff = currentBytes - lastBytes
                networkSpeed = when {
                    diff > 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", diff / (1024.0 * 1024.0))
                    else -> "${diff / 1024} KB/s"
                }
            } else {
                networkSpeed = "0 KB/s"
            }
            lastBytes = currentBytes

            delay(1000)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (activity?.isInPictureInPictureMode == false) {
                        exoPlayer?.pause()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer?.pause()
                    exoPlayer?.playWhenReady = false
                    if (activity?.isFinishing == true) {
                        exoPlayer?.stop()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {

                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isFullscreen) {
        if (window != null && !isInPipMode) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    LaunchedEffect(currentChannel) {
        val player = exoPlayer ?: return@LaunchedEffect
        isBuffering = true
        errorOccurred = null
        val mediaItem = MediaItem.fromUri(getStreamUrl(currentChannel))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                
                if (playbackState == Player.STATE_ENDED) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    player.play()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (retryCount < 3) {
                    retryCount++
                    player.prepare()
                    player.play()
                } else {
                    errorOccurred = "Playback Error: ${error.localizedMessage}"
                }
            }
            
            override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                isPlaying = isPlayingParam
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(currentChannel) {
        retryCount = 0
    }

    LaunchedEffect(showControls, showSideList, isPlaying) {
        if (showControls && !showSideList && isPlaying && !isInPipMode) {
            delay(5000)
            showControls = false
        }
    }

    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null) {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isInPipMode
            ) {
                if (showSideList) showSideList = false else showControls = !showControls
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInPipMode) {
            if (isBuffering && errorOccurred == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
            }

            if (errorOccurred != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(text = "Failed to play stream", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(text = errorOccurred!!, color = Color.Gray, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { errorOccurred = null; exoPlayer?.prepare(); exoPlayer?.play() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
                }
            }

            AnimatedVisibility(visible = showControls, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Box(modifier = Modifier.fillMaxSize()) {

                    Surface(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = handleBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) 
                            }
                            
                            AsyncImage(
                                model = currentChannel.streamIcon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)),
                                contentScale = ContentScale.Fit,
                                placeholder = painterResource(R.drawable.app_icon_android),
                                error = painterResource(R.drawable.app_icon_android)
                            )
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentChannel.name ?: "Live Stream",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                    Text("LIVE", color = Color.Red, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    Spacer(Modifier.width(12.dp))
                                    Icon(Icons.Default.Speed, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(networkSpeed, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            
                            IconButton(onClick = { showSideList = true }) { 
                                Icon(Icons.AutoMirrored.Filled.List, "Channels", tint = Color.White) 
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val currentIndex = channels.indexOfFirst { it.streamId == currentChannel.streamId }
                                if (currentIndex > 0) {
                                    currentChannel = channels[currentIndex - 1]
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = { exoPlayer?.seekBack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Replay10, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = { if (isPlaying) exoPlayer?.pause() else exoPlayer?.play() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)
                            )
                        }

                        IconButton(
                            onClick = { exoPlayer?.seekForward() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Forward10, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = {
                                val currentIndex = channels.indexOfFirst { it.streamId == currentChannel.streamId }
                                if (currentIndex != -1 && currentIndex < channels.size - 1) {
                                    currentChannel = channels[currentIndex + 1]
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.navigationBarsPadding().padding(bottom = 8.dp)
                        ) {
                            if (duration > 0) {
                                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                    Slider(
                                        value = currentPosition.toFloat(),
                                        onValueChange = { exoPlayer?.seekTo(it.toLong()) },
                                        valueRange = 0f..duration.toFloat(),
                                        modifier = Modifier.fillMaxWidth().height(16.dp),
                                        thumb = {
                                            Box(
                                                modifier = Modifier.size(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                )
                                            }
                                        },
                                        track = { sliderState ->
                                            SliderDefaults.Track(
                                                sliderState = sliderState,
                                                modifier = Modifier.height(2.dp),
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                                )
                                            )
                                        }
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(formatTime(currentPosition), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                        Text(formatTime(duration), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                    IconButton(onClick = { showQualityDialog = true }) {
                                        Icon(Icons.Default.HighQuality, "Kualitas", tint = Color.White.copy(alpha = 0.8f))
                                        TrackSelectionMenu(
                                            visible = showQualityDialog,
                                            tracks = currentTracks,
                                            trackType = C.TRACK_TYPE_VIDEO,
                                            onDismiss = { showQualityDialog = false },
                                            onTrackSelected = { group, trackIndex ->
                                                exoPlayer?.let { player ->
                                                    player.trackSelectionParameters = player.trackSelectionParameters
                                                        .buildUpon()
                                                        .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                                        .build()
                                                }
                                                showQualityDialog = false
                                            }
                                        )
                                    }

                                    IconButton(onClick = { showAudioDialog = true }) {
                                        Icon(Icons.Default.GraphicEq, "Audio", tint = Color.White.copy(alpha = 0.8f))
                                        TrackSelectionMenu(
                                            visible = showAudioDialog,
                                            tracks = currentTracks,
                                            trackType = C.TRACK_TYPE_AUDIO,
                                            onDismiss = { showAudioDialog = false },
                                            onTrackSelected = { group, trackIndex ->
                                                exoPlayer?.let { player ->
                                                    player.trackSelectionParameters = player.trackSelectionParameters
                                                        .buildUpon()
                                                        .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                                        .build()
                                                }
                                                showAudioDialog = false
                                            }
                                        )
                                    }
                                }

                                IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                    Icon(
                                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        null, tint = Color.White
                                    )
                                }
                                
                                IconButton(onClick = { enterPip() }) {
                                    Icon(Icons.Default.PictureInPictureAlt, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showSideList,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight().width(200.dp),
                    color = Color.Black,
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column {
                        Spacer(Modifier.height(40.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CHANNEL LIST", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            IconButton(onClick = { showSideList = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray)
                            }
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        
                        val listState = rememberLazyListState()
                        LaunchedEffect(showSideList) {
                            val index = channels.indexOfFirst { it.streamId == currentChannel.streamId }
                            if (index >= 0) listState.animateScrollToItem(index)
                        }
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(channels) { channel ->
                                val isSelected = channel.streamId == currentChannel.streamId
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { currentChannel = channel; showSideList = false }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = channel.streamIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)),
                                            contentScale = ContentScale.Fit,
                                            placeholder = painterResource(R.drawable.app_icon_android),
                                            error = painterResource(R.drawable.app_icon_android)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            channel.name ?: "Siaran",
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectionMenu(
    visible: Boolean,
    tracks: Tracks,
    trackType: @C.TrackType Int,
    onDismiss: () -> Unit,
    onTrackSelected: (Tracks.Group, Int) -> Unit
) {
    if (visible) {
        BasicAlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            ),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .width(140.dp)
                            .padding(vertical = 8.dp)
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1A1A),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = if (trackType == C.TRACK_TYPE_VIDEO) "RESOLUTION" else "AUDIO",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.05f))

                            val groups = tracks.groups.filter { it.type == trackType }
                            if (groups.isEmpty()) {
                                Text(
                                    "N/A",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                    groups.forEach { group ->
                                        for (i in 0 until group.length) {
                                            if (group.isTrackSupported(i)) {
                                                val format = group.getTrackFormat(i)
                                                val isSelected = group.isTrackSelected(i)
                                                val label = when (trackType) {
                                                    C.TRACK_TYPE_VIDEO -> {
                                                        if (format.height > 0) "${format.height}p" else "Auto"
                                                    }
                                                    C.TRACK_TYPE_AUDIO -> {
                                                        val lang = format.language?.uppercase(Locale.US)
                                                        val label = format.label
                                                        when {
                                                            !lang.isNullOrBlank() && lang != "UND" -> lang
                                                            !label.isNullOrBlank() -> label
                                                            else -> "Track ${i + 1}"
                                                        }
                                                    }
                                                    else -> "T${i + 1}"
                                                }

                                                item {
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { onTrackSelected(group, i) },
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                                style = MaterialTheme.typography.labelMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                                )
                                                            )
                                                            if (isSelected) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@UnstableApi
@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    XStreamProTheme {
        PlayerScreen(
            session = UserSession("http://test.com", "user", "pass"),
            initialChannel = LiveStream(streamId = 1, name = "Test Channel"),
            channels = listOf(LiveStream(streamId = 1, name = "Test Channel")),
            onBack = {}
        )
    }
}
