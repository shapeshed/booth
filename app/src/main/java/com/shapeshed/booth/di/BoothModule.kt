package com.shapeshed.booth.di

import com.shapeshed.booth.BoothApp
import com.shapeshed.booth.data.PodcastIndexCredentialsStore
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.PodcastSearchCatalog
import com.shapeshed.booth.data.SettingsStore
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/** Application-scoped bindings make ViewModel dependencies replaceable in tests. */
@Module
@InstallIn(SingletonComponent::class)
object BoothModule {
    @Provides
    @Singleton
    fun provideBoothApp(@ApplicationContext context: Context): BoothApp = context as BoothApp

    @Provides
    @Singleton
    fun providePodcastRepository(app: BoothApp): PodcastRepository = app.podcastRepository

    @Provides
    @Singleton
    fun providePodcastSearchCatalog(app: BoothApp): PodcastSearchCatalog = app.podcastSearchCatalog

    @Provides
    @Singleton
    fun provideSettings(app: BoothApp): SettingsStore = app.settings

    @Provides
    @Singleton
    fun providePodcastIndexCredentials(app: BoothApp): PodcastIndexCredentialsStore = app.podcastIndexCredentials
}
