package com.shapeshed.booth.di

import com.shapeshed.booth.data.ApplePodcastSearchProvider
import com.shapeshed.booth.data.PodcastDiscoveryProvider
import com.shapeshed.booth.data.PodcastIndexSearchProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BoothDirectoryProviderModule {
    @Provides
    @Singleton
    fun provideDiscoveryProviders(
        apple: ApplePodcastSearchProvider,
        podcastIndex: PodcastIndexSearchProvider,
    ): @JvmSuppressWildcards List<PodcastDiscoveryProvider> = listOf(apple, podcastIndex)
}
