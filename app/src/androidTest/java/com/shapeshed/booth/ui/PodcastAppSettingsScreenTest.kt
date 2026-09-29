package com.shapeshed.booth.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.shapeshed.booth.BuildConfig
import com.shapeshed.booth.data.PodcastDownloadNetwork
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
    fun showsSettingsAndKeepsPodcastDiscovery() {
        setSettingsContent()

        composeRule.onNodeWithText("Playback speed").assertDoesNotExist()
        scrollTo("Add new episodes to Up Next")
        composeRule.onNodeWithText("Add new episodes to Up Next").assertIsDisplayed()
        scrollTo("Podcasts added to Up Next")
        composeRule.onNodeWithText("Podcasts added to Up Next").assertIsDisplayed()
        composeRule.onNodeWithText("3 podcasts").assertIsDisplayed()
        scrollTo("Download episodes added to Up Next")
        composeRule.onNodeWithText("Download episodes added to Up Next").assertIsDisplayed()
        composeRule.onNodeWithText("Download videos when available").assertDoesNotExist()
        composeRule.onNodeWithText("Podcast video downloads").assertDoesNotExist()
        // The Up Next automatic download toggle is off in this test's defaults, and Download network
        // only gates that path, so the row must be absent rather than offering a choice that cannot
        // change anything.
        composeRule.onNodeWithText("Download network").assertDoesNotExist()
        // Refresh is scheduled per feed from its own publishing pattern, so the interval and network
        // rows are gone. Their absence is the behaviour, and the other rows are asserted around them
        // so a row that moved cannot quietly satisfy this by shifting position.
        composeRule.onNodeWithText("Refresh interval").assertDoesNotExist()
        composeRule.onNodeWithText("Refresh network").assertDoesNotExist()
        composeRule.onNodeWithText("Every 6 hours").assertDoesNotExist()
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
    }

    @Test
    fun buildLabelUsesExpectedNightlyOrDirtyShape() {
        val buildLabel = BuildConfig.BUILD_LABEL

        if (buildLabel.isNotBlank()) {
            assertTrue(buildLabel.matches(Regex("(nightly|dirty)-[0-9a-f]{7}")))
        }
    }

    @Test
    fun tappingBuildLabelOffersItForCopying() {
        var copied: String? = null
        setSettingsContent(onCopyVersion = { copied = it })
        val buildLabel = BuildConfig.BUILD_LABEL.takeIf { it.isNotBlank() }
            ?: "Version ${BuildConfig.VERSION_NAME}"

        scrollTo(buildLabel)
        composeRule.onNodeWithTag("settings-version").performClick()

        // The clipboard write is injected rather than read back from the system ClipboardManager:
        // since Android 10 getPrimaryClip() returns null unless the reader holds focus, which a
        // createComposeRule() instrumentation test never does. Asserting on the system clipboard
        // here made this test fail on every API 29+ device while proving nothing.
        assertEquals(buildLabel, copied)
    }

    @Test
    fun countAndDataRowsOpenTheirManagementActions() {
        var managedCategory: PodcastManagementCategory? = null
        var autoQueueEnabled: Boolean? = null
        var downloadQueuedEpisodes: Boolean? = null
        var importCalls = 0
        var exportCalls = 0
        setSettingsContent(
            onManagePodcasts = { managedCategory = it },
            autoQueueEnabled = true,
            onAutoQueueEnabledChange = { autoQueueEnabled = it },
            downloadEpisodesAddedToUpNext = false,
            onDownloadEpisodesAddedToUpNextChange = { downloadQueuedEpisodes = it },
            onImportOpml = { importCalls++ },
            onExportOpml = { exportCalls++ },
        )

        scrollTo("Add new episodes to Up Next")
        composeRule.onNodeWithText("Add new episodes to Up Next").performClick()
        composeRule.runOnIdle {
            assertEquals(false, autoQueueEnabled)
            assertEquals(null, managedCategory)
        }

        managedCategory = null
        scrollTo("Podcasts added to Up Next")
        composeRule.onNodeWithText("Podcasts added to Up Next").performClick()
        composeRule.runOnIdle {
            assertEquals(PodcastManagementCategory.AUTO_QUEUE, managedCategory)
        }

        managedCategory = null
        scrollTo("Download episodes added to Up Next")
        composeRule.onNodeWithText("Download episodes added to Up Next").performClick()
        composeRule.runOnIdle {
            assertEquals(true, downloadQueuedEpisodes)
            assertEquals(null, managedCategory)
        }

        scrollTo("Import OPML")
        composeRule.onNodeWithText("Import OPML").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, importCalls) }

        scrollTo("Export OPML")
        composeRule.onNodeWithText("Export OPML").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, exportCalls) }
    }

    @Test
    fun theDefaultPlaybackSpeedRowOpensTheSheetAndReportsTheChosenSpeed() {
        // The row and its sheet are new and this is their only coverage. It shows the app-wide
        // speed, and choosing a preset has to reach the callback, or the setting silently does
        // nothing while looking entirely functional.
        var chosen: Float? = null
        setSettingsContent(
            globalPlaybackSpeed = 1.25f,
            onGlobalPlaybackSpeedChange = { chosen = it },
        )

        scrollTo("Default playback speed")
        composeRule.onNodeWithText("Default playback speed").assertIsDisplayed()
        // The supporting text is the current value, so the row states what it is set to.
        composeRule.onNodeWithText("Default playback speed").assertTextContains("1.25×")

        composeRule.onNodeWithText("Default playback speed").performClick()
        composeRule.runOnIdle { assertEquals(null, chosen) }
        composeRule.onNodeWithText("1.5×").performClick()
        composeRule.runOnIdle { assertEquals(1.5f, chosen) }
    }

    @Test
    fun podcastNotificationsIsHiddenWhileNotificationsAreOff() {
        // Which podcasts notify cannot take effect while notifications are off. The walk-through
        // covers the on case, so together they pin both directions.
        setSettingsContent(notificationsEnabled = false)

        // Anchored on the group label below the notifications group rather than on "Notifications"
        // itself, which matches both the group label and the global toggle's headline and so is
        // ambiguous. The anchor matters: an absence assertion against a blank screen proves nothing.
        scrollTo("Podcast discovery")
        composeRule.onNodeWithText("Podcast discovery").assertIsDisplayed()
        composeRule.onNodeWithText("Podcast notifications").assertDoesNotExist()
        composeRule.onNodeWithText("All podcasts").assertDoesNotExist()
    }

    @Test
    fun podcastsAddedToUpNextIsHiddenWhileTheFeatureIsOff() {
        // With the global toggle off, which podcasts are opted in cannot take effect, so the row is
        // hidden rather than shown with a "disabled globally" summary. The walk-through covers the
        // on case, so the two together pin both directions.
        setSettingsContent(autoQueueEnabled = false)

        scrollTo("Add new episodes to Up Next")
        composeRule.onNodeWithText("Add new episodes to Up Next").assertIsDisplayed()
        composeRule.onNodeWithText("Podcasts added to Up Next").assertDoesNotExist()
        composeRule.onNodeWithText("3 podcasts").assertDoesNotExist()
    }

    @Test
    fun downloadNetworkAppearsOnlyWhenUpNextDownloadsAreOn() {
        // The paired half of the absence asserted elsewhere: with automatic Up Next downloads on,
        // the network row is there, because it is the only control over that path's network.
        setSettingsContent(downloadEpisodesAddedToUpNext = true)

        scrollTo("Download network")
        composeRule.onNodeWithText("Download network").assertIsDisplayed()
        // ListItem merges a row's headline and supporting text into one node, so the value is
        // asserted against the row rather than by matching the label alone.
        composeRule.onNodeWithText("Download network").assertTextContains("Wi-Fi only")
    }

    @Test
    fun backupRowsInvokeImportAndExportCallbacks() {
        var importCalls = 0
        var exportCalls = 0
        setSettingsContent(
            onImportBackup = { importCalls++ },
            onExportBackup = { exportCalls++ },
        )

        scrollTo("Import Booth backup")
        composeRule.onNodeWithText("Import Booth backup").performClick()
        scrollTo("Export Booth backup")
        composeRule.onNodeWithText("Export Booth backup").performClick()

        composeRule.runOnIdle {
            assertEquals(1, importCalls)
            assertEquals(1, exportCalls)
        }
    }

    private fun setSettingsContent(
        onManagePodcasts: (PodcastManagementCategory) -> Unit = {},
        autoQueueEnabled: Boolean = true,
        notificationsEnabled: Boolean = true,
        globalPlaybackSpeed: Float = 1f,
        onGlobalPlaybackSpeedChange: (Float) -> Unit = {},
        onAutoQueueEnabledChange: (Boolean) -> Unit = {},
        downloadEpisodesAddedToUpNext: Boolean = false,
        onDownloadEpisodesAddedToUpNextChange: (Boolean) -> Unit = {},
        onImportOpml: () -> Unit = {},
        onExportOpml: () -> Unit = {},
        onImportBackup: () -> Unit = {},
        onExportBackup: () -> Unit = {},
        onCopyVersion: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            BoothAppTheme {
                PodcastAppSettingsScreen(
                    podcastManagementCounts = PodcastManagementCounts(
                        total = 4,
                        autoQueue = 3,
                        notifications = 4,
                    ),
                    autoQueueEnabled = autoQueueEnabled,
                    onAutoQueueEnabledChange = onAutoQueueEnabledChange,
                    downloadEpisodesAddedToUpNext = downloadEpisodesAddedToUpNext,
                    onDownloadEpisodesAddedToUpNextChange = onDownloadEpisodesAddedToUpNextChange,
                    downloadNetwork = PodcastDownloadNetwork.WIFI_ONLY,
                    onDownloadNetworkChange = {},
                    notificationsEnabled = notificationsEnabled,
                    globalPlaybackSpeed = globalPlaybackSpeed,
                    onGlobalPlaybackSpeedChange = onGlobalPlaybackSpeedChange,
                    onNotificationsEnabledChange = {},
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
                    onImportBackup = onImportBackup,
                    onExportBackup = onExportBackup,
                    onManagePodcasts = onManagePodcasts,
                    onCopyVersion = onCopyVersion,
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
