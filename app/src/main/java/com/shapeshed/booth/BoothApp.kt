package com.shapeshed.booth

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.shapeshed.booth.data.FeedParser
import com.shapeshed.booth.data.Prof18FeedParser
import com.shapeshed.booth.data.SettingsStore
import com.shapeshed.booth.data.PodcastDatabase
import com.shapeshed.booth.data.PodcastIndexSearchProvider
import com.shapeshed.booth.data.PodcastIndexCredentialsStore
import com.shapeshed.booth.data.ApplePodcastSearchProvider
import com.shapeshed.booth.data.DefaultPodcastSearchCatalog
import com.shapeshed.booth.data.PodcastSearchCatalog
import com.shapeshed.booth.data.PodcastDiscoveryCatalog
import com.shapeshed.booth.data.PodcastRepository
import com.shapeshed.booth.data.PodcastFeedProvider
import com.shapeshed.booth.data.RssPodcastFeedProvider
import com.shapeshed.booth.data.SaxStreamingFeedParser
import com.shapeshed.booth.data.StreamingPodcastFeedProvider
import com.shapeshed.booth.data.StreamingCompletePodcastFeedProvider
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import dagger.hilt.android.HiltAndroidApp

/**
 * Owns the app's few long-lived singletons. No DI framework — the same deliberately plain wiring
 * as the rest of the app; ViewModels reach [repository] via the application instance. Also serves
 * as Coil's image-loader factory so favicons load through the shared OkHttp client.
 */
@HiltAndroidApp
class BoothApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        com.shapeshed.booth.data.PodcastDownloadReconciliationWorker.schedule(this)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = 32 })
            .cache(Cache(File(cacheDir, "http_cache"), CACHE_SIZE_BYTES))
            .build()
    }

    val feedParser: FeedParser by lazy { Prof18FeedParser(okHttpClient) }

    val settings: SettingsStore by lazy { SettingsStore(this) }
    val podcastIndexCredentials: PodcastIndexCredentialsStore by lazy { PodcastIndexCredentialsStore(this) }

    private val podcastDatabase: PodcastDatabase by lazy { PodcastDatabase.create(this) }

    val podcastRepository: PodcastRepository by lazy {
        PodcastRepository(
            podcastFeedProvider,
            podcastDatabase.podcastDao(),
            podcastDatabase.downloadAssetDao(),
            okHttpClient,
            streamingFeedProvider,
        )
    }

    private val legacyPodcastFeedProvider: RssPodcastFeedProvider by lazy {
        RssPodcastFeedProvider(feedParser)
    }

    private val podcastFeedProvider: PodcastFeedProvider by lazy {
        StreamingCompletePodcastFeedProvider(
            client = okHttpClient,
            parser = SaxStreamingFeedParser(),
            fallback = legacyPodcastFeedProvider,
        )
    }

    private val streamingFeedProvider: StreamingPodcastFeedProvider by lazy {
        StreamingPodcastFeedProvider(
            client = okHttpClient,
            parser = SaxStreamingFeedParser(),
            fallback = legacyPodcastFeedProvider,
        )
    }

    val applePodcastSearchProvider: ApplePodcastSearchProvider by lazy {
        ApplePodcastSearchProvider(
            client = okHttpClient,
            localeProvider = {
                resources.configuration.locales[0] ?: Locale.getDefault()
            },
        )
    }

    val podcastSearchProvider: PodcastIndexSearchProvider by lazy {
        PodcastIndexSearchProvider(
            client = okHttpClient,
            credentialsStore = podcastIndexCredentials,
            localeProvider = {
                resources.configuration.locales[0] ?: Locale.getDefault()
            },
        )
    }

    val podcastSearchCatalog: PodcastSearchCatalog by lazy {
        DefaultPodcastSearchCatalog(
            providers = listOf(
                applePodcastSearchProvider,
                podcastSearchProvider,
            ),
        )
    }

    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .build()

    private companion object {
        const val CACHE_SIZE_BYTES = 20L * 1024 * 1024

    }
}
