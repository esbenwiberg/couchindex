package com.couchindex.app.tv

import com.couchindex.core.MediaKind
import com.couchindex.core.TitleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CouchIndexDeepLinksTest {
    @Test
    fun `round trips movie and series title links`() {
        listOf(
            TitleId(550, MediaKind.Movie),
            TitleId(1396, MediaKind.Series),
        ).forEach { titleId ->
            assertEquals(titleId, CouchIndexDeepLinks.parseTitleId(CouchIndexDeepLinks.titleUri(titleId)))
        }
    }

    @Test
    fun `rejects unsupported or malformed links`() {
        assertNull(CouchIndexDeepLinks.parseTitleId(null))
        assertNull(CouchIndexDeepLinks.parseTitleId(CouchIndexDeepLinks.HOME_URI))
        assertNull(CouchIndexDeepLinks.parseTitleId("https://title/movie/550"))
        assertNull(CouchIndexDeepLinks.parseTitleId("couchindex://title/episode/550"))
        assertNull(CouchIndexDeepLinks.parseTitleId("couchindex://title/movie/-1"))
        assertNull(CouchIndexDeepLinks.parseTitleId("couchindex://title/movie/550/extra"))
        assertEquals(
            TitleId(550, MediaKind.Movie),
            CouchIndexDeepLinks.parseTitleId("COUCHINDEX://TITLE/MOVIE/550"),
        )
    }
}
