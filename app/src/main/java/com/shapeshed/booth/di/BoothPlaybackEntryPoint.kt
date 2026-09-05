package com.shapeshed.booth.di

import android.app.Application
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.SettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BoothPlaybackEntryPoint {
    val podcastRepository: PodcastRepository
    val settings: SettingsStore
}

fun boothPlaybackEntryPoint(application: Application): BoothPlaybackEntryPoint =
    EntryPointAccessors.fromApplication(application, BoothPlaybackEntryPoint::class.java)
