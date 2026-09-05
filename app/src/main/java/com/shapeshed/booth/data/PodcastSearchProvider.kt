package com.shapeshed.booth.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import java.text.Normalizer
import java.util.Locale

class PodcastIndexSearchProvider(
    private val client: OkHttpClient,
    private val credentialsStore: PodcastIndexCredentialsStore,
    private val localeProvider: () -> Locale = { Locale.getDefault() },
    // This public search endpoint does not require credentials.
    private val baseUrl: String = "https://api.podcastindex.org/search",
) : PodcastSearchProvider, PodcastDiscoveryProvider {
    override val supportsCategoryPaging: Boolean = true
    @Volatile
    private var categoryIdsByName: Map<String, String>? = null
    @Volatile
    private var categoryNamesById: Map<String, String>? = null
    override val id: String = "podcast-index"
    override val displayName: String = "Podcast Index"
    override val supportsPopularPodcasts: Boolean
        get() = credentialsStore.credentials.value != null

    override suspend fun search(query: String): List<PodcastSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = baseUrl.toHttpUrl().newBuilder().addQueryParameter("term", query).build()
        client.newCall(request(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Podcast search failed with HTTP ${response.code}")
            val body = response.body.string()
            val feeds = JSONObject(body).optJSONArray("results") ?: return@use emptyList()
            buildList {
                for (index in 0 until feeds.length()) {
                    val item = feeds.optJSONObject(index) ?: continue
                    val feedUrl = item.optString("feedUrl").takeIf(String::isNotBlank) ?: continue
                    add(parsePublicSearchResult(item, feedUrl))
                }
            }
        }
    }

    override suspend fun enrich(result: PodcastSearchResult): PodcastSearchResult = withContext(Dispatchers.IO) {
        if (credentialsStore.credentials.value == null) {
            Log.w(TAG, "enrich skipped; Podcast Index credentials unavailable")
            return@withContext result
        }
        runCatching { loadCategoryIds() }
        val url = "https://api.podcastindex.org/api/1.0/podcasts/byfeedurl".toHttpUrl()
            .newBuilder()
            .addQueryParameter("url", result.podcast.feedUrl)
            .build()
        client.newCall(request(url).build()).execute().use { response ->
            Log.d(TAG, "enrich request url=$url status=${response.code}")
            if (!response.isSuccessful) return@use result
            val feed = JSONObject(response.body.string()).optJSONObject("feed") ?: return@use result
            val categoryIds = parseProviderCategoryIds(feed)
            result.copy(
                podcast = result.podcast.copy(
                    categories = parseProviderCategoryNames(feed),
                    categoryIds = categoryIds.ifEmpty { result.podcast.categoryIds },
                ),
            )
        }
    }

    override suspend fun browse(shelf: PodcastDiscoveryShelf): List<PodcastSearchResult> = withContext(Dispatchers.IO) {
        if (!supportsPopularPodcasts) return@withContext emptyList()
        runCatching { loadCategoryIds() }
        val url = "https://api.podcastindex.org/api/1.0/podcasts/trending".toHttpUrl()
            .newBuilder()
            .addQueryParameter("max", "25")
            // Include the regional language first, then the base language because
            // many feeds publish only `en`/`fr` rather than a regional tag.
            .addQueryParameter("lang", podcastIndexLanguageFilter(localeProvider()))
            .build()
        client.newCall(request(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Podcast discovery failed with HTTP ${response.code}")
            val feeds = JSONObject(response.body.string()).optJSONArray("feeds") ?: return@use emptyList()
            buildList { for (index in 0 until feeds.length()) feeds.optJSONObject(index)?.let { add(parseResult(it, it.optString("url"))) } }
        }
    }

    override suspend fun browse(category: PodcastDiscoveryCategory): List<PodcastSearchResult> = withContext(Dispatchers.IO) {
        if (!supportsPopularPodcasts) return@withContext emptyList()
        runCatching { loadCategoryIds() }
        val categoryId = resolveCategoryId(category)
            ?: run {
                Log.w(TAG, "Skipping category request; no Podcast Index ID for id=${category.id} title=${category.title}")
                return@withContext emptyList()
            }
        val url = "https://api.podcastindex.org/api/1.0/recent/feeds".toHttpUrl()
            .newBuilder()
            .addQueryParameter("max", "100")
            .addQueryParameter("cat", categoryId)
            .addQueryParameter("lang", podcastIndexLanguageFilter(localeProvider()))
            .build()
        client.newCall(request(url).build()).execute().use { response ->
            Log.d(TAG, "category request url=$url status=${response.code}")
            if (!response.isSuccessful) {
                Log.e(TAG, "category request failed status=${response.code}")
                error("Podcast category request failed with HTTP ${response.code}")
            }
            val feeds = JSONObject(response.body.string()).optJSONArray("feeds") ?: return@use emptyList()
            buildList { for (index in 0 until feeds.length()) feeds.optJSONObject(index)?.let { add(parseResult(it, it.optString("url"))) } }
        }
    }

    override suspend fun browse(category: PodcastDiscoveryCategory, offset: Int): List<PodcastSearchResult> = withContext(Dispatchers.IO) {
        if (!supportsPopularPodcasts) return@withContext emptyList()
        runCatching { loadCategoryIds() }
        val categoryId = resolveCategoryId(category)
            ?: return@withContext emptyList()
        val url = "https://api.podcastindex.org/api/1.0/recent/feeds".toHttpUrl()
            .newBuilder()
            .addQueryParameter("max", "100")
            .addQueryParameter("cat", categoryId)
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("lang", podcastIndexLanguageFilter(localeProvider()))
            .build()
        client.newCall(request(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Podcast category request failed with HTTP ${response.code}")
            val feeds = JSONObject(response.body.string()).optJSONArray("feeds") ?: return@use emptyList()
            buildList { for (index in 0 until feeds.length()) feeds.optJSONObject(index)?.let { add(parseResult(it, it.optString("url"))) } }
        }
    }

    private fun resolveCategoryId(category: PodcastDiscoveryCategory): String? {
        category.id.toIntOrNull()?.let { return category.id }
        val knownCategories = categoryIdsByName ?: loadCategoryIds()
        return listOf(category.id, category.title)
            .asSequence()
            .map(::normalizeCategoryName)
            .mapNotNull(knownCategories::get)
            .firstOrNull()
    }

    private fun loadCategoryIds(): Map<String, String> {
        categoryIdsByName?.let { return it }
        val url = "https://api.podcastindex.org/api/1.0/categories/list".toHttpUrl()
        var namesById = emptyMap<String, String>()
        val loaded = client.newCall(request(url).build()).execute().use { response ->
            if (!response.isSuccessful) error("Podcast categories request failed with HTTP ${response.code}")
            val categories = JSONObject(response.body.string()).optJSONArray("feeds") ?: return@use emptyMap()
            buildMap {
                for (index in 0 until categories.length()) {
                    val category = categories.optJSONObject(index) ?: continue
                    val id = category.optString("id").takeIf(String::isNotBlank) ?: continue
                    val name = category.optString("name").takeIf(String::isNotBlank) ?: continue
                    namesById = namesById + (id to name)
                    put(normalizeCategoryName(name), id)
                }
            }
        }
        categoryNamesById = namesById
        categoryIdsByName = loaded
        return loaded
    }

    private fun normalizeCategoryName(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .trim()
        .lowercase(Locale.ROOT)
        .replace("&", "and")
        .replace(Regex("\\s+"), " ")


    private fun request(url: okhttp3.HttpUrl): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "Booth/1.0 (Android podcast player; com.shapeshed.booth)")
            .get()
        credentialsStore.credentials.value?.let { credentials ->
            val date = Instant.now().epochSecond.toString()
            val authorization = sha1(credentials.apiKey + credentials.apiSecret + date)
            builder.header("X-Auth-Key", credentials.apiKey)
                .header("X-Auth-Date", date)
                .header("Authorization", authorization)
        }
        return builder
    }

    private fun parseResult(item: JSONObject, feedUrl: String): PodcastSearchResult {
        val canonicalUrl = canonicalFeedUrl(feedUrl)
        return PodcastSearchResult(
            providerId = id,
            podcast = Podcast(
                id = item.optLong("id", podcastId(canonicalUrl)),
                title = item.optString("title").ifBlank { canonicalUrl },
                author = item.optString("author").takeIf(String::isNotBlank),
                feedUrl = canonicalUrl,
                siteUrl = item.optString("link").takeIf(String::isNotBlank),
                descriptionHtml = item.optString("description").takeIf(String::isNotBlank),
                artworkUrl = item.optString("image").takeIf(String::isNotBlank),
                categories = parseProviderCategoryNames(item),
                categoryIds = parseProviderCategoryIds(item),
            ),
        )
    }

    private fun parsePublicSearchResult(item: JSONObject, feedUrl: String): PodcastSearchResult {
        val canonicalUrl = canonicalFeedUrl(feedUrl)
        return PodcastSearchResult(
            providerId = id,
            podcast = Podcast(
                id = item.optLong("collectionId", podcastId(canonicalUrl)),
                title = item.optString("collectionName")
                    .ifBlank { item.optString("title") }
                    .ifBlank { canonicalUrl },
                author = item.optString("artistName")
                    .ifBlank { item.optString("author") }
                    .takeIf(String::isNotBlank),
                feedUrl = canonicalUrl,
                siteUrl = item.optString("collectionViewUrl")
                    .ifBlank { item.optString("link") }
                    .takeIf(String::isNotBlank),
                descriptionHtml = item.optString("description").takeIf(String::isNotBlank),
                artworkUrl = item.optString("artworkUrl600")
                    .ifBlank { item.optString("artworkUrl100") }
                    .ifBlank { item.optString("image") }
                    .takeIf(String::isNotBlank),
                // The public Apple-compatible response exposes display genres such as
                // "Podcasts" which are not Podcast Index category IDs. Wait for enrich()
                // to provide the provider's canonical category map.
                categories = parseProviderCategoryNames(item),
                categoryIds = parseProviderCategoryIds(item),
            ),
        )
    }

    private fun parseProviderCategoryIds(item: JSONObject): Map<String, String> = buildMap {
        fun addAlias(name: String?, id: String?) {
            val categoryName = name?.takeIf(String::isNotBlank) ?: return
            val categoryId = id?.takeIf(String::isNotBlank) ?: return
            if (categoryId.toIntOrNull() == null) return
            put(categoryName, categoryId)
            categoryNamesById?.get(categoryId)?.let { canonicalName -> put(canonicalName, categoryId) }
        }

        item.optJSONObject("categories")?.let { categories ->
            categories.keys().forEach { key ->
                val value = categories.optString(key).takeIf(String::isNotBlank) ?: return@forEach
                when {
                    key.toIntOrNull() != null -> addAlias(value, key)
                    value.toIntOrNull() != null -> addAlias(key, value)
                }
            }
        }
        item.optJSONArray("categories")?.let { categories ->
            for (index in 0 until categories.length()) {
                val category = categories.optJSONObject(index) ?: continue
                addAlias(
                    name = category.optString("name"),
                    id = category.optString("id"),
                )
            }
        }
    }

    private fun parseProviderCategoryNames(item: JSONObject): List<String> {
        val canonicalNames = categoryNamesById?.values?.toSet().orEmpty()
        return parseProviderCategoryIds(item).keys.filter { it in canonicalNames }.distinct()
    }

    private fun sha1(value: String): String = MessageDigest.getInstance("SHA-1")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "PodcastIndex"
    }
}

/**
 * Podcast Index accepts comma-separated language filters. Keep a regional tag when present,
 * but include its base language because feed publishers frequently omit the region.
 */
internal fun podcastIndexLanguageFilter(locale: Locale): String {
    val language = locale.language.lowercase(Locale.ROOT).trim()
    if (language.isBlank()) return ""

    val regional = locale.toLanguageTag()
        .lowercase(Locale.ROOT)
        .takeIf { it.isNotBlank() && it != "und" && it != language }

    return listOfNotNull(regional, language).distinct().joinToString(",")
}
