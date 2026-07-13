package com.couchindex.core

enum class BrowseMediaFilter {
    All,
    Movies,
    Series,
}

enum class BrowseSort {
    Title,
    Newest,
    ImdbRating,
    TmdbRating,
}

data class BrowseCatalogueQuery(
    val mediaFilter: BrowseMediaFilter = BrowseMediaFilter.All,
    val genreId: Int? = null,
    val sort: BrowseSort = BrowseSort.Title,
)

class BrowseCatalogue {
    fun invoke(titles: List<Title>, query: BrowseCatalogueQuery): List<Title> = titles
        .asSequence()
        .filter { title ->
            when (query.mediaFilter) {
                BrowseMediaFilter.All -> true
                BrowseMediaFilter.Movies -> title.mediaKind == MediaKind.Movie
                BrowseMediaFilter.Series -> title.mediaKind == MediaKind.Series
            }
        }
        .filter { title -> query.genreId == null || query.genreId in title.genreIds }
        .sortedWith(comparator(query.sort))
        .toList()

    private fun comparator(sort: BrowseSort): Comparator<Title> {
        val titleTieBreaker = compareBy<Title> { it.name.lowercase() }
            .thenBy { it.mediaKind.name }
            .thenBy { it.id.tmdbId }
        return when (sort) {
            BrowseSort.Title -> titleTieBreaker
            BrowseSort.Newest -> compareByDescending<Title> { it.releaseDate != null }
                .thenByDescending { it.releaseDate.orEmpty() }
                .then(titleTieBreaker)
            BrowseSort.ImdbRating -> ratingComparator("IMDb").then(titleTieBreaker)
            BrowseSort.TmdbRating -> ratingComparator("TMDb").then(titleTieBreaker)
        }
    }

    private fun ratingComparator(source: String): Comparator<Title> =
        compareByDescending<Title> { it.rating(source) != null }
            .thenByDescending { it.rating(source)?.let { rating -> rating.value / rating.scale } ?: 0.0 }
            .thenByDescending { it.rating(source)?.voteCount ?: 0 }

    private fun Title.rating(source: String): Rating? =
        ratings.firstOrNull { it.source.equals(source, ignoreCase = true) }
}
