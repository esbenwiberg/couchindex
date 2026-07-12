package com.couchindex.core

class SetWatchlistMembership(
    private val maximumItems: Int = 500,
) {
    init {
        require(maximumItems > 0) { "maximumItems must be positive" }
    }

    fun invoke(
        existing: List<WatchlistEntry>,
        titleId: TitleId,
        isMember: Boolean,
        changedAtEpochMillis: Long,
    ): List<WatchlistEntry> {
        val withoutTitle = existing.filterNot { it.titleId == titleId }
        return if (isMember) {
            listOf(WatchlistEntry(titleId, changedAtEpochMillis))
                .plus(withoutTitle)
                .take(maximumItems)
        } else {
            withoutTitle
        }
    }
}
