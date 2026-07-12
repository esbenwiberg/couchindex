package com.couchindex.core

class SetTitleFeedback(
    private val maximumItems: Int = 1_000,
) {
    init {
        require(maximumItems > 0) { "maximumItems must be positive" }
    }

    fun invoke(
        existing: List<FeedbackEntry>,
        titleId: TitleId,
        value: FeedbackValue?,
        changedAtEpochMillis: Long,
    ): List<FeedbackEntry> {
        val withoutTitle = existing.filterNot { it.titleId == titleId }
        return if (value == null) {
            withoutTitle
        } else {
            listOf(FeedbackEntry(titleId, value, changedAtEpochMillis))
                .plus(withoutTitle)
                .take(maximumItems)
        }
    }
}
