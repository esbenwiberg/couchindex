package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SetWatchedStatusTest {
    private val first = TitleId(1, MediaKind.Movie)
    private val second = TitleId(2, MediaKind.Series)

    @Test
    fun `marking watched moves the title first without duplicates`() {
        val existing = listOf(
            WatchedEntry(first, 10),
            WatchedEntry(second, 20),
        )

        val updated = SetWatchedStatus().invoke(existing, first, isWatched = true, changedAtEpochMillis = 30)

        assertEquals(listOf(first, second), updated.map { it.titleId })
        assertEquals(30L, updated.first().watchedAtEpochMillis)
    }

    @Test
    fun `marking unwatched preserves the remaining history order`() {
        val existing = listOf(
            WatchedEntry(first, 10),
            WatchedEntry(second, 20),
        )

        val updated = SetWatchedStatus().invoke(existing, first, isWatched = false, changedAtEpochMillis = 30)

        assertEquals(listOf(second), updated.map { it.titleId })
    }
}
