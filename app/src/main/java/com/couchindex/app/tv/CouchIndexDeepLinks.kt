package com.couchindex.app.tv

import com.couchindex.core.MediaKind
import com.couchindex.core.TitleId
import java.net.URI
import java.util.Locale

object CouchIndexDeepLinks {
    const val HOME_URI = "couchindex://home"

    fun titleUri(titleId: TitleId): String {
        val kind = when (titleId.mediaKind) {
            MediaKind.Movie -> "movie"
            MediaKind.Series -> "series"
        }
        return "couchindex://title/$kind/${titleId.tmdbId}"
    }

    fun parseTitleId(rawUri: String?): TitleId? = runCatching {
        val uri = rawUri?.let(::URI) ?: return null
        if (!uri.scheme.equals("couchindex", ignoreCase = true) || !uri.host.equals("title", ignoreCase = true)) {
            return null
        }
        val segments = uri.path.trim('/').split('/').filter(String::isNotBlank)
        if (segments.size != 2) return null
        val mediaKind = when (segments[0].lowercase(Locale.ROOT)) {
            "movie" -> MediaKind.Movie
            "series" -> MediaKind.Series
            else -> return null
        }
        val tmdbId = segments[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        TitleId(tmdbId, mediaKind)
    }.getOrNull()
}
