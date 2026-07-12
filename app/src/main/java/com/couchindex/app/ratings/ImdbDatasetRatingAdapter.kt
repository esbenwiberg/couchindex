package com.couchindex.app.ratings

import android.content.Context
import com.couchindex.core.BatchRatingAdapter
import com.couchindex.core.Rating
import com.couchindex.core.RatingScope
import com.couchindex.core.Title
import com.couchindex.core.TitleId
import java.io.File
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.zip.GZIPInputStream

class ImdbDatasetRatingAdapter(
    context: Context,
    private val datasetUrl: URL = URL(DEFAULT_DATASET_URL),
    private val now: () -> Long = System::currentTimeMillis,
    private val retrievedAt: () -> String = { Instant.now().toString() },
) : BatchRatingAdapter {
    private val cacheFile = File(context.cacheDir, "imdb/title.ratings.tsv.gz")

    override fun ratingsFor(titles: List<Title>): Map<TitleId, List<Rating>> {
        val titleIdsByImdbId = titles.mapNotNull { title ->
            title.externalIds["imdb"]?.let { imdbId -> imdbId to title.id }
        }.toMap()
        if (titleIdsByImdbId.isEmpty()) return emptyMap()

        val ratingsByImdbId = GZIPInputStream(currentDataset().inputStream()).bufferedReader().use { reader ->
            ImdbRatingsTsvParser.parse(reader, titleIdsByImdbId.keys, retrievedAt())
        }
        return ratingsByImdbId.mapNotNull { (imdbId, rating) ->
            titleIdsByImdbId[imdbId]?.let { titleId -> titleId to listOf(rating) }
        }.toMap()
    }

    private fun currentDataset(): File {
        val fresh = cacheFile.isFile && now() - cacheFile.lastModified() < CACHE_MAX_AGE_MILLIS
        if (fresh) return cacheFile

        cacheFile.parentFile?.mkdirs()
        val temporary = File(cacheFile.parentFile, "${cacheFile.name}.download")
        val connection = (datasetUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
        }
        check(connection.responseCode in 200..299) { "IMDb dataset request failed with HTTP ${connection.responseCode}" }
        connection.inputStream.use { input -> temporary.outputStream().use(input::copyTo) }
        temporary.copyTo(cacheFile, overwrite = true)
        temporary.delete()
        return cacheFile
    }

    companion object {
        private const val DEFAULT_DATASET_URL = "https://datasets.imdbws.com/title.ratings.tsv.gz"
        private const val CACHE_MAX_AGE_MILLIS = 24 * 60 * 60 * 1_000L
    }
}

object ImdbRatingsTsvParser {
    fun parse(
        reader: Reader,
        requestedIds: Set<String>,
        retrievedAt: String,
    ): Map<String, Rating> {
        if (requestedIds.isEmpty()) return emptyMap()

        val remaining = requestedIds.toMutableSet()
        val ratings = linkedMapOf<String, Rating>()
        for (line in reader.buffered().lineSequence()) {
            if (remaining.isEmpty()) break
            if (line.startsWith("tconst\t")) continue
            val columns = line.split('\t')
            if (columns.size < 3) continue
            val imdbId = columns[0]
            if (imdbId !in remaining) continue
            val value = columns[1].toDoubleOrNull() ?: continue
            val votes = columns[2].toIntOrNull() ?: continue
            ratings[imdbId] = Rating(
                source = "IMDb",
                value = value,
                scale = 10.0,
                voteCount = votes,
                scope = RatingScope.Title,
                retrievedAt = retrievedAt,
            )
            remaining.remove(imdbId)
        }
        return ratings
    }
}
