package com.couchindex.app.config

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyTest {
    @Test
    fun `policy URL is a stable public HTTPS page`() {
        val uri = URI(PrivacyPolicy.URL)

        assertEquals("https", uri.scheme)
        assertEquals("esbenwiberg.github.io", uri.host)
        assertEquals("/couchindex/privacy/", uri.path)
        assertTrue(uri.query == null && uri.fragment == null)
    }
}
