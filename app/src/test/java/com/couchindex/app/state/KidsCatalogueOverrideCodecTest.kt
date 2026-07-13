package com.couchindex.app.state

import com.couchindex.core.KidsCatalogueOverride
import com.couchindex.core.KidsOverrideDecision
import com.couchindex.core.MediaKind
import com.couchindex.core.TitleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KidsCatalogueOverrideCodecTest {
    private val codec = KidsCatalogueOverrideCodec()

    @Test
    fun `round trips canonical overrides and explicit age exception`() {
        val overrides = listOf(
            KidsCatalogueOverride(
                titleId = TitleId(42, MediaKind.Movie),
                decision = KidsOverrideDecision.Allowed,
                allowsOverAge = true,
                changedAtEpochMillis = 123,
                titleName = "The Example",
            ),
        )

        assertEquals(overrides, codec.decode(codec.encode(overrides)))
    }

    @Test
    fun `block wins when persisted input contains duplicate title decisions`() {
        val titleId = TitleId(42, MediaKind.Series)
        val decoded = codec.decode(
            codec.encode(
                listOf(
                    KidsCatalogueOverride(titleId, KidsOverrideDecision.Allowed, true, 200),
                    KidsCatalogueOverride(titleId, KidsOverrideDecision.Blocked, false, 100),
                ),
            ),
        )

        assertEquals(KidsOverrideDecision.Blocked, decoded.single().decision)
        assertFalse(decoded.single().allowsOverAge)
    }

    @Test
    fun `ignores malformed persisted entries`() {
        assertEquals(emptyList<KidsCatalogueOverride>(), codec.decode("[{\"tmdbId\":0}]"))
        assertEquals(emptyList<KidsCatalogueOverride>(), codec.decode("not-json"))
    }
}
