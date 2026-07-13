package com.couchindex.app.tv

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.couchindex.app.R
import com.couchindex.app.cache.CatalogueSnapshot
import com.couchindex.core.MediaKind
import com.couchindex.core.Title

class TvHomeChannelPublisher(context: Context) {
    private val context = context.applicationContext
    private val helper = PreviewChannelHelper(this.context)
    private val preferences = this.context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun refresh(
        snapshot: CatalogueSnapshot?,
        titles: List<Title> = snapshot?.titles.orEmpty(),
    ) {
        val programmes = buildTvHomeProgrammes(titles)
        val existingChannel = findExistingChannel()
        if (programmes.isEmpty()) {
            existingChannel?.let { deleteProgrammes(it.id) }
            recordPublishedProgrammes(emptyList())
            return
        }

        val channelId = existingChannel?.id?.also { id ->
            helper.updatePreviewChannel(id, buildChannel())
        } ?: helper.publishDefaultChannel(buildChannel()).also { id ->
            check(id > 0) { "TV Provider did not publish the CouchIndex channel" }
            preferences.edit().putLong(CHANNEL_ID_KEY, id).apply()
        }

        val previousProgrammeIds = programmeIds(channelId)
        val publishedIds = programmes.mapIndexedNotNull { index, programme ->
            helper.publishPreviewProgram(programme.toPreviewProgram(channelId, programmes.size - index))
                .takeIf { it > 0 }
        }
        val activeProgrammeIds = when {
            publishedIds.size == programmes.size -> {
                previousProgrammeIds.forEach(helper::deletePreviewProgram)
                publishedIds
            }

            previousProgrammeIds.isNotEmpty() -> {
                publishedIds.forEach(helper::deletePreviewProgram)
                previousProgrammeIds
            }

            else -> publishedIds
        }
        recordPublishedProgrammes(activeProgrammeIds)
        Log.i(TAG, "Published TV channel $channelId with ${activeProgrammeIds.size} programmes")
    }

    private fun findExistingChannel(): PreviewChannel? {
        val savedId = preferences.getLong(CHANNEL_ID_KEY, -1L)
        val saved = savedId.takeIf { it > 0 }?.let(helper::getPreviewChannel)
        if (saved != null) return saved

        return helper.allChannels.firstOrNull { it.internalProviderId == CHANNEL_INTERNAL_ID }
            ?.also { preferences.edit().putLong(CHANNEL_ID_KEY, it.id).apply() }
    }

    private fun buildChannel(): PreviewChannel =
        PreviewChannel.Builder()
            .setDisplayName("CouchIndex")
            .setDescription("Highly rated picks from your streaming subscriptions")
            .setInternalProviderId(CHANNEL_INTERNAL_ID)
            .setAppLinkIntentUri(Uri.parse(CouchIndexDeepLinks.HOME_URI))
            .setLogo(channelLogo())
            .build()

    private fun channelLogo(): Bitmap {
        val drawable = checkNotNull(context.getDrawable(R.drawable.couchindex_mark))
        return Bitmap.createBitmap(LOGO_SIZE, LOGO_SIZE, Bitmap.Config.ARGB_8888).also { bitmap ->
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(Canvas(bitmap))
        }
    }

    // TV Provider 1.1.0 exposes required program fields through a restricted base builder.
    @SuppressLint("RestrictedApi")
    private fun TvHomeProgramme.toPreviewProgram(channelId: Long, weight: Int): PreviewProgram {
        val posterUri = posterUrl?.takeIf(String::isNotBlank)?.let(Uri::parse)
        val artUri = posterUri ?: localBannerUri()
        val type = when (titleId.mediaKind) {
            MediaKind.Movie -> TvContractCompat.PreviewPrograms.TYPE_MOVIE
            MediaKind.Series -> TvContractCompat.PreviewPrograms.TYPE_TV_SERIES
        }
        return PreviewProgram.Builder()
            .setChannelId(channelId)
            .setWeight(weight)
            .setTitle(title)
            .setDescription(description)
            .setType(type)
            .setPosterArtUri(artUri)
            .setPosterArtAspectRatio(
                if (posterUri != null) {
                    TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3
                } else {
                    TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9
                },
            )
            .setIntentUri(Uri.parse(CouchIndexDeepLinks.titleUri(titleId)))
            .setInternalProviderId(contentId)
            .setContentId(contentId)
            .setAvailability(TvContractCompat.PreviewPrograms.AVAILABILITY_FREE_WITH_SUBSCRIPTION)
            .setReleaseDate(releaseDate)
            .setSearchable(true)
            .setBrowsable(true)
            .build()
    }

    private fun localBannerUri(): Uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath("drawable")
            .appendPath("couchindex_banner")
            .build()

    private fun deleteProgrammes(channelId: Long) {
        programmeIds(channelId).forEach(helper::deletePreviewProgram)
    }

    private fun programmeIds(channelId: Long): List<Long> {
        val uri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId)
        return context.contentResolver.query(
            uri,
            arrayOf(BaseColumns._ID),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            buildList {
                while (cursor.moveToNext()) add(cursor.getLong(idIndex))
            }
        }.orEmpty()
    }

    private fun recordPublishedProgrammes(programmeIds: List<Long>) {
        preferences.edit()
            .putInt(PUBLISHED_PROGRAMME_COUNT_KEY, programmeIds.size)
            .putString(PUBLISHED_PROGRAMME_IDS_KEY, programmeIds.joinToString(","))
            .apply()
    }

    private companion object {
        const val CHANNEL_INTERNAL_ID = "couchindex-recommendations"
        const val CHANNEL_ID_KEY = "channel_id"
        const val PUBLISHED_PROGRAMME_COUNT_KEY = "published_programme_count"
        const val PUBLISHED_PROGRAMME_IDS_KEY = "published_programme_ids"
        const val PREFERENCES_NAME = "tv-home-channel"
        const val LOGO_SIZE = 108
        const val TAG = "CouchIndexTv"
    }
}
