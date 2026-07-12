package com.couchindex.core

import java.util.Locale

fun interface RatingAdapter {
    fun ratingsFor(title: Title): List<Rating>
}

fun interface BatchRatingAdapter {
    fun ratingsFor(titles: List<Title>): Map<TitleId, List<Rating>>
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

class EnrichTitleBatchRatings(
    private val adapters: List<BatchRatingAdapter>,
) {
    fun invoke(titles: List<Title>): List<Title> {
        if (adapters.isEmpty()) return titles

        val ratingsByTitle = mutableMapOf<TitleId, MutableList<Rating>>()
        adapters.forEach { adapter ->
            runCatching { adapter.ratingsFor(titles) }
                .getOrDefault(emptyMap())
                .forEach { (titleId, ratings) ->
                    ratingsByTitle.getOrPut(titleId, ::mutableListOf).addAll(ratings)
                }
        }
        return titles.map { title ->
            val ratings = ratingsByTitle[title.id].orEmpty()
            EnrichTitleRatings(listOf(RatingAdapter { ratings })).invoke(title)
        }
    }
}
