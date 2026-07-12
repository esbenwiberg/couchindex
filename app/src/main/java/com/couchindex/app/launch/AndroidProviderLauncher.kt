package com.couchindex.app.launch

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.couchindex.core.LaunchTarget

enum class ProviderLaunchResult {
    ProviderAppLaunched,
    WatchOptionsLaunched,
    InstallPageLaunched,
    ActivityUnavailable,
}

class AndroidProviderLauncher(
    private val context: Context,
) : ProviderLaunchCapabilities {
    private val resolver = ProviderLaunchResolver(this)

    fun resolve(target: LaunchTarget?): ResolvedProviderLaunch = resolver.resolve(target)

    fun launch(target: LaunchTarget?): ProviderLaunchResult {
        val resolved = resolve(target)
        val intent = when (resolved.mode) {
            ProviderLaunchMode.ProviderApp -> providerIntent(resolved.packageName ?: return unavailable())
            ProviderLaunchMode.WatchOptions,
            ProviderLaunchMode.InstallProvider,
            -> Intent(Intent.ACTION_VIEW, Uri.parse(resolved.uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            ProviderLaunchMode.Unavailable -> null
        } ?: return unavailable()

        return runCatching {
            context.startActivity(intent)
            when (resolved.mode) {
                ProviderLaunchMode.ProviderApp -> ProviderLaunchResult.ProviderAppLaunched
                ProviderLaunchMode.WatchOptions -> ProviderLaunchResult.WatchOptionsLaunched
                ProviderLaunchMode.InstallProvider -> ProviderLaunchResult.InstallPageLaunched
                ProviderLaunchMode.Unavailable -> ProviderLaunchResult.ActivityUnavailable
            }
        }.getOrDefault(ProviderLaunchResult.ActivityUnavailable)
    }

    override fun canLaunchPackage(packageName: String): Boolean =
        providerIntent(packageName) != null

    override fun canOpenUri(uri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val handler = intent.resolveActivity(context.packageManager) ?: return false
        return handler.packageName !in NON_BROWSER_HANDLER_PACKAGES
    }

    private fun providerIntent(packageName: String): Intent? =
        context.packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: context.packageManager.getLaunchIntentForPackage(packageName)

    private fun unavailable(): ProviderLaunchResult = ProviderLaunchResult.ActivityUnavailable

    companion object {
        private val NON_BROWSER_HANDLER_PACKAGES = setOf("com.android.tv.frameworkpackagestubs")
    }
}
