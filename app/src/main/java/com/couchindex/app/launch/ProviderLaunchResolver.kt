package com.couchindex.app.launch

import com.couchindex.core.LaunchTarget

enum class ProviderLaunchMode {
    ProviderApp,
    WatchOptions,
    InstallProvider,
    Unavailable,
}

data class ResolvedProviderLaunch(
    val mode: ProviderLaunchMode,
    val label: String,
    val uri: String? = null,
    val packageName: String? = null,
)

interface ProviderLaunchCapabilities {
    fun canLaunchPackage(packageName: String): Boolean
    fun canOpenUri(uri: String): Boolean
}

class ProviderLaunchResolver(
    private val capabilities: ProviderLaunchCapabilities,
) {
    fun resolve(target: LaunchTarget?): ResolvedProviderLaunch {
        if (target == null) return unavailable()

        val packageName = target.androidPackageName?.takeIf { it.isNotBlank() }
        if (packageName != null && capabilities.canLaunchPackage(packageName)) {
            return ResolvedProviderLaunch(
                mode = ProviderLaunchMode.ProviderApp,
                label = "Open ${target.label}",
                packageName = packageName,
            )
        }

        val watchUri = target.uri?.takeIf { it.isNotBlank() }
        if (watchUri != null && capabilities.canOpenUri(watchUri)) {
            return ResolvedProviderLaunch(
                mode = ProviderLaunchMode.WatchOptions,
                label = "View watch options",
                uri = watchUri,
            )
        }

        val installUri = packageName?.let { "market://details?id=$it" }
        if (installUri != null && capabilities.canOpenUri(installUri)) {
            return ResolvedProviderLaunch(
                mode = ProviderLaunchMode.InstallProvider,
                label = "Install ${target.label}",
                uri = installUri,
            )
        }

        return unavailable()
    }

    private fun unavailable(): ResolvedProviderLaunch =
        ResolvedProviderLaunch(
            mode = ProviderLaunchMode.Unavailable,
            label = "No launch option",
        )
}
