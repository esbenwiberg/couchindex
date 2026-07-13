package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsEligibilityPolicyTest {
    private val policy = KidsEligibilityPolicy()

    @Test
    fun `allows certification at or below configured age`() {
        assertTrue(policy.isAllowed(title(7), maximumAge = 7))
        assertTrue(policy.isAllowed(title(0), maximumAge = 7))
    }

    @Test
    fun `hides over-age and unknown certification`() {
        assertFalse(policy.isAllowed(title(11), maximumAge = 7))
        assertFalse(policy.isAllowed(title(null), maximumAge = 15))
    }

    @Test
    fun `filters the complete catalogue before surfaces consume it`() {
        val allowed = title(7, tmdbId = 1)
        val blocked = title(15, tmdbId = 2)
        val unknown = title(null, tmdbId = 3)

        assertEquals(listOf(allowed), policy.filter(listOf(allowed, blocked, unknown), maximumAge = 11))
    }

    @Test
    fun `block wins over certification and a previous allow`() {
        val title = title(7)
        val overrides = listOf(
            override(title.id, KidsOverrideDecision.Allowed, changedAt = 1),
            override(title.id, KidsOverrideDecision.Blocked, changedAt = 2),
        )

        assertFalse(policy.isAllowed(title, maximumAge = 11, overrides = overrides))
    }

    @Test
    fun `manual allow admits unknown title`() {
        val title = title(null)

        assertTrue(
            policy.isAllowed(
                title,
                maximumAge = 11,
                overrides = listOf(override(title.id, KidsOverrideDecision.Allowed)),
            ),
        )
    }

    @Test
    fun `over-age title requires explicit exception`() {
        val title = title(15)

        assertFalse(
            policy.isAllowed(
                title,
                maximumAge = 11,
                overrides = listOf(override(title.id, KidsOverrideDecision.Allowed)),
            ),
        )
        assertTrue(
            policy.isAllowed(
                title,
                maximumAge = 11,
                overrides = listOf(override(title.id, KidsOverrideDecision.Allowed, allowsOverAge = true)),
            ),
        )
    }

    private fun title(minimumAge: Int?, tmdbId: Int = 1): Title = Title(
        id = TitleId(tmdbId, MediaKind.Movie),
        name = "Title $tmdbId",
        year = 2026,
        mediaKind = MediaKind.Movie,
        runtimeMinutes = 90,
        synopsis = "",
        offers = emptyList(),
        ratings = emptyList(),
        launchTargets = emptyList(),
        certification = minimumAge?.let { ContentCertification("DK", it.toString(), it) },
    )

    private fun override(
        titleId: TitleId,
        decision: KidsOverrideDecision,
        allowsOverAge: Boolean = false,
        changedAt: Long = 1,
    ) = KidsCatalogueOverride(titleId, decision, allowsOverAge, changedAt)
}
