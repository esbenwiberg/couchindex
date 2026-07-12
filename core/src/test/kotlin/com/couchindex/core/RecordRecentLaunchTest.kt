package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordRecentLaunchTest {
    @Test
    fun `moves a relaunched title to the front without duplicates`() {
        val first = TitleId(1, MediaKind.Movie)
        val second = TitleId(2, MediaKind.Series)
        val record = RecordRecentLaunch()
        val existing = listOf(
            RecentLaunch(first, "Earlier", launchedAtEpochMillis = 10),
            RecentLaunch(second, "Later", launchedAtEpochMillis = 20),
        )

        val updated = record.invoke(existing, first, launchedAtEpochMillis = 30)

        assertEquals(listOf(first, second), updated.map { it.titleId })
        assertEquals(30L, updated.first().launchedAtEpochMillis)
    }

    @Test
    fun `caps retained launch history`() {
        val record = RecordRecentLaunch(maximumItems = 2)
        val existing = listOf(
            RecentLaunch(TitleId(1, MediaKind.Movie), "One"),
            RecentLaunch(TitleId(2, MediaKind.Movie), "Two"),
        )

        val updated = record.invoke(existing, TitleId(3, MediaKind.Movie), launchedAtEpochMillis = 30)

        assertEquals(listOf(3, 1), updated.map { it.titleId.tmdbId })
    }
}
