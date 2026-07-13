package com.couchindex.app.cache

import com.couchindex.core.ContentCertification
import com.couchindex.core.Genre
import com.couchindex.core.LaunchTarget
import com.couchindex.core.MediaKind
import com.couchindex.core.MonetizationType
import com.couchindex.core.Offer
import com.couchindex.core.Provider
import com.couchindex.core.Rating
import com.couchindex.core.RatingScope
import com.couchindex.core.Title
import com.couchindex.core.TitleId
import org.json.JSONArray
import org.json.JSONObject

object CatalogueSnapshotCodec {
    fun encode(snapshot: CatalogueSnapshot): String =
        JSONObject()
            .put("version", VERSION)
            .put("region", snapshot.region)
            .put("savedAtEpochMillis", snapshot.savedAtEpochMillis)
            .put("providers", JSONArray().apply { snapshot.providers.forEach { put(it.toJson()) } })
            .put("genres", JSONArray().apply { snapshot.genres.forEach { put(it.toJson()) } })
            .put("titles", JSONArray().apply { snapshot.titles.forEach { put(it.toJson()) } })
            .toString()

    fun decode(body: String): CatalogueSnapshot? = runCatching {
        val json = JSONObject(body)
        if (json.optInt("version") != VERSION) return null
        val region = json.optString("region").takeIf { it.isNotBlank() } ?: return null
        val savedAt = json.optLong("savedAtEpochMillis").takeIf { it > 0 } ?: return null
        val providers = json.optJSONArray("providers").objects().mapNotNull { it.toProvider() }
        val genres = json.optJSONArray("genres").objects().mapNotNull { it.toGenre() }
        val titles = json.optJSONArray("titles").objects().mapNotNull { it.toTitle() }
        if (providers.isEmpty() || genres.isEmpty() || titles.isEmpty()) return null
        CatalogueSnapshot(region, savedAt, providers, titles, genres)
    }.getOrNull()

    private fun Provider.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("shortName", shortName)
            .putNullable("tmdbProviderId", tmdbProviderId)
            .put("displayPriority", displayPriority)
            .putNullable("androidPackageName", androidPackageName)

    private fun JSONObject.toProvider(): Provider? {
        val id = optionalString("id") ?: return null
        val name = optionalString("name") ?: return null
        return Provider(
            id = id,
            name = name,
            shortName = optionalString("shortName") ?: name,
            tmdbProviderId = optionalInt("tmdbProviderId"),
            displayPriority = optionalInt("displayPriority") ?: Int.MAX_VALUE,
            androidPackageName = optionalString("androidPackageName"),
        )
    }

    private fun Genre.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("mediaKinds", JSONArray(mediaKinds.map { it.name }))

    private fun JSONObject.toGenre(): Genre? {
        val id = optionalInt("id")?.takeIf { it > 0 } ?: return null
        val name = optionalString("name") ?: return null
        val mediaKinds = optJSONArray("mediaKinds").strings()
            .mapNotNull { value -> runCatching { MediaKind.valueOf(value) }.getOrNull() }
            .toSet()
        if (mediaKinds.isEmpty()) return null
        return Genre(id, name, mediaKinds)
    }

    private fun Title.toJson(): JSONObject =
        JSONObject()
            .put("tmdbId", id.tmdbId)
            .put("mediaKind", mediaKind.name)
            .put("name", name)
            .putNullable("year", year)
            .putNullable("releaseDate", releaseDate)
            .putNullable("runtimeMinutes", runtimeMinutes)
            .put("synopsis", synopsis)
            .put("offers", JSONArray().apply { offers.forEach { put(it.toJson()) } })
            .put("ratings", JSONArray().apply { ratings.forEach { put(it.toJson()) } })
            .put("launchTargets", JSONArray().apply { launchTargets.forEach { put(it.toJson()) } })
            .put("isNewOnService", isNewOnService)
            .put("isHiddenGem", isHiddenGem)
            .put("externalIds", JSONObject(externalIds))
            .putNullable("posterUrl", posterUrl)
            .put("genreIds", JSONArray(genreIds.toList()))
            .putNullable("certification", certification?.toJson())

