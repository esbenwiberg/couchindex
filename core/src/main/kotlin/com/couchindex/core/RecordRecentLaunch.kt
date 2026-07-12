package com.couchindex.core

class RecordRecentLaunch(
    private val maximumItems: Int = 50,
) {
    init {
        require(maximumItems > 0) { "maximumItems must be positive" }
    }

    fun invoke(
        existing: List<RecentLaunch>,
        titleId: TitleId,
        launchedAtEpochMillis: Long,
    ): List<RecentLaunch> =
        listOf(
            RecentLaunch(
                titleId = titleId,
                launchedAtLabel = "Recently",
                launchedAtEpochMillis = launchedAtEpochMillis,
            ),
        ).plus(existing.filterNot { it.titleId == titleId })
            .take(maximumItems)
}
