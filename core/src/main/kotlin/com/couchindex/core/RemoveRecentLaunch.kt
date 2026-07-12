package com.couchindex.core

class RemoveRecentLaunch {
    fun invoke(
        existing: List<RecentLaunch>,
        titleId: TitleId,
    ): List<RecentLaunch> = existing.filterNot { it.titleId == titleId }
}
