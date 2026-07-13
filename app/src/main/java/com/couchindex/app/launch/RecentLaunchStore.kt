package com.couchindex.app.launch

import android.content.Context
import com.couchindex.core.MediaKind
import com.couchindex.core.RecentLaunch
import com.couchindex.core.RecordRecentLaunch
import com.couchindex.core.RemoveRecentLaunch
import com.couchindex.core.TitleId
import com.couchindex.core.ViewerProfile
import com.couchindex.app.state.profiledPreferencesName
import org.json.JSONArray
import org.json.JSONObject

class RecentLaunchStore(
    context: Context,
    profile: ViewerProfile = ViewerProfile.Adult,
    private val recordRecentLaunch: RecordRecentLaunch = RecordRecentLaunch(),
    private val removeRecentLaunch: RemoveRecentLaunch = RemoveRecentLaunch(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.getSharedPreferences(
        profiledPreferencesName(PREFERENCES_NAME, profile),
        Context.MODE_PRIVATE,
    )

    fun load(): List<RecentLaunch> =
        runCatching {
            val array = JSONArray(preferences.getString(KEY_HISTORY, "[]"))
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toRecentLaunch() }
        }.getOrDefault(emptyList())

    fun record(titleId: TitleId): List<RecentLaunch> {
        val updated = recordRecentLaunch.invoke(load(), titleId, clock())
        save(updated)
        return updated
    }

    fun remove(titleId: TitleId): List<RecentLaunch> {
        val updated = removeRecentLaunch.invoke(load(), titleId)
        save(updated)
        return updated
    }

    private fun save(updated: List<RecentLaunch>) {
        val array = JSONArray().apply {
            updated.forEach { launch -> put(launch.toJson()) }
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun JSONObject.toRecentLaunch(): RecentLaunch? {
        val tmdbId = optInt("tmdbId").takeIf { it > 0 } ?: return null
        val mediaKind = runCatching { MediaKind.valueOf(optString("mediaKind")) }.getOrNull() ?: return null
        return RecentLaunch(
            titleId = TitleId(tmdbId, mediaKind),
            launchedAtLabel = "Recently",
            launchedAtEpochMillis = optLong("launchedAtEpochMillis").takeIf { it > 0 },
        )
    }

    private fun RecentLaunch.toJson(): JSONObject =
        JSONObject()
            .put("tmdbId", titleId.tmdbId)
            .put("mediaKind", titleId.mediaKind.name)
            .put("launchedAtEpochMillis", launchedAtEpochMillis)

    companion object {
        private const val PREFERENCES_NAME = "couchindex-recent-launches"
        private const val KEY_HISTORY = "history.v1"
    }
}
