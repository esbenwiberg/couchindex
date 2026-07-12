package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCatalogueTest {
    private val search = SearchCatalogue()

    @Test
    fun `matches title and synopsis terms without case sensitivity`() {
        val titles = SampleCatalogue.titles

        assertEquals(listOf("Kitchen Shift"), search.invoke(titles, "KITCHEN").map { it.name })
        assertEquals(listOf("Northern Signal"), search.invoke(titles, "lost broadcast").map { it.name })
    }

    @Test
    fun `requires every query term and ranks title prefixes first`() {
        val results = search.invoke(SampleCatalogue.titles, "long family")

        assertEquals(listOf("Long Weekend"), results.map { it.name })
    }

    @Test
    fun `returns no results for a blank query`() {
        assertTrue(search.invoke(SampleCatalogue.titles, "   ").isEmpty())
    }
}
