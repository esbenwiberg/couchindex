package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildHomeRowsTest {
    private val buildHomeRows = BuildHomeRows()

    @Test
    fun `continue watching is first`() {
        val rows = buildHomeRows.invoke(
            catalogue = SampleCatalogue.titles,
            subscriptions = SampleCatalogue.subscriptions,
            recentLaunches = SampleCatalogue.recentLaunches,
        )

        assertEquals("continue-watching", rows.first().id)
    }

    @Test
    fun `rows exclude titles only available from disabled subscriptions`() {
        val rows = buildHomeRows.invoke(
            catalogue = SampleCatalogue.titles,
            subscriptions = SampleCatalogue.subscriptions,
            recentLaunches = SampleCatalogue.recentLaunches,
        )

        val allVisibleTitles = rows.flatMap { it.titles }.map { it.name }.toSet()
        assertFalse("Glass Mountain should be hidden while Viaplay is disabled", "Glass Mountain" in allVisibleTitles)
        assertTrue("Long Weekend remains visible because Disney+ is enabled", "Long Weekend" in allVisibleTitles)
    }

    @Test
    fun `empty derived rows are omitted while continue watching stays first`() {
        val title = SampleCatalogue.titles.first().copy(
            runtimeMinutes = null,
            isNewOnService = false,
            isHiddenGem = false,
        )

        val rows = buildHomeRows.invoke(
            catalogue = listOf(title),
            subscriptions = SampleCatalogue.subscriptions,
            recentLaunches = emptyList(),
        )

        assertEquals(listOf("continue-watching", "highly-rated", "movies"), rows.map { it.id })
    }

    @Test
    fun `watchlist follows continue watching and preserves saved order`() {
        val watchedFirst = SampleCatalogue.titles[2]
        val watchedSecond = SampleCatalogue.titles[0]

        val rows = buildHomeRows.invoke(
            catalogue = SampleCatalogue.titles,
            subscriptions = SampleCatalogue.subscriptions,
            recentLaunches = emptyList(),
            watchlistEntries = listOf(
                WatchlistEntry(watchedFirst.id, 20),
                WatchlistEntry(watchedSecond.id, 10),
            ),
        )

        assertEquals(listOf("continue-watching", "watchlist"), rows.take(2).map { it.id })
        assertEquals(listOf(watchedFirst.id, watchedSecond.id), rows[1].titles.map { it.id })
    }

    @Test
    fun `personal recommendations follow watchlist and use explicit feedback`() {
        val liked = SampleCatalogue.titles[0].copy(genreIds = setOf(878))
        val recommended = SampleCatalogue.titles[1].copy(genreIds = setOf(878, 18))

        val rows = buildHomeRows.invoke(
            catalogue = listOf(liked, recommended),
            subscriptions = SampleCatalogue.subscriptions,
            recentLaunches = emptyList(),
            feedbackEntries = listOf(FeedbackEntry(liked.id, FeedbackValue.Liked, 10)),
        )

        assertEquals("for-you", rows[1].id)
        assertEquals(listOf(recommended.id), rows[1].titles.map { it.id })
    }
}
