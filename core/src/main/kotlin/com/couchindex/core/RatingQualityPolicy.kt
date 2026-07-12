package com.couchindex.core

import kotlin.math.ln

class RatingQualityPolicy(
    private val minimumScore: Double = 0.7,
    private val minimumVotes: Int = 100,
    private val hiddenGemMaximumVotes: Int = 5_000,
) {
    init {
        require(minimumScore in 0.0..1.0) { "minimumScore must be normalized" }
        require(minimumVotes > 0) { "minimumVotes must be positive" }
        require(hiddenGemMaximumVotes >= minimumVotes) { "hiddenGemMaximumVotes must include minimumVotes" }
    }

    fun highlyRated(titles: List<Title>, limit: Int = 12): List<Title> =
        titles.withSignals()
            .filter { (_, signal) -> signal.normalizedScore >= minimumScore && signal.voteCount >= minimumVotes }
            .sortedByDescending { (_, signal) -> signal.confidence }
            .take(limit)
            .map { (title) -> title }

    fun hiddenGems(titles: List<Title>, limit: Int = 12): List<Title> =
        titles.mapNotNull { title ->
            val signal = title.bestSignal()
            if (title.isHiddenGem || signal?.isHiddenGem() == true) title to signal else null
        }
            .sortedByDescending { (_, signal) -> signal?.confidence ?: 0.0 }
            .take(limit)
            .map { (title) -> title }

    private fun List<Title>.withSignals(): List<Pair<Title, RatingSignal>> =
        mapNotNull { title -> title.bestSignal()?.let { title to it } }

    private fun Title.bestSignal(): RatingSignal? =
        ratings.mapNotNull { rating ->
            val votes = rating.voteCount?.takeIf { it > 0 } ?: return@mapNotNull null
            if (rating.scale <= 0.0) return@mapNotNull null
            val normalized = (rating.value / rating.scale).coerceIn(0.0, 1.0)
            RatingSignal(
                normalizedScore = normalized,
                voteCount = votes,
                confidence = normalized * ln(votes.toDouble() + 1.0),
            )
        }.maxByOrNull { it.confidence }

    private fun RatingSignal.isHiddenGem(): Boolean =
        normalizedScore >= minimumScore && voteCount in minimumVotes..hiddenGemMaximumVotes

    private data class RatingSignal(
        val normalizedScore: Double,
        val voteCount: Int,
        val confidence: Double,
    )
}
