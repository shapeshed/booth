package com.shapeshed.booth.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.shapeshed.booth.BuildConfig
import com.shapeshed.booth.data.PodcastDownloadNetwork
import com.shapeshed.booth.data.PodcastRefreshInterval
import com.shapeshed.booth.data.PodcastRefreshNetwork
import com.shapeshed.booth.data.PodcastSearchProvider
import com.shapeshed.booth.data.PodcastSearchResult
import com.shapeshed.booth.ui.theme.BoothAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PodcastAppSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsSimplifiedSettingsAndKeepsPodcastDiscovery() {
        setSettingsContent()

        composeRule.onNodeWithText("Playback speed").assertIsDisplayed()
        composeRule.onNodeWithText("1×").assertIsDisplayed()
        scrollTo("Add new episodes to Up Next")
        composeRule.onNodeWithText("Add new episodes to Up Next").assertIsDisplayed()
        composeRule.onNodeWithText("3 podcasts").assertIsDisplayed()
        scrollTo("Download Up Next episodes")
        composeRule.onNodeWithText("Download Up Next episodes").assertIsDisplayed()
        scrollTo("Download network")
        composeRule.onNodeWithText("Download network").assertIsDisplayed()
        composeRule.onNodeWithText("Wi-Fi only").assertIsDisplayed()
        scrollTo("Podcast notifications")
        composeRule.onNodeWithText("Podcast notifications").assertIsDisplayed()
        composeRule.onNodeWithText("All podcasts").assertIsDisplayed()
        scrollTo("Podcast discovery")
        composeRule.onNodeWithText("Podcast discovery").assertIsDisplayed()
        scrollTo("Podcast search")
        composeRule.onNodeWithText("Podcast search").assertIsDisplayed()
        scrollTo("Import & Export")
        composeRule.onNodeWithText("Import & Export").assertIsDisplayed()
        val buildLabel = BuildConfig.BUILD_LABEL.takeIf { it.isNotBlank() } ?: "Version ${BuildConfig.VERSION_NAME}"
        scrollTo(buildLabel)
        composeRule.onNodeWithText(buildLabel).assertIsDisplayed()

        composeRule.onNodeWithText("Manage playback speed per podcast").assertDoesNotExist()
        composeRule.onNodeWithText("Auto refresh").assertDoesNotExist()
        composeRule.onNodeWithText("Download video").assertDoesNotExist()
    }

    @Test
    fun buildLabelUsesExpectedNightlyOrDirtyShape() {
        val buildLabel = BuildConfig.BUILD_LABEL

        if (buildLabel.isNotBlank()) {
            assertTrue(buildLabel.matches(Regex("(nightly|dirty)-[0-9a-f]{7}")))
        }
    }

    @Test
    fun tappingBuildLabelCopiesItToClipboard() {
        setSettingsContent()
        val buildLabel = BuildConfig.BUILD_LABEL.takeIf { it.isNotBlank() } ?: "Version ${BuildConfig.VERSION_NAME}"

        scrollTo(buildLabel)
        composeRule.onNodeWithTag("settings-version").performClick()

        val clipboard = ApplicationProvider
            .getApplicationContext<Context>()
            .getSystemService(ClipboardManager::class.java)
        assertEquals(buildLabel, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun countAndDataRowsOpenTheirManagementActions() {
        var managedCategory: PodcastManagementCategory? = null
        var autoQueueEnabled = false
        var importCalls = 0
        var exportCalls = 0
        setSettingsContent(
            onManagePodcasts = { managedCategory = it },
            autoQueueEnabled = false,
            onAutoQueueEnabledChange = { autoQueueEnabled = it },
            onImportOpml = { importCalls++ },
            onExportOpml = { exportCalls++ },
        )

        scrollTo("Add new episodes to Up Next")
        composeRule.onNodeWithText("Add new episodes to Up Next").performClick()
        composeRule.runOnIdle {
            assertEquals(PodcastManagementCategory.AUTO_QUEUE, managedCategory)
            assertEquals(true, autoQueueEnabled)
        }

        scrollTo("Import OPML")
        composeRule.onNodeWithText("Import OPML").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, importCalls) }

        scrollTo("Export OPML")
        composeRule.onNodeWithText("Export OPML").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, exportCalls) }
    }

    private fun setSettingsContent(
        onManagePodcasts: (PodcastManagementCategory) -> Unit = {},
        autoQueueEnabled: Boolean = true,
        onAutoQueueEnabledChange: (Boolean) -> Unit = {},
        onImportOpml: () -> Unit = {},
        onExportOpml: () -> Unit = {},
    ) {
        composeRule.setContent {
            BoothAppTheme {
                PodcastAppSettingsScreen(
                    globalPlaybackSpeed = 1f,
                    podcastManagementCounts = PodcastManagementCounts(
                        total = 4,
                        playbackSpeed = 2,
                        autoRefresh = 4,
                        autoDownload = 4,
                        autoQueue = 3,
                        videoDownload = 0,
                        notifications = 4,
                    ),
                    autoQueueEnabled = autoQueueEnabled,
                    onAutoQueueEnabledChange = onAutoQueueEnabledChange,
                    skipSilence = false,
                    onGlobalPlaybackSpeedChange = {},
                    onSkipSilenceChange = {},
                    videoDownloadsEnabled = false,
                    onVideoDownloadsEnabledChange = {},
                    autoRefreshEnabled = true,
                    onAutoRefreshEnabledChange = {},
                    refreshInterval = PodcastRefreshInterval.SIX_HOURS,
                    onRefreshIntervalChange = {},
                    refreshNetwork = PodcastRefreshNetwork.WIFI_ONLY,
                    onRefreshNetworkChange = {},
                    downloadNetwork = PodcastDownloadNetwork.WIFI_ONLY,
                    onDownloadNetworkChange = {},
                    notificationsEnabled = true,
                    onNotificationsEnabledChange = {},
                    autoDownloadEnabled = true,
                    onAutoDownloadEnabledChange = {},
                    searchProviders = listOf(testSearchProvider),
                    selectedSearchProviderId = testSearchProvider.id,
                    onSearchProviderChange = {},
                    podcastIndexCredentials = null,
                    onSavePodcastIndexCredentials = { _, _ -> },
                    onClearPodcastIndexCredentials = {},
                    isImporting = false,
                    statusMessage = null,
                    onImportOpml = onImportOpml,
                    onExportOpml = onExportOpml,
                    onManagePodcasts = onManagePodcasts,
                )
            }
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNodeWithTag(SETTINGS_LIST_TAG).performScrollToNode(hasText(text))
    }

    private companion object {
        const val SETTINGS_LIST_TAG = "podcast_app_settings_list"

        val testSearchProvider = object : PodcastSearchProvider {
            override val id = "test"
            override val displayName = "Test provider"
            override suspend fun search(query: String): List<PodcastSearchResult> = emptyList()
        }
    }
}
