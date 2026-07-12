package com.couchindex.core

import java.util.Locale

fun interface RatingAdapter {
    fun ratingsFor(title: Title): List<Rating>
}

class EnrichTitleRatings(
    private val adapters: List<RatingAdapter>,
) {
    fun invoke(title: Title): Title {
        if (adapters.isEmpty()) return title

        val ratingsBySource = linkedMapOf<RatingKey, Rating>()
        title.ratings.forEach { rating -> ratingsBySource[rating.key()] = rating }
        adapters.forEach { adapter ->
            runCatching { adapter.ratingsFor(title) }
                .getOrDefault(emptyList())
                .forEach { rating -> ratingsBySource[rating.key()] = rating }
        }
        return title.copy(ratings = ratingsBySource.values.toList())
    }

    private fun Rating.key(): RatingKey =
        RatingKey(source.trim().lowercase(Locale.ROOT), scope)

    private data class RatingKey(
        val source: String,
        val scope: RatingScope,
    )
}
