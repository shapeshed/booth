package com.shapeshed.booth

import android.content.Intent
import android.os.Bundle
import com.shapeshed.booth.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shapeshed.booth.ui.PodcastHomeScreen
import com.shapeshed.booth.ui.theme.BoothAppTheme
import com.shapeshed.booth.ui.PodcastPlaybackViewModel
import com.shapeshed.booth.ui.PodcastViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var initialPodcastEpisodeId by mutableStateOf<Long?>(null)
    private var initialPodcastNotificationAction by mutableStateOf<String?>(null)
    private var forceGettingStarted by mutableStateOf(false)
    private var initialContentReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !initialContentReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initialPodcastEpisodeId = intent.initialPodcastEpisodeIdExtra()
        initialPodcastNotificationAction = intent.initialPodcastNotificationActionExtra()
        forceGettingStarted = intent.debugGettingStartedExtra()
        setContent {
            BoothAppTheme {
            PodcastHomeScreen(
                initialEpisodeId = initialPodcastEpisodeId,
                initialNotificationAction = initialPodcastNotificationAction,
                forceGettingStarted = forceGettingStarted,
                onInitialContentReady = { initialContentReady = true },
                viewModel = hiltViewModel<PodcastViewModel>(),
                playbackViewModel = hiltViewModel<PodcastPlaybackViewModel>(),
            )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initialPodcastEpisodeId = intent.initialPodcastEpisodeIdExtra()
        initialPodcastNotificationAction = intent.initialPodcastNotificationActionExtra()
        forceGettingStarted = intent.debugGettingStartedExtra()
    }
}

private fun Intent.initialPodcastEpisodeIdExtra(): Long? =
    getLongExtra(ExtraInitialPodcastEpisodeId, 0L).takeIf { it > 0L }

private fun Intent.initialPodcastNotificationActionExtra(): String? =
    getStringExtra(ExtraInitialPodcastNotificationAction)

private fun Intent.debugGettingStartedExtra(): Boolean =
    BuildConfig.DEBUG && getBooleanExtra(ExtraDebugGettingStarted, false)

const val ExtraInitialPodcastEpisodeId = "com.shapeshed.booth.extra.INITIAL_PODCAST_EPISODE_ID"
const val ExtraInitialPodcastNotificationAction = "com.shapeshed.booth.extra.INITIAL_PODCAST_NOTIFICATION_ACTION"
const val ExtraDebugGettingStarted = "com.shapeshed.booth.extra.DEBUG_GETTING_STARTED"
const val PodcastNotificationActionPlay = "com.shapeshed.booth.action.PLAY_EPISODE"
const val PodcastNotificationActionAddToQueue = "com.shapeshed.booth.action.ADD_EPISODE_TO_QUEUE"
