package com.couchindex.app.config

import com.couchindex.app.BuildConfig

data class AppConfig(
    val tmdbReadAccessToken: String,
) {
    val hasTmdbReadAccessToken: Boolean
        get() = tmdbReadAccessToken.isNotBlank()
}

object AppConfigLoader {
    fun load(): AppConfig =
        AppConfig(
            tmdbReadAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN.trim(),
        )
}
