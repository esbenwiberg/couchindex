package com.couchindex.app.state

import android.content.Context
import com.couchindex.core.FeedbackEntry
import com.couchindex.core.FeedbackValue
import com.couchindex.core.MediaKind
import com.couchindex.core.SetTitleFeedback
import com.couchindex.core.TitleId
import org.json.JSONArray
import org.json.JSONObject

class FeedbackStore(
    context: Context,
    private val setTitleFeedback: SetTitleFeedback = SetTitleFeedback(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<FeedbackEntry> =
        runCatching {
            val array = JSONArray(preferences.getString(KEY_ENTRIES, "[]"))
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toEntry() }
        }.getOrDefault(emptyList())

    fun set(titleId: TitleId, value: FeedbackValue?): List<FeedbackEntry> {
        val updated = setTitleFeedback.invoke(load(), titleId, value, clock())
        val array = JSONArray().apply {
            updated.forEach { entry -> put(entry.toJson()) }
        }
        preferences.edit().putString(KEY_ENTRIES, array.toString()).apply()
        return updated
    }

    private fun JSONObject.toEntry(): FeedbackEntry? {
        val tmdbId = optInt("tmdbId").takeIf { it > 0 } ?: return null
        val mediaKind = runCatching { MediaKind.valueOf(optString("mediaKind")) }.getOrNull() ?: return null
        val value = runCatching { FeedbackValue.valueOf(optString("value")) }.getOrNull() ?: return null
        val changedAt = optLong("changedAtEpochMillis").takeIf { it > 0 } ?: return null
        return FeedbackEntry(TitleId(tmdbId, mediaKind), value, changedAt)
    }

    private fun FeedbackEntry.toJson(): JSONObject =
        JSONObject()
            .put("tmdbId", titleId.tmdbId)
            .put("mediaKind", titleId.mediaKind.name)
            .put("value", value.name)
            .put("changedAtEpochMillis", changedAtEpochMillis)

    companion object {
        private const val PREFERENCES_NAME = "couchindex-feedback"
        private const val KEY_ENTRIES = "entries.v1"
    }
}
