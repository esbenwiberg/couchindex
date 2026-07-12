package com.couchindex.app.launch

import com.couchindex.core.LaunchTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderLaunchResolverTest {
    private val target = LaunchTarget(
        providerId = "netflix",
        label = "Netflix",
        uri = "https://www.themoviedb.org/movie/42/watch?locale=DK",
        androidPackageName = "com.netflix.ninja",
    )

    @Test
    fun `prefers an installed provider app`() {
        val resolved = resolver(packages = setOf("com.netflix.ninja"), schemes = setOf("https", "market"))
            .resolve(target)

        assertEquals(ProviderLaunchMode.ProviderApp, resolved.mode)
        assertEquals("Open Netflix", resolved.label)
    }

    @Test
    fun `uses title watch options when the provider app is absent`() {
        val resolved = resolver(schemes = setOf("https", "market")).resolve(target)

        assertEquals(ProviderLaunchMode.WatchOptions, resolved.mode)
        assertEquals("View watch options", resolved.label)
    }

    @Test
    fun `uses the install page when no browser is available`() {
        val resolved = resolver(schemes = setOf("market")).resolve(target)

        assertEquals(ProviderLaunchMode.InstallProvider, resolved.mode)
        assertEquals("market://details?id=com.netflix.ninja", resolved.uri)
        assertEquals("Install Netflix", resolved.label)
    }

    @Test
    fun `reports unavailable when no route can handle the target`() {
        val resolved = resolver().resolve(target.copy(androidPackageName = null))

        assertEquals(ProviderLaunchMode.Unavailable, resolved.mode)
        assertEquals("No launch option", resolved.label)
    }

    private fun resolver(
        packages: Set<String> = emptySet(),
        schemes: Set<String> = emptySet(),
    ): ProviderLaunchResolver = ProviderLaunchResolver(
        capabilities = object : ProviderLaunchCapabilities {
            override fun canLaunchPackage(packageName: String): Boolean = packageName in packages

            override fun canOpenUri(uri: String): Boolean = schemes.any { scheme -> uri.startsWith("$scheme:") }
        },
    )
}
