package com.shapeshed.booth

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

/**
 * Application entry point. All long-lived dependencies come from the Hilt singleton graph in
 * [com.shapeshed.booth.di.BoothModule]; see `boothWorkerEntryPoint` for the workers, receivers
 * and services the framework instantiates itself.
 *
 * This class used to hand-wire its own OkHttpClient, Room database and repository as well. That
 * produced two Room instances and two OkHttp clients — sharing one cache directory — per process,
 * and writes through one graph did not invalidate flows observed on the other. It is also Coil's
 * image-loader factory, so favicons go through the same shared client.
 */
@HiltAndroidApp
class BoothApp :
    Application(),
    SingletonImageLoader.Factory {

    // Hilt injects this before onCreate, so newImageLoader can hand the one shared client to
    // Coil without going back through EntryPointAccessors. It is the same instance the workers
    // and the playback service resolve.
    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        com.shapeshed.booth.data.PodcastDownloadReconciliationWorker.schedule(this)
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
        .build()
}
