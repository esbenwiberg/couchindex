package com.couchindex.app.tmdb

import com.couchindex.core.MediaKind
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

enum class TmdbDiscoverMediaType(
    val path: String,
    val mediaKind: MediaKind,
    val titleKey: String,
    val dateKey: String,
) {
    Movie(
        path = "discover/movie",
        mediaKind = MediaKind.Movie,
        titleKey = "title",
        dateKey = "release_date",
    ),
    Tv(
        path = "discover/tv",
        mediaKind = MediaKind.Series,
        titleKey = "name",
        dateKey = "first_air_date",
    ),
}

enum class TmdbProviderMediaType(val path: String) {
    Movie("watch/providers/movie"),
    Tv("watch/providers/tv"),
}

data class TmdbDiscoverQuery(
    val mediaType: TmdbDiscoverMediaType,
    val region: String = "DK",
    val tmdbProviderIds: Set<Int>,
    val page: Int = 1,
    val language: String = "en-US",
) {
    init {
        require(region.isNotBlank()) { "region must not be blank" }
        require(tmdbProviderIds.isNotEmpty()) { "tmdbProviderIds must not be empty" }
        require(page >= 1) { "page must be 1 or greater" }
    }
}

data class TmdbDiscoverPage(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<TmdbDiscoverItem>,
)

data class TmdbDiscoverItem(
    val tmdbId: Int,
    val mediaKind: MediaKind,
    val name: String,
    val year: Int?,
    val overview: String,
    val posterPath: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
)

data class TmdbWatchProvider(
    val tmdbProviderId: Int,
    val name: String,
    val displayPriority: Int,
)

fun interface TmdbDiscoverSource {
    fun fetchDiscoverPage(query: TmdbDiscoverQuery): TmdbDiscoverPage
}

fun interface TmdbProviderSource {
    fun fetchWatchProviders(mediaType: TmdbProviderMediaType, region: String): List<TmdbWatchProvider>
}

class TmdbDiscoverClient(
    private val readAccessToken: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
) : TmdbDiscoverSource, TmdbProviderSource {
    fun discoverUrl(query: TmdbDiscoverQuery): URL {
        val params = listOf(
            "language" to query.language,
            "page" to query.page.toString(),
            "sort_by" to "popularity.desc",
            "watch_region" to query.region,
            "with_watch_monetization_types" to "flatrate",
            "with_watch_providers" to query.tmdbProviderIds.joinToString("|"),
        )
        return URL("${baseUrl.trimEnd('/')}/${query.mediaType.path}?${params.toQueryString()}")
    }

    override fun fetchDiscoverPage(query: TmdbDiscoverQuery): TmdbDiscoverPage {
        check(readAccessToken.isNotBlank()) { "TMDb read access token is missing" }
        return TmdbDiscoverParser.parse(
            body = executeGet(discoverUrl(query)),
            mediaType = query.mediaType,
        )
    }

    fun watchProvidersUrl(mediaType: TmdbProviderMediaType, region: String): URL {
        require(region.isNotBlank()) { "region must not be blank" }
        val params = listOf(
            "language" to "en-US",
            "watch_region" to region,
        )
        return URL("${baseUrl.trimEnd('/')}/${mediaType.path}?${params.toQueryString()}")
    }

    override fun fetchWatchProviders(
        mediaType: TmdbProviderMediaType,
        region: String,
    ): List<TmdbWatchProvider> {
        check(readAccessToken.isNotBlank()) { "TMDb read access token is missing" }
        return TmdbProviderParser.parse(executeGet(watchProvidersUrl(mediaType, region)), region)
    }

    private fun executeGet(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $readAccessToken")
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (status !in 200..299) {
            error("TMDb request failed with HTTP $status: $body")
        }

        return body
    }

    private fun List<Pair<String, String>>.toQueryString(): String =
        joinToString("&") { (key, value) -> "${key.urlEncode()}=${value.urlEncode()}" }

    private fun String.urlEncode(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())

    companion object {
        private const val DEFAULT_BASE_URL = "https://api.themoviedb.org/3"
    }
}

object TmdbProviderParser {
    fun parse(body: String, region: String): List<TmdbWatchProvider> {
        val results = JSONObject(body).optJSONArray("results") ?: JSONArray()
        return (0 until results.length()).mapNotNull { index ->
            results.optJSONObject(index)?.toWatchProvider(region)
        }
    }

    private fun JSONObject.toWatchProvider(region: String): TmdbWatchProvider? {
        val providerId = optInt("provider_id").takeIf { it > 0 } ?: return null
        val name = optString("provider_name").takeIf { it.isNotBlank() } ?: return null
        val regionalPriority = optJSONObject("display_priorities")?.optInt(region, Int.MAX_VALUE)
        return TmdbWatchProvider(
            tmdbProviderId = providerId,
            name = name,
            displayPriority = regionalPriority ?: optInt("display_priority", Int.MAX_VALUE),
        )
    }
}

object TmdbDiscoverParser {
    fun parse(body: String, mediaType: TmdbDiscoverMediaType): TmdbDiscoverPage {
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: JSONArray()

        return TmdbDiscoverPage(
            page = json.optInt("page"),
            totalPages = json.optInt("total_pages"),
            totalResults = json.optInt("total_results"),
            results = (0 until results.length()).mapNotNull { index ->
                results.optJSONObject(index)?.toDiscoverItem(mediaType)
            },
        )
    }

    private fun JSONObject.toDiscoverItem(mediaType: TmdbDiscoverMediaType): TmdbDiscoverItem? {
        val tmdbId = optInt("id").takeIf { it > 0 } ?: return null
        val name = nullableString(mediaType.titleKey) ?: nullableString("original_title") ?: nullableString("original_name")
            ?: return null

        return TmdbDiscoverItem(
            tmdbId = tmdbId,
            mediaKind = mediaType.mediaKind,
            name = name,
            year = nullableString(mediaType.dateKey).yearOrNull(),
            overview = nullableString("overview").orEmpty(),
            posterPath = nullableString("poster_path"),
            voteAverage = optionalDouble("vote_average"),
            voteCount = optInt("vote_count").takeIf { it > 0 },
        )
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optionalDouble(key: String): Double? =
        if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }

    private fun String?.yearOrNull(): Int? =
        this?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()
}
