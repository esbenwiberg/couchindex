package com.couchindex.app.settings

import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentPinHasherTest {
    private val hasher = ParentPinHasher()

    @Test
    fun `validates exactly four digits`() {
        assertTrue(ParentPinHasher.isValidPin("2048"))
        assertFalse(ParentPinHasher.isValidPin("123"))
        assertFalse(ParentPinHasher.isValidPin("12a4"))
    }

    @Test
    fun `salted hashes verify without storing the PIN`() {
        val salt = ByteArray(16) { it.toByte() }
        val expected = hasher.hash("2048", salt)

        assertTrue(MessageDigest.isEqual(expected, hasher.hash("2048", salt)))
        assertFalse(MessageDigest.isEqual(expected, hasher.hash("2049", salt)))
    }
}
