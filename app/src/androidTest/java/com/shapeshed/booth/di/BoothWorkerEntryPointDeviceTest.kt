package com.shapeshed.booth.di

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shapeshed.booth.data.PodcastRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
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
}
