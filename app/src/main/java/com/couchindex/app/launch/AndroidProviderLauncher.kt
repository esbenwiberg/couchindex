package com.couchindex.app.launch

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.couchindex.core.LaunchTarget

enum class ProviderLaunchResult {
    Launched,
    MissingTarget,
    MissingUri,
    ActivityUnavailable,
}

class AndroidProviderLauncher(
    private val context: Context,
) {
    fun launch(target: LaunchTarget?): ProviderLaunchResult {
        if (target == null) return ProviderLaunchResult.MissingTarget

        val uri = target.uri?.takeIf { it.isNotBlank() } ?: return ProviderLaunchResult.MissingUri
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            return ProviderLaunchResult.ActivityUnavailable
        }

        context.startActivity(intent)
        return ProviderLaunchResult.Launched
    }
}
