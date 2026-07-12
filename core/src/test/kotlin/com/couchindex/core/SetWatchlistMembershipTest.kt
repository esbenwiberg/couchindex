package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SetWatchlistMembershipTest {
    private val first = TitleId(1, MediaKind.Movie)
    private val second = TitleId(2, MediaKind.Series)

    @Test
    fun `adding a title places it first without duplicates`() {
        val setMembership = SetWatchlistMembership()
        val existing = listOf(
            WatchlistEntry(first, 10),
            WatchlistEntry(second, 20),
        )

        val updated = setMembership.invoke(existing, first, isMember = true, changedAtEpochMillis = 30)

        assertEquals(listOf(first, second), updated.map { it.titleId })
        assertEquals(30L, updated.first().addedAtEpochMillis)
    }

    @Test
    fun `removing a title preserves the remaining order`() {
        val setMembership = SetWatchlistMembership()
        val existing = listOf(
            WatchlistEntry(first, 10),
            WatchlistEntry(second, 20),
        )

        val updated = setMembership.invoke(existing, first, isMember = false, changedAtEpochMillis = 30)

        assertEquals(listOf(second), updated.map { it.titleId })
    }
}
