package com.couchindex.app.tv

import com.couchindex.core.SampleCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvHomeChannelContentTest {
    @Test
    fun `builds a bounded unique programme row with stable app links`() {
        val duplicate = SampleCatalogue.titles.first()
        val programmes = buildTvHomeProgrammes(SampleCatalogue.titles + duplicate, maximumCount = 4)

        assertEquals(4, programmes.size)
        assertEquals(programmes.size, programmes.map { it.titleId }.distinct().size)
        assertTrue(programmes.take(3).all { programme ->
            SampleCatalogue.titles.first { it.id == programme.titleId }.isNewOnService
        })
        assertEquals(
            programmes.first().titleId,
            CouchIndexDeepLinks.parseTitleId(CouchIndexDeepLinks.titleUri(programmes.first().titleId)),
        )
    }

    @Test
    fun `returns no cards for missing catalogue data`() {
        assertTrue(buildTvHomeProgrammes(emptyList()).isEmpty())
        assertTrue(buildTvHomeProgrammes(SampleCatalogue.titles, maximumCount = 0).isEmpty())
    }
}