    private fun JSONObject.toTitle(): Title? {
        val tmdbId = optInt("tmdbId").takeIf { it > 0 } ?: return null
        val mediaKind = enumValue<MediaKind>("mediaKind") ?: return null
        val name = optionalString("name") ?: return null
        return Title(
            id = TitleId(tmdbId, mediaKind),
            name = name,
            year = optionalInt("year"),
            releaseDate = optionalString("releaseDate"),
            mediaKind = mediaKind,
            runtimeMinutes = optionalInt("runtimeMinutes"),
            synopsis = optString("synopsis"),
            offers = optJSONArray("offers").objects().mapNotNull { it.toOffer() },
            ratings = optJSONArray("ratings").objects().mapNotNull { it.toRating() },
            launchTargets = optJSONArray("launchTargets").objects().mapNotNull { it.toLaunchTarget() },
            isNewOnService = optBoolean("isNewOnService"),
            isHiddenGem = optBoolean("isHiddenGem"),
            externalIds = optJSONObject("externalIds")?.stringMap().orEmpty(),
            posterUrl = optionalString("posterUrl"),
            genreIds = optJSONArray("genreIds").ints().toSet(),
            certification = optJSONObject("certification")?.toCertification(),
        )
    }

    private fun ContentCertification.toJson(): JSONObject =
        JSONObject()
            .put("countryCode", countryCode)
            .put("rating", rating)
            .put("minimumAge", minimumAge)

    private fun JSONObject.toCertification(): ContentCertification? {
        val countryCode = optionalString("countryCode") ?: return null
        val rating = optionalString("rating") ?: return null
        val minimumAge = optionalInt("minimumAge")?.takeIf { it >= 0 } ?: return null
        return ContentCertification(countryCode, rating, minimumAge)
    }

    private fun Offer.toJson(): JSONObject =
        JSONObject()
            .put("providerId", providerId)
            .put("region", region)
            .put("monetizationType", monetizationType.name)

    private fun JSONObject.toOffer(): Offer? {
        val providerId = optionalString("providerId") ?: return null
        val region = optionalString("region") ?: return null
        val type = enumValue<MonetizationType>("monetizationType") ?: return null
        return Offer(providerId, region, type)
    }

    private fun Rating.toJson(): JSONObject =
        JSONObject()
            .put("source", source)
            .put("value", value)
            .put("scale", scale)
            .putNullable("voteCount", voteCount)
            .put("scope", scope.name)
            .put("retrievedAt", retrievedAt)

    private fun JSONObject.toRating(): Rating? {
        val source = optionalString("source") ?: return null
        val value = optionalDouble("value") ?: return null
        val scale = optionalDouble("scale")?.takeIf { it > 0 } ?: return null
        val scope = enumValue<RatingScope>("scope") ?: return null
        return Rating(source, value, scale, optionalInt("voteCount"), scope, optString("retrievedAt"))
    }

    private fun LaunchTarget.toJson(): JSONObject =
        JSONObject()
            .put("providerId", providerId)
            .put("label", label)
            .putNullable("uri", uri)
            .putNullable("androidPackageName", androidPackageName)

    private fun JSONObject.toLaunchTarget(): LaunchTarget? {
        val providerId = optionalString("providerId") ?: return null
        val label = optionalString("label") ?: return null
        return LaunchTarget(providerId, label, optionalString("uri"), optionalString("androidPackageName"))
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optionalInt(key: String): Int? =
        if (isNull(key) || !has(key)) null else optInt(key)

    private fun JSONObject.optionalDouble(key: String): Double? =
        if (isNull(key) || !has(key)) null else optDouble(key).takeIf { !it.isNaN() }

    private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String): T? =
        runCatching { enumValueOf<T>(optString(key)) }.getOrNull()

    private fun JSONObject.stringMap(): Map<String, String> =
        keys().asSequence().mapNotNull { key -> optionalString(key)?.let { key to it } }.toMap()

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull(::optJSONObject)

    private fun JSONArray?.ints(): List<Int> =
        if (this == null) emptyList() else (0 until length()).map { optInt(it) }.filter { it > 0 }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    private const val VERSION = 3
}
