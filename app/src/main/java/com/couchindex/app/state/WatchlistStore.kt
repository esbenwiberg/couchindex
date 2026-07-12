package com.couchindex.app.state

import android.content.Context
import com.couchindex.core.MediaKind
import com.couchindex.core.SetWatchlistMembership
import com.couchindex.core.TitleId
import com.couchindex.core.WatchlistEntry
import org.json.JSONArray
import org.json.JSONObject

class WatchlistStore(
    context: Context,
    private val setMembership: SetWatchlistMembership = SetWatchlistMembership(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<WatchlistEntry> =
        runCatching {
            val array = JSONArray(preferences.getString(KEY_ENTRIES, "[]"))
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toEntry() }
        }.getOrDefault(emptyList())

    fun setMembership(titleId: TitleId, isMember: Boolean): List<WatchlistEntry> {
        val updated = setMembership.invoke(load(), titleId, isMember, clock())
        val array = JSONArray().apply {
            updated.forEach { entry -> put(entry.toJson()) }
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply()
        return updated
    }

    private fun JSONObject.toEntry(): WatchlistEntry? {
        val tmdbId = optInt("tmdbId").takeIf { it > 0 } ?: return null
        val mediaKind = runCatching { MediaKind.valueOf(optString("mediaKind")) }.getOrNull() ?: return null
        val addedAt = optLong("addedAtEpochMillis").takeIf { it > 0 } ?: return null
        return WatchlistEntry(TitleId(tmdbId, mediaKind), addedAt)
    }

    private fun WatchlistEntry.toJson(): JSONObject =
        JSONObject()
            .put("tmdbId", titleId.tmdbId)
            .put("mediaKind", titleId.mediaKind.name)
            .put("addedAtEpochMillis", addedAtEpochMillis)

    companion object {
        private const val PREFERENCES_NAME = "couchindex-watchlist"
        private const val KEY_ENTRIES = "entries.v1"
    }
}
