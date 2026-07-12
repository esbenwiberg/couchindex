package com.couchindex.core

class SetWatchedStatus(
    private val maximumItems: Int = 1_000,
) {
    init {
        require(maximumItems > 0) { "maximumItems must be positive" }
    }

    fun invoke(
        existing: List<WatchedEntry>,
        titleId: TitleId,
        isWatched: Boolean,
        changedAtEpochMillis: Long,
    ): List<WatchedEntry> {
        val withoutTitle = existing.filterNot { it.titleId == titleId }
        return if (isWatched) {
            listOf(WatchedEntry(titleId, changedAtEpochMillis))
                .plus(withoutTitle)
                .take(maximumItems)
        } else {
            withoutTitle
        }
    }
}
