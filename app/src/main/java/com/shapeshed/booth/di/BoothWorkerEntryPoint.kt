package com.shapeshed.booth.di

import android.content.Context
import com.shapeshed.booth.data.PodcastRepository
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
}

fun boothWorkerEntryPoint(context: Context): BoothWorkerEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, BoothWorkerEntryPoint::class.java)
