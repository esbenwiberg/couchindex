package com.couchindex.app.state

import android.content.Context
import com.couchindex.core.MediaKind
import com.couchindex.core.SetWatchedStatus
import com.couchindex.core.TitleId
import com.couchindex.core.WatchedEntry
import com.couchindex.core.ViewerProfile
import org.json.JSONArray
import org.json.JSONObject

class WatchedStore(
    context: Context,
    profile: ViewerProfile = ViewerProfile.Adult,
    private val setWatchedStatus: SetWatchedStatus = SetWatchedStatus(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.getSharedPreferences(
        profiledPreferencesName(PREFERENCES_NAME, profile),
        Context.MODE_PRIVATE,
    )

    fun load(): List<WatchedEntry> =
        runCatching {
            val array = JSONArray(preferences.getString(KEY_ENTRIES, "[]"))
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toEntry() }
        }.getOrDefault(emptyList())

    fun setWatched(titleId: TitleId, isWatched: Boolean): List<WatchedEntry> {
        val updated = setWatchedStatus.invoke(load(), titleId, isWatched, clock())
        val array = JSONArray().apply {
            updated.forEach { entry -> put(entry.toJson()) }
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply()
        return updated
    }

    private fun JSONObject.toEntry(): WatchedEntry? {
        val tmdbId = optInt("tmdbId").takeIf { it > 0 } ?: return null
        val mediaKind = runCatching { MediaKind.valueOf(optString("mediaKind")) }.getOrNull() ?: return null
        val watchedAt = optLong("watchedAtEpochMillis").takeIf { it > 0 } ?: return null
        return WatchedEntry(TitleId(tmdbId, mediaKind), watchedAt)
    }

    private fun WatchedEntry.toJson(): JSONObject =
        JSONObject()
            .put("tmdbId", titleId.tmdbId)
            .put("mediaKind", titleId.mediaKind.name)
            .put("watchedAtEpochMillis", watchedAtEpochMillis)

    companion object {
        private const val PREFERENCES_NAME = "couchindex-watched"
        private const val KEY_ENTRIES = "entries.v1"
    }
}
