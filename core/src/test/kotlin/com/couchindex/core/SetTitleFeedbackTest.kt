package com.couchindex.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SetTitleFeedbackTest {
    private val titleId = TitleId(42, MediaKind.Movie)

    @Test
    fun `sets replaces and clears title feedback`() {
        val setFeedback = SetTitleFeedback()
        val liked = setFeedback.invoke(emptyList(), titleId, FeedbackValue.Liked, 10)
        val disliked = setFeedback.invoke(liked, titleId, FeedbackValue.Disliked, 20)
        val cleared = setFeedback.invoke(disliked, titleId, null, 30)

        assertEquals(listOf(FeedbackEntry(titleId, FeedbackValue.Liked, 10)), liked)
        assertEquals(listOf(FeedbackEntry(titleId, FeedbackValue.Disliked, 20)), disliked)
        assertEquals(emptyList<FeedbackEntry>(), cleared)
    }
}
