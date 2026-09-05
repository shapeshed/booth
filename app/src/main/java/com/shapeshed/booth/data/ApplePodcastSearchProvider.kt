package com.shapeshed.booth.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Apple’s public podcast catalogue search. It requires no secret in the app. */
class ApplePodcastSearchProvider(
    private val client: OkHttpClient,
    private val localeProvider: () -> Locale = { Locale.getDefault() },
    private val apiBaseUrl: String = "https://itunes.apple.com",
    private val rssBaseUrl: String = "https://itunes.apple.com",
) : PodcastSearchProvider, PodcastDiscoveryProvider {
    private val country: String
        get() = localeProvider().country.lowercase(Locale.ROOT).ifBlank { "us" }
    override val id: String = "apple"
    override val displayName: String = "Apple Podcasts"
    override val supportsCategoryPaging: Boolean = true
    override val supportsPopularPodcasts: Boolean = true
    private val categoryCharts = ConcurrentHashMap<String, List<PodcastSearchResult>>()

    override suspend fun browse(shelf: PodcastDiscoveryShelf): List<PodcastSearchResult> =
        withContext(Dispatchers.IO) {
            val path = when (shelf) {
                PodcastDiscoveryShelf.TOP_SHOWS -> "toppodcasts/limit=25/explicit=true/json"
            }
            loadChart(path)
        }

    override suspend fun browse(category: PodcastDiscoveryCategory): List<PodcastSearchResult> =
        browse(category, 0)

    override suspend fun browse(category: PodcastDiscoveryCategory, offset: Int): List<PodcastSearchResult> =
        withContext(Dispatchers.IO) {
            val cacheKey = "$country:${category.appleGenreId}"
            val chart = categoryCharts[cacheKey] ?: synchronized(categoryCharts) {
                categoryCharts[cacheKey] ?: loadChart(
                    "toppodcasts/genre=${category.appleGenreId}/limit=200/explicit=true/json",
                ).also { categoryCharts[cacheKey] = it }
            }
            chart.drop(offset).take(25)
        }

    private fun loadChart(path: String): List<PodcastSearchResult> {
        val chartUrl = "$rssBaseUrl/$country/rss/$path".toHttpUrl()
        val chart = getJson(chartUrl)
        val entries = chart.optJSONObject("feed")?.optJSONArray("entry") ?: return emptyList()
            val ids = buildList {
                for (index in 0 until entries.length()) {
                    entries.optJSONObject(index)
                        ?.optJSONObject("id")
                        ?.optJSONObject("attributes")
                        ?.optString("im:id")
                        ?.takeIf(String::isNotBlank)
                        ?.let(::add)
                }
            }
            if (ids.isEmpty()) return emptyList()
        return lookup(ids)
    }

    override suspend fun search(query: String): List<PodcastSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "$apiBaseUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", query)
            .addQueryParameter("country", country)
            .addQueryParameter("media", "podcast")
            .addQueryParameter("entity", "podcast")
            .addQueryParameter("limit", "25")
            .build()
        val response = getJson(url)
        parseLookupResults(response)
    }

    private fun lookup(ids: List<String>): List<PodcastSearchResult> {
        val url = "$apiBaseUrl/lookup".toHttpUrl().newBuilder()
            .addQueryParameter("id", ids.joinToString(","))
            .addQueryParameter("entity", "podcast")
            .addQueryParameter("country", country)
            .addQueryParameter("limit", ids.size.toString())
            .build()
        val lookupResults = parseLookupResults(getJson(url))
        return ids.mapNotNull { id ->
            lookupResults.firstOrNull { result ->
                result.podcast.siteUrl?.contains("id$id") == true
            }
        }
    }


    internal fun parseLookupResults(json: JSONObject): List<PodcastSearchResult> {
        val results = json.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val feedUrl = item.optString("feedUrl").takeIf(String::isNotBlank) ?: continue
                val categories = buildList {
                    item.optString("primaryGenreName").takeIf(String::isNotBlank)?.let(::add)
                    val genres = item.optJSONArray("genres")
                    if (genres != null) {
                        for (genreIndex in 0 until genres.length()) {
                            genres.optString(genreIndex).takeIf(String::isNotBlank)?.let(::add)
                        }
                    }
                }.distinct()
                val categoryIds = buildMap {
                    val primaryName = item.optString("primaryGenreName").takeIf(String::isNotBlank)
                    val primaryId = item.optString("primaryGenreId").takeIf(String::isNotBlank)
                    if (primaryName != null && primaryId != null) put(primaryName, primaryId)
                    val genreIds = item.optJSONArray("genreIds")
                    val genres = item.optJSONArray("genres")
                    if (genres != null && genreIds != null) {
                        for (genreIndex in 0 until minOf(genres.length(), genreIds.length())) {
                            val name = genres.optString(genreIndex).takeIf(String::isNotBlank)
                            val id = genreIds.optString(genreIndex).takeIf(String::isNotBlank)
                            if (name != null && id != null) put(name, id)
                        }
                    }
                }
                add(
                    PodcastSearchResult(
                        providerId = id,
                        podcast = Podcast(
                            id = podcastId(feedUrl),
                            title = item.optString("collectionName").ifBlank { feedUrl },
                            author = item.optString("artistName").takeIf(String::isNotBlank),
                            feedUrl = canonicalFeedUrl(feedUrl),
                            siteUrl = item.optString("collectionViewUrl").takeIf(String::isNotBlank),
                            descriptionHtml = null,
                            artworkUrl = item.optString("artworkUrl600").takeIf(String::isNotBlank)
                                ?: item.optString("artworkUrl100").takeIf(String::isNotBlank),
                            categories = categories,
                            categoryIds = categoryIds,
                        ),
                    ),
                )
            }
        }
    }

    private fun getJson(url: okhttp3.HttpUrl): JSONObject {
        client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", "Booth Podcast Player")
                .get()
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) error("Apple Podcasts request failed with HTTP ${response.code}")
            return JSONObject(response.body.string())
        }
    }
}
