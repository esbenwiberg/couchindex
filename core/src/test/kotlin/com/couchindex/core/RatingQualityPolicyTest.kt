package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingQualityPolicyTest {
    private val policy = RatingQualityPolicy()

    @Test
    fun `highly rated requires score and vote support`() {
        val supported = title(1, "Supported", score = 7.6, votes = 10_000)
        val tinySample = title(2, "Tiny sample", score = 9.9, votes = 12)
        val lowScore = title(3, "Low score", score = 6.9, votes = 100_000)

        val ranked = policy.highlyRated(listOf(tinySample, lowScore, supported))

        assertEquals(listOf(supported.id), ranked.map { it.id })
    }

    @Test
    fun `confidence rewards meaningful vote support`() {
        val broadSupport = title(1, "Broad support", score = 7.5, votes = 10_000)
        val narrowerSupport = title(2, "Narrower support", score = 8.0, votes = 500)

        val ranked = policy.highlyRated(listOf(narrowerSupport, broadSupport))

        assertEquals(listOf(broadSupport.id, narrowerSupport.id), ranked.map { it.id })
    }

    @Test
    fun `hidden gems combine quality with moderate exposure`() {
        val gem = title(1, "Gem", score = 8.0, votes = 800)
        val tooPopular = title(2, "Too popular", score = 8.0, votes = 10_000)
        val tooLow = title(3, "Too low", score = 6.9, votes = 500)
        val editorial = title(4, "Editorial", score = null, votes = null, isHiddenGem = true)

        val ranked = policy.hiddenGems(listOf(tooPopular, editorial, tooLow, gem))

        assertEquals(listOf(gem.id, editorial.id), ranked.map { it.id })
    }

    private fun title(
        id: Int,
        name: String,
        score: Double?,
        votes: Int?,
        isHiddenGem: Boolean = false,
    ): Title = SampleCatalogue.titles.first().copy(
        id = TitleId(id, MediaKind.Movie),
        name = name,
        ratings = score?.let {
            listOf(
                Rating(
                    source = "Test",
                    value = it,
                    scale = 10.0,
                    voteCount = votes,
                    scope = RatingScope.Title,
                    retrievedAt = "2026-07-12",
                ),
            )
        }.orEmpty(),
        isHiddenGem = isHiddenGem,
    )
}
