package com.couchindex.app.state

import com.couchindex.core.ViewerProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileStorageTest {
    @Test
    fun `adult keeps legacy storage and kids receives isolated storage`() {
        assertEquals("couchindex-watchlist", profiledPreferencesName("couchindex-watchlist", ViewerProfile.Adult))
        assertEquals("couchindex-watchlist-kids", profiledPreferencesName("couchindex-watchlist", ViewerProfile.Kids))
    }
}
