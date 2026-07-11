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
}
