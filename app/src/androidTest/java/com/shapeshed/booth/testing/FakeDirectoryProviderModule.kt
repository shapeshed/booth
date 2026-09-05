package com.shapeshed.booth.testing

import com.shapeshed.booth.data.PodcastDiscoveryProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.shapeshed.booth.di.BoothDirectoryProviderModule::class],
)
object FakeDirectoryProviderModule {
    @Provides
    @Singleton
    fun provideFakeDirectoryProviders(): @JvmSuppressWildcards List<PodcastDiscoveryProvider> =
        listOf(FakeDirectoryProvider)
}

private object FakeDirectoryProvider : PodcastDiscoveryProvider {
    override val id: String = "fake-directory"
    override val supportsCategoryPaging: Boolean = true

    override suspend fun browse(shelf: com.shapeshed.booth.data.PodcastDiscoveryShelf) = emptyList<com.shapeshed.booth.data.PodcastSearchResult>()

    override suspend fun browse(category: com.shapeshed.booth.data.PodcastDiscoveryCategory) =
        listOf(com.shapeshed.booth.data.PodcastSearchResult(id, fakePodcast(category.title)))

    override suspend fun browse(category: com.shapeshed.booth.data.PodcastDiscoveryCategory, offset: Int) =
        if (offset == 0) browse(category) else emptyList()

    private fun fakePodcast(category: String) = com.shapeshed.booth.data.Podcast(
        id = 1L,
        title = "Fake $category podcast",
        author = "Test provider",
        feedUrl = "https://example.com/fake.xml",
        siteUrl = null,
        descriptionHtml = null,
        artworkUrl = null,
    )
}
