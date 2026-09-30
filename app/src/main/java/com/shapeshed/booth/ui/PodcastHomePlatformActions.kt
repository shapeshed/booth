package com.shapeshed.booth.ui

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDateTime

internal class PodcastHomePlatformActions internal constructor(
    val importOpml: () -> Unit,
    val importBackup: () -> Unit,
    val exportOpml: () -> Unit,
    val exportBackup: () -> Unit,
    val setNotificationsEnabled: (Boolean) -> Unit,
    val notificationsPermissionGranted: Boolean,

    /**
     * Asks for POST_NOTIFICATIONS if it is missing, without touching the new-episode notification
     * preference.
     *
     * These are separate decisions. The media notification is the foreground-service notification,
     * and without the permission the service still runs but the notification is never shown, so the
     * user loses the lock screen, the notification shade and any headset transport for the episode
     * they are listening to. New-episode notifications stay off unless asked for in Settings.
     */
    val ensureMediaNotificationPermission: () -> Unit,
)

@Composable
internal fun rememberPodcastHomePlatformActions(
    context: Context,
    viewModel: PodcastViewModel,
    onSelectImport: () -> Unit,
): PodcastHomePlatformActions {
    val initialNotificationPermissionGranted = rememberPodcastNotificationPermission(context)
    var notificationPermissionGranted by remember {
        mutableStateOf(initialNotificationPermissionGranted)
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onSelectImport()
        viewModel.importOpml(context, uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-opml"),
    ) { uri ->
        uri?.let { viewModel.exportOpml(context, it) }
    }
    val backupExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.exportBackup(context, it) }
        }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(context, it) }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
        if (granted) viewModel.setPodcastNotificationsEnabled(true)
    }
    // Separate from the launcher above because the two requests mean different things: this one
    // only wants the media notification, so it must not switch new-episode notifications on.
    val mediaNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
    }
    return remember(
        importLauncher,
        exportLauncher,
        backupExportLauncher,
        backupImportLauncher,
        notificationLauncher,
        mediaNotificationLauncher,
        notificationPermissionGranted,
        onSelectImport,
    ) {
        PodcastHomePlatformActions(
            importOpml = { importLauncher.launch(arrayOf("text/xml", "text/x-opml", "application/xml", "*/*")) },
            importBackup = {
                backupImportLauncher.launch(arrayOf("application/zip", "*/*"))
            },
            exportOpml = { exportLauncher.launch("booth-subscriptions.opml") },
            exportBackup = { backupExportLauncher.launch(backupFileName(LocalDateTime.now())) },
            setNotificationsEnabled = { enabled ->
                when {
                    !enabled -> viewModel.setPodcastNotificationsEnabled(false)

                    notificationPermissionGranted -> viewModel.setPodcastNotificationsEnabled(true)

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }

                    else -> viewModel.setPodcastNotificationsEnabled(true)
                }
            },
            notificationsPermissionGranted = notificationPermissionGranted,
            ensureMediaNotificationPermission = {
                if (!notificationPermissionGranted &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    mediaNotificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}
