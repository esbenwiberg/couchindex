package com.couchindex.app.cache

import com.couchindex.core.SampleCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogueSnapshotCodecTest {
    private val snapshot = CatalogueSnapshot(
        region = "DK",
        savedAtEpochMillis = 1_700_000_000_000,
        providers = SampleCatalogue.providers,
        titles = SampleCatalogue.titles,
        genres = SampleCatalogue.genres,
    )

    @Test
    fun `round trips the complete catalogue snapshot`() {
        val decoded = CatalogueSnapshotCodec.decode(CatalogueSnapshotCodec.encode(snapshot))

        assertEquals(snapshot, decoded)
    }

    @Test
    fun `rejects malformed and unsupported snapshots`() {
        assertNull(CatalogueSnapshotCodec.decode("not json"))
        assertNull(CatalogueSnapshotCodec.decode("""{"version":4}"""))
    }

    @Test
    fun `reports cache age and stale threshold`() {
        assertEquals("30 min ago", snapshot.ageLabel(snapshot.savedAtEpochMillis + 30 * 60_000))
        assertEquals(CacheFreshness.Cached, snapshot.freshness(snapshot.savedAtEpochMillis + 23 * 60 * 60_000))
        assertEquals(CacheFreshness.Stale, snapshot.freshness(snapshot.savedAtEpochMillis + 24 * 60 * 60_000))
    }

    @Test
    fun `builds explicit cached and stale refresh failure statuses`() {
        val cached = snapshot.cacheStatus(snapshot.savedAtEpochMillis + 30 * 60_000, "Refresh failed")
        val stale = snapshot.cacheStatus(snapshot.savedAtEpochMillis + 2 * 24 * 60 * 60_000, "Refresh failed")

        assertEquals("Refresh failed; 6 cached titles - 30 min ago", cached.detail)
        assertEquals("Cached", cached.badge)
        assertEquals(true, cached.highlighted)
        assertEquals("Stale", stale.badge)
        assertEquals(false, stale.highlighted)
    }
}
