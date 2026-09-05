package com.shapeshed.booth.di

import com.shapeshed.booth.data.FeedParser
import com.shapeshed.booth.data.ApplePodcastSearchProvider
import com.shapeshed.booth.data.DefaultPodcastSearchCatalog
import com.shapeshed.booth.data.Prof18FeedParser
import com.shapeshed.booth.data.PodcastDatabase
import com.shapeshed.booth.data.PodcastIndexCredentialsStore
import com.shapeshed.booth.data.PodcastIndexSearchProvider
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.PodcastSearchCatalog
import com.shapeshed.booth.data.SettingsStore
import com.shapeshed.booth.data.PodcastFeedProvider
import com.shapeshed.booth.data.RssPodcastFeedProvider
import com.shapeshed.booth.data.SaxStreamingFeedParser
import com.shapeshed.booth.data.StreamingCompletePodcastFeedProvider
import com.shapeshed.booth.data.StreamingPodcastFeedProvider
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.Locale
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
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = 32 })
            .cache(Cache(File(context.cacheDir, "http_cache"), 20L * 1024 * 1024))
            .build()

    @Provides
    @Singleton
    fun provideFeedParser(client: OkHttpClient): FeedParser = Prof18FeedParser(client)

    @Provides
    @Singleton
    fun providePodcastDatabase(@ApplicationContext context: Context): PodcastDatabase =
        PodcastDatabase.create(context)

    @Provides
    @Singleton
    fun providePodcastFeedProvider(client: OkHttpClient, parser: FeedParser): PodcastFeedProvider =
        StreamingCompletePodcastFeedProvider(
            client = client,
            parser = SaxStreamingFeedParser(),
            fallback = RssPodcastFeedProvider(parser),
        )

    @Provides
    @Singleton
    fun provideStreamingFeedProvider(client: OkHttpClient, parser: FeedParser): StreamingPodcastFeedProvider =
        StreamingPodcastFeedProvider(
            client = client,
            parser = SaxStreamingFeedParser(),
            fallback = RssPodcastFeedProvider(parser),
        )

    @Provides
    @Singleton
    fun providePodcastRepository(
        feedProvider: PodcastFeedProvider,
        database: PodcastDatabase,
        client: OkHttpClient,
        streamingFeedProvider: StreamingPodcastFeedProvider,
    ): PodcastRepository = PodcastRepository(
        feedProvider,
        database.podcastDao(),
        database.downloadAssetDao(),
        client,
        streamingFeedProvider,
    )

    @Provides
    @Singleton
    fun provideSettings(@ApplicationContext context: Context): SettingsStore = SettingsStore(context)

    @Provides
    @Singleton
    fun providePodcastIndexCredentials(@ApplicationContext context: Context): PodcastIndexCredentialsStore =
        PodcastIndexCredentialsStore(context)

    @Provides
    @Singleton
    fun provideAppleSearchProvider(
        client: OkHttpClient,
        @ApplicationContext context: Context,
    ): ApplePodcastSearchProvider = ApplePodcastSearchProvider(
        client = client,
        localeProvider = { context.resources.configuration.locales[0] ?: Locale.getDefault() },
    )

    @Provides
    @Singleton
    fun providePodcastIndexSearchProvider(
        client: OkHttpClient,
        credentials: PodcastIndexCredentialsStore,
        @ApplicationContext context: Context,
    ): PodcastIndexSearchProvider = PodcastIndexSearchProvider(
        client = client,
        credentialsStore = credentials,
        localeProvider = { context.resources.configuration.locales[0] ?: Locale.getDefault() },
    )

    @Provides
    @Singleton
    fun providePodcastSearchCatalog(
        apple: ApplePodcastSearchProvider,
        podcastIndex: PodcastIndexSearchProvider,
    ): PodcastSearchCatalog = DefaultPodcastSearchCatalog(listOf(apple, podcastIndex))
}
