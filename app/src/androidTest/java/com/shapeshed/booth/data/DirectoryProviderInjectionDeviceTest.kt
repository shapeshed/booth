package com.shapeshed.booth.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DirectoryProviderInjectionDeviceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var providers: @JvmSuppressWildcards List<PodcastDiscoveryProvider>

    @Before
    fun injectDependencies() {
        hiltRule.inject()
    }

    @Test
    fun testBindingReplacesProductionDirectoryProviders() {
        assertEquals(listOf("fake-directory"), providers.map(PodcastDiscoveryProvider::id))
        assertEquals(true, providers.single().supportsCategoryPaging)
    }
}
