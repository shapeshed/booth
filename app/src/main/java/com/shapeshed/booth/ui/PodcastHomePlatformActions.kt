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

internal class PodcastHomePlatformActions internal constructor(
    val importOpml: () -> Unit,
    val importBackup: () -> Unit,
    val exportOpml: () -> Unit,
    val exportBackup: () -> Unit,
    val exportBackupZip: () -> Unit,
    val setNotificationsEnabled: (Boolean) -> Unit,
    val notificationsPermissionGranted: Boolean,
)

@Composable
internal fun rememberPodcastHomePlatformActions(
    context: Context,
    viewModel: PodcastViewModel,
    onImportSelected: () -> Unit,
): PodcastHomePlatformActions {
    val initialNotificationPermissionGranted = rememberPodcastNotificationPermission(context)
    var notificationPermissionGranted by remember {
        mutableStateOf(initialNotificationPermissionGranted)
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onImportSelected()
        viewModel.importOpml(context, uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-opml"),
    ) { uri ->
        uri?.let { viewModel.exportOpml(context, it) }
    }
    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.exportBackup(context, it) }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(context, it) }
    }
    val backupZipExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.exportBackupZip(context, it) }
        }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
        if (granted) viewModel.setPodcastNotificationsEnabled(true)
    }
    return remember(
        importLauncher,
        exportLauncher,
        backupExportLauncher,
        backupImportLauncher,
        backupZipExportLauncher,
        notificationLauncher,
        notificationPermissionGranted,
        onImportSelected,
    ) {
        PodcastHomePlatformActions(
            importOpml = { importLauncher.launch(arrayOf("text/xml", "text/x-opml", "application/xml", "*/*")) },
            importBackup = { backupImportLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
            exportOpml = { exportLauncher.launch("booth-subscriptions.opml") },
            exportBackup = { backupExportLauncher.launch("booth-backup.json") },
            exportBackupZip = { backupZipExportLauncher.launch("booth-backup.zip") },
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
        )
    }
}
