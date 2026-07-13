package com.couchindex.app.cache

import com.couchindex.core.Provider
import com.couchindex.core.Title

data class CatalogueSnapshot(
    val region: String,
    val savedAtEpochMillis: Long,
    val providers: List<Provider>,
    val titles: List<Title>,
)

enum class CacheFreshness {
    Cached,
    Stale,
}

fun CatalogueSnapshot.freshness(
    nowEpochMillis: Long,
    staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
): CacheFreshness =
    if (nowEpochMillis - savedAtEpochMillis >= staleAfterMillis) CacheFreshness.Stale else CacheFreshness.Cached

fun CatalogueSnapshot.ageLabel(nowEpochMillis: Long): String {
    val ageMillis = (nowEpochMillis - savedAtEpochMillis).coerceAtLeast(0)
    val minutes = ageMillis / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1_440 -> "${minutes / 60} hr ago"
        else -> "${minutes / 1_440} days ago"
    }
}

private const val DEFAULT_STALE_AFTER_MILLIS = 24L * 60L * 60L * 1_000L
