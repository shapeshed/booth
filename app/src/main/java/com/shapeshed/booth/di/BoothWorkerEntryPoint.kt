package com.shapeshed.booth.di

import android.content.Context
import com.shapeshed.booth.data.DownloadProgressStore
import com.shapeshed.booth.data.PodcastBackupManager
import com.shapeshed.booth.data.PodcastDownloadManager
import com.shapeshed.booth.data.PodcastRefreshCoordinator
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.PodcastSubscriptionProgressStore
import com.shapeshed.booth.data.SettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

/** Dependencies for framework-created workers that cannot use constructor injection. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BoothWorkerEntryPoint {
    val podcastRepository: PodcastRepository
    val settings: SettingsStore

    /**
     * The single application-wide client. Workers and broadcast receivers used to reach for
     * `BoothApp.okHttpClient`, which was a second client sharing the same cache directory.
     */
    val okHttpClient: OkHttpClient

    /** Injected rather than a process-global, so a test can hand a worker its own instance. */
    val downloadProgressStore: DownloadProgressStore

    /** Injected for the same reason: written from IO and the main thread. */
    val subscriptionProgressStore: PodcastSubscriptionProgressStore

    /** Singleton, so its enqueue mutex is shared rather than per instance. */
    val downloadManager: PodcastDownloadManager

    /** Shared process lock prevents simultaneous feed workers from materializing large episode sets. */
    val podcastRefreshCoordinator: PodcastRefreshCoordinator

    val backupManager: PodcastBackupManager
}

fun boothWorkerEntryPoint(context: Context): BoothWorkerEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, BoothWorkerEntryPoint::class.java)
