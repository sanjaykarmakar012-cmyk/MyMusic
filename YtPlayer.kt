package com.mymusic.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/** Official YouTube IFrame player (ToS-compliant): video stays visible, pauses when app is backgrounded. */
@Composable
fun YtPlayer(videoId: String, onEnded: () -> Unit, onError: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val endedCb by rememberUpdatedState(onEnded)
    val errorCb by rememberUpdatedState(onError)
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }

    val view = remember {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) { player = youTubePlayer }
                override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                    if (state == PlayerConstants.PlayerState.ENDED) endedCb()
                }
                override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                    errorCb() // e.g. embedding disabled -> skip to next
                }
            }, IFramePlayerOptions.Builder().controls(1).build())
        }
    }

    DisposableEffect(owner, view) {
        owner.lifecycle.addObserver(view)
        onDispose { owner.lifecycle.removeObserver(view); view.release() }
    }
    LaunchedEffect(player, videoId) { player?.loadVideo(videoId, 0f) }

    AndroidView(factory = { view }, modifier = modifier)
}
