package com.couchindex.app.tv

import com.couchindex.core.MediaKind
import com.couchindex.core.Title
import com.couchindex.core.TitleId
import java.util.Locale

data class TvHomeProgramme(
    val titleId: TitleId,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val releaseDate: String?,
) {
    val contentId: String = "${titleId.mediaKind.name.lowercase(Locale.ROOT)}-${titleId.tmdbId}"
}

fun buildTvHomeProgrammes(
    titles: List<Title>,
    maximumCount: Int = 12,
): List<TvHomeProgramme> =
    titles
        .distinctBy(Title::id)
        .sortedWith(
            compareByDescending<Title> { it.isNewOnService }
                .thenByDescending { it.bestRating() }
                .thenByDescending { it.ratingVoteSupport() }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
        )
        .take(maximumCount.coerceAtLeast(0))
        .map { title ->
            TvHomeProgramme(
                titleId = title.id,
                title = title.name,
                description = title.synopsis.ifBlank {
                    when (title.mediaKind) {
                        MediaKind.Movie -> "Movie available with your subscriptions"
                        MediaKind.Series -> "Series available with your subscriptions"
                    }
                },
                posterUrl = title.posterUrl,
                releaseDate = title.releaseDate,
            )
        }

private fun Title.bestRating(): Double =
    ratings.maxOfOrNull { rating -> rating.value / rating.scale } ?: Double.NEGATIVE_INFINITY

private fun Title.ratingVoteSupport(): Int = ratings.maxOfOrNull { it.voteCount ?: 0 } ?: 0
