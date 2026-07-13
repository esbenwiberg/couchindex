package com.couchindex.app.state

import android.content.Context
import com.couchindex.core.KidsCatalogueOverride
import com.couchindex.core.KidsOverrideDecision
import com.couchindex.core.MediaKind
import com.couchindex.core.TitleId
import org.json.JSONArray
import org.json.JSONObject

class KidsCatalogueOverrideStore(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val codec = KidsCatalogueOverrideCodec()

    fun load(): List<KidsCatalogueOverride> = codec.decode(preferences.getString(KEY_OVERRIDES, "[]"))

    fun set(
        titleId: TitleId,
        titleName: String,
        decision: KidsOverrideDecision?,
        allowsOverAge: Boolean = false,
    ): List<KidsCatalogueOverride> {
        val updated = load()
            .filterNot { it.titleId == titleId }
            .plus(
                decision?.let {
                    KidsCatalogueOverride(
                        titleId = titleId,
                        decision = it,
                        allowsOverAge = it == KidsOverrideDecision.Allowed && allowsOverAge,
                        changedAtEpochMillis = clock(),
                        titleName = titleName,
                    )
                },
            )
            .filterNotNull()
            .sortedByDescending { it.changedAtEpochMillis }
        preferences.edit().putString(KEY_OVERRIDES, codec.encode(updated)).apply()
        return updated
    }

    companion object {
        private const val PREFERENCES_NAME = "couchindex-kids-catalogue"
        private const val KEY_OVERRIDES = "overrides.v1"
    }
}

class KidsCatalogueOverrideCodec {
    fun encode(overrides: List<KidsCatalogueOverride>): String = JSONArray().apply {
        overrides.forEach { override ->
            put(
                JSONObject()
                    .put("tmdbId", override.titleId.tmdbId)
                    .put("mediaKind", override.titleId.mediaKind.name)
                    .put("decision", override.decision.name)
                    .put("allowsOverAge", override.allowsOverAge)
                    .put("changedAtEpochMillis", override.changedAtEpochMillis)
                    .put("titleName", override.titleName),
            )
        }
    }.toString()

    fun decode(raw: String?): List<KidsCatalogueOverride> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length())
            .mapNotNull { index -> array.optJSONObject(index)?.toOverride() }
            .groupBy { it.titleId }
            .mapNotNull { (_, entries) ->
                entries.firstOrNull { it.decision == KidsOverrideDecision.Blocked }
                    ?: entries.maxByOrNull { it.changedAtEpochMillis }
            }
            .sortedByDescending { it.changedAtEpochMillis }
    }.getOrDefault(emptyList())

    private fun JSONObject.toOverride(): KidsCatalogueOverride? {
        val tmdbId = optInt("tmdbId").takeIf { it > 0 } ?: return null
        val mediaKind = runCatching { MediaKind.valueOf(optString("mediaKind")) }.getOrNull() ?: return null
        val decision = runCatching { KidsOverrideDecision.valueOf(optString("decision")) }.getOrNull() ?: return null
        val changedAt = optLong("changedAtEpochMillis").takeIf { it > 0 } ?: return null
        return KidsCatalogueOverride(
            titleId = TitleId(tmdbId, mediaKind),
            decision = decision,
            allowsOverAge = decision == KidsOverrideDecision.Allowed && optBoolean("allowsOverAge"),
            changedAtEpochMillis = changedAt,
            titleName = optString("titleName"),
        )
    }
}
