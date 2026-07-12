package com.couchindex.core

class RecommendTitles {
    fun invoke(
        catalogue: List<Title>,
        feedback: List<FeedbackEntry>,
        limit: Int = 12,
    ): List<Title> {
        require(limit > 0) { "limit must be positive" }

        val titlesById = catalogue.associateBy { it.id }
        val likedGenres = feedback.genreWeights(titlesById, FeedbackValue.Liked)
        if (likedGenres.isEmpty()) return emptyList()
        val dislikedGenres = feedback.genreWeights(titlesById, FeedbackValue.Disliked)
        val ratedTitleIds = feedback.map { it.titleId }.toSet()

        return catalogue
            .asSequence()
            .filterNot { it.id in ratedTitleIds }
            .map { title ->
                val likedScore = title.genreIds.sumOf { likedGenres[it] ?: 0 }
                val dislikedPenalty = title.genreIds.sumOf { dislikedGenres[it] ?: 0 }
                title to (likedScore * LIKED_WEIGHT - dislikedPenalty * DISLIKED_WEIGHT)
            }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<Title, Int>> { it.second }.thenBy { it.first.name })
            .take(limit)
            .map { (title) -> title }
            .toList()
    }

    private fun List<FeedbackEntry>.genreWeights(
        titlesById: Map<TitleId, Title>,
        value: FeedbackValue,
    ): Map<Int, Int> =
        asSequence()
            .filter { it.value == value }
            .mapNotNull { titlesById[it.titleId] }
            .flatMap { it.genreIds.asSequence() }
            .groupingBy { it }
            .eachCount()

    companion object {
        private const val LIKED_WEIGHT = 2
        private const val DISLIKED_WEIGHT = 3
    }
}
