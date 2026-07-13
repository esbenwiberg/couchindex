package com.couchindex.app.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import com.couchindex.app.cache.CatalogueCacheStore
import com.couchindex.app.settings.KidsSettingsStore
import com.couchindex.app.state.KidsCatalogueOverrideStore
import com.couchindex.core.KidsEligibilityPolicy
import com.couchindex.core.ViewerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TvHomeChannelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TvContractCompat.ACTION_INITIALIZE_PROGRAMS) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                runCatching {
                    val snapshot = CatalogueCacheStore(context).load()
                    val settingsStore = KidsSettingsStore(context)
                    val titles = if (snapshot != null && settingsStore.initialProfile() == ViewerProfile.Kids) {
                        val settings = settingsStore.load()
                        KidsEligibilityPolicy().filter(
                            snapshot.titles,
                            settings.maximumAge,
                            KidsCatalogueOverrideStore(context).load(),
                        )
                    } else {
                        snapshot?.titles.orEmpty()
                    }
                    TvHomeChannelPublisher(context).refresh(snapshot, titles)
                }
                    .onFailure { error -> Log.w("CouchIndexTv", "TV Provider initialization failed", error) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
