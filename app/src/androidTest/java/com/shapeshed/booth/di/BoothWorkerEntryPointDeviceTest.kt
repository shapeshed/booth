package com.shapeshed.booth.di

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shapeshed.booth.data.DownloadProgressStore
import com.shapeshed.booth.data.PodcastBackupManager
import com.shapeshed.booth.data.PodcastDownloadManager
import com.shapeshed.booth.data.PodcastRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BoothWorkerEntryPointDeviceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: PodcastRepository

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var downloadManager: PodcastDownloadManager

    @Inject
    lateinit var backupManager: PodcastBackupManager

    @Inject
    lateinit var progressStore: DownloadProgressStore

    @Before
    fun injectDependencies() {
        hiltRule.inject()
    }

    @Test
    fun providesTheSameRepositoryObservedByTheUi() {
        val workerRepository = boothWorkerEntryPoint(ApplicationProvider.getApplicationContext())
            .podcastRepository

        assertSame(repository, workerRepository)
    }

    /**
     * There used to be a second object graph built by hand in BoothApp, so workers, the download
     * receiver and the playback service each got their own Room instance and their own OkHttp
     * client sharing one cache directory. Room's invalidation tracker is per instance, so writes
     * through one graph never reached flows observed on the other. These two tests pin the
     * invariant that made that possible in the first place.
     */
    @Test
    fun playbackAndWorkerComponentsShareOneRepositoryInstance() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()

        val workerRepository = boothWorkerEntryPoint(application).podcastRepository
        val playbackRepository = boothPlaybackEntryPoint(application).podcastRepository

        assertSame(repository, workerRepository)
        assertSame(workerRepository, playbackRepository)
    }

    @Test
    fun everyComponentSharesOneHttpClient() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()

        // Two Cache objects over one directory is the failure mode: OkHttp requires exclusive
        // access, and the rebuildJournal() fallback deletes the directory.
        assertSame(okHttpClient, boothWorkerEntryPoint(application).okHttpClient)
    }

    /**
     * PodcastDownloadManager was constructed by hand at eight call sites, so its enqueue mutex was
     * per instance and provided no mutual exclusion at all between the worker that enqueues and
     * the reconciliation worker that inspects the same rows. One instance per process makes the
     * lock mean what it looks like it means.
     */
    @Test
    fun downloadManagerIsSharedRatherThanPerCallSite() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()

        assertSame(downloadManager, boothWorkerEntryPoint(application).downloadManager)
    }

    @Test
    fun backupManagerIsSharedRatherThanPerCallSite() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()

        assertSame(backupManager, boothWorkerEntryPoint(application).backupManager)
    }

    /**
     * DownloadProgressStore was a process-global `object` written from Dispatchers.IO in the
     * download workers, the reconciliation worker and the broadcast receiver, and from the main
     * thread in the ViewModel. Beyond being invisible in the graph, that made it impossible for a
     * test to hand a component its own instance and assert on the progress it produced.
     */
    @Test
    fun downloadProgressStoreIsInjectedRatherThanGlobal() {
        val application = ApplicationProvider.getApplicationContext<android.app.Application>()

        assertSame(progressStore, boothWorkerEntryPoint(application).downloadProgressStore)
    }
}
