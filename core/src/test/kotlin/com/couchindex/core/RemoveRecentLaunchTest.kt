package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoveRecentLaunchTest {
    @Test
    fun `removes the selected title and preserves history order`() {
        val first = TitleId(1, MediaKind.Movie)
        val second = TitleId(2, MediaKind.Series)
        val third = TitleId(3, MediaKind.Movie)
        val existing = listOf(
            RecentLaunch(first, "First"),
            RecentLaunch(second, "Second"),
            RecentLaunch(third, "Third"),
        )

        val updated = RemoveRecentLaunch().invoke(existing, second)

        assertEquals(listOf(first, third), updated.map { it.titleId })
    }
}
