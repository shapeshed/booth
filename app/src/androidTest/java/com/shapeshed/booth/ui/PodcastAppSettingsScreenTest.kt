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
import com.shapeshed.booth.data.PodcastDownloadLimit
import com.shapeshed.booth.data.PodcastDeleteBeforeAutoDownload
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
        scrollTo("Download network")
        composeRule.onNodeWithText("Download network").assertIsDisplayed()
        composeRule.onNodeWithText("Wi-Fi only").assertIsDisplayed()
        scrollTo("Auto refresh")
        composeRule.onNodeWithText("Auto refresh").assertIsDisplayed()
        scrollTo("Refresh podcasts automatically")
        composeRule.onNodeWithText("Refresh podcasts automatically").assertIsDisplayed()
        composeRule.onNodeWithText("0 podcasts").assertIsDisplayed()
        scrollTo("Refresh interval")
        composeRule.onNodeWithText("Refresh interval").assertIsDisplayed()
        composeRule.onNodeWithText("Every 6 hours").assertIsDisplayed()
        scrollTo("Refresh network")
        composeRule.onNodeWithText("Refresh network").assertIsDisplayed()
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

        scrollTo("Podcasts added to Up Next")
        composeRule.onNodeWithText("Podcasts added to Up Next").performClick()
        composeRule.runOnIdle {
            assertEquals(PodcastManagementCategory.AUTO_QUEUE, managedCategory)
        }

        scrollTo("Refresh podcasts automatically")
        composeRule.onNodeWithText("Refresh podcasts automatically").performClick()
        composeRule.runOnIdle {
            assertEquals(PodcastManagementCategory.AUTO_REFRESH, managedCategory)
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
    fun refreshRowsChangeTheirSelectedValues() {
        var interval: PodcastRefreshInterval? = null
        var network: PodcastRefreshNetwork? = null
        setSettingsContent(
            onRefreshIntervalChange = { interval = it },
            onRefreshNetworkChange = { network = it },
        )

        scrollTo("Refresh interval")
        composeRule.onNodeWithText("Refresh interval").performClick()
        composeRule.onNodeWithText("Daily").performClick()
        composeRule.runOnIdle { assertEquals(PodcastRefreshInterval.DAILY, interval) }

        scrollTo("Refresh network")
        composeRule.onNodeWithText("Refresh network").performClick()
        composeRule.onNodeWithText("Wi-Fi or mobile data").performClick()
        composeRule.runOnIdle { assertEquals(PodcastRefreshNetwork.ANY_CONNECTION, network) }
    }

    @Test
    fun downloadRetentionRowsChangeTheirSelectedValues() {
        var limit: PodcastDownloadLimit? = null
        var cleanup: PodcastDeleteBeforeAutoDownload? = null
        setSettingsContent(
            onDownloadLimitChange = { limit = it },
            onDeleteBeforeAutoDownloadChange = { cleanup = it },
        )

        scrollTo("Download limit")
        composeRule.onNodeWithText("Download limit").performClick()
        composeRule.onNodeWithText("100 episodes").performClick()
        composeRule.runOnIdle { assertEquals(PodcastDownloadLimit.ONE_HUNDRED, limit) }

        scrollTo("Delete before downloading")
        composeRule.onNodeWithText("Delete before downloading").performClick()
        composeRule.onNodeWithText("All eligible episodes").performClick()
        composeRule.runOnIdle { assertEquals(PodcastDeleteBeforeAutoDownload.ALL_ELIGIBLE, cleanup) }
    }

    @Test
    fun backupRowsInvokeImportAndJsonAndZipExportCallbacks() {
        var importCalls = 0
        var jsonExportCalls = 0
        var zipExportCalls = 0
        setSettingsContent(
            onImportBackup = { importCalls++ },
            onExportBackup = { jsonExportCalls++ },
            onExportBackupZip = { zipExportCalls++ },
        )

        scrollTo("Import Booth backup")
        composeRule.onNodeWithText("Import Booth backup").performClick()
        scrollTo("Export Booth backup")
        composeRule.onNodeWithText("Export Booth backup").performClick()
        scrollTo("Export Booth backup ZIP")
        composeRule.onNodeWithText("Export Booth backup ZIP").performClick()

        composeRule.runOnIdle {
            assertEquals(1, importCalls)
            assertEquals(1, jsonExportCalls)
            assertEquals(1, zipExportCalls)
        }
    }

    private fun setSettingsContent(
        onManagePodcasts: (PodcastManagementCategory) -> Unit = {},
        autoQueueEnabled: Boolean = true,
        onAutoQueueEnabledChange: (Boolean) -> Unit = {},
        downloadEpisodesAddedToUpNext: Boolean = false,
        onDownloadEpisodesAddedToUpNextChange: (Boolean) -> Unit = {},
        onRefreshIntervalChange: (PodcastRefreshInterval) -> Unit = {},
        onRefreshNetworkChange: (PodcastRefreshNetwork) -> Unit = {},
        onDownloadLimitChange: (PodcastDownloadLimit) -> Unit = {},
        onDeleteBeforeAutoDownloadChange: (PodcastDeleteBeforeAutoDownload) -> Unit = {},
        onImportOpml: () -> Unit = {},
        onExportOpml: () -> Unit = {},
        onImportBackup: () -> Unit = {},
        onExportBackup: () -> Unit = {},
        onExportBackupZip: () -> Unit = {},
    ) {
        composeRule.setContent {
            BoothAppTheme {
                PodcastAppSettingsScreen(
                    podcastManagementCounts = PodcastManagementCounts(
                        total = 4,
                        autoRefresh = 0,
                        autoQueue = 3,
                        notifications = 4,
                    ),
                    autoQueueEnabled = autoQueueEnabled,
                    onAutoQueueEnabledChange = onAutoQueueEnabledChange,
                    downloadEpisodesAddedToUpNext = downloadEpisodesAddedToUpNext,
                    onDownloadEpisodesAddedToUpNextChange = onDownloadEpisodesAddedToUpNextChange,
                    refreshInterval = PodcastRefreshInterval.SIX_HOURS,
                    onRefreshIntervalChange = onRefreshIntervalChange,
                    refreshNetwork = PodcastRefreshNetwork.WIFI_ONLY,
                    onRefreshNetworkChange = onRefreshNetworkChange,
                    downloadNetwork = PodcastDownloadNetwork.WIFI_ONLY,
                    onDownloadNetworkChange = {},
                    onDownloadLimitChange = onDownloadLimitChange,
                    onDeleteBeforeAutoDownloadChange = onDeleteBeforeAutoDownloadChange,
                    notificationsEnabled = true,
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
                    onExportBackupZip = onExportBackupZip,
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
