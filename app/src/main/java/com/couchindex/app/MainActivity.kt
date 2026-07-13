package com.couchindex.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.couchindex.app.config.AppConfig
import com.couchindex.app.config.AppConfigLoader
import com.couchindex.app.config.PrivacyPolicy
import com.couchindex.app.cache.CatalogueCacheStore
import com.couchindex.app.cache.CatalogueSnapshot
import com.couchindex.app.cache.cacheStatus
import com.couchindex.app.launch.AndroidProviderLauncher
import com.couchindex.app.launch.ProviderLaunchResult
import com.couchindex.app.launch.RecentLaunchStore
import com.couchindex.app.launch.ResolvedProviderLaunch
import com.couchindex.app.ratings.ImdbDatasetRatingAdapter
import com.couchindex.app.settings.SubscriptionStore
import com.couchindex.app.settings.KidsSettings
import com.couchindex.app.settings.KidsSettingsStore
import com.couchindex.app.state.FeedbackStore
import com.couchindex.app.state.KidsCatalogueOverrideStore
import com.couchindex.app.state.WatchedStore
import com.couchindex.app.state.WatchlistStore
import com.couchindex.app.tmdb.TmdbCatalogueRepository
import com.couchindex.app.tmdb.TmdbDiscoverClient
import com.couchindex.app.tmdb.TmdbGenreDirectory
import com.couchindex.app.tmdb.TmdbProviderDirectory
import com.couchindex.app.tv.CouchIndexDeepLinks
import com.couchindex.app.tv.TvHomeChannelPublisher
import com.couchindex.core.BrowseCatalogue
import com.couchindex.core.BrowseCatalogueQuery
import com.couchindex.core.BrowseMediaFilter
import com.couchindex.core.BrowseRow
import com.couchindex.core.BrowseSort
import com.couchindex.core.BuildHomeRows
import com.couchindex.core.FeedbackValue
import com.couchindex.core.Genre
import com.couchindex.core.LaunchTarget
import com.couchindex.core.KidsEligibilityPolicy
import com.couchindex.core.KidsCatalogueOverride
import com.couchindex.core.KidsOverrideDecision
import com.couchindex.core.MediaKind
import com.couchindex.core.Provider
import com.couchindex.core.Rating
import com.couchindex.core.SampleCatalogue
import com.couchindex.core.SearchCatalogue
import com.couchindex.core.Subscription
import com.couchindex.core.Title
import com.couchindex.core.TitleId
import com.couchindex.core.ViewerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val requestedTitleId = mutableStateOf<TitleId?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTitleId.value = CouchIndexDeepLinks.parseTitleId(intent?.dataString)
        setContent {
            CouchIndexApp(
                requestedTitleId = requestedTitleId.value,
                onRequestedTitleHandled = { requestedTitleId.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTitleId.value = CouchIndexDeepLinks.parseTitleId(intent.dataString)
    }
}

private enum class Destination(val label: String) {
    Home("Home"),
    Browse("Browse"),
    Search("Search"),
    Settings("Settings"),
    ParentalControls("Parental controls"),
}

private sealed interface ParentAction {
    data object EnterKids : ParentAction
    data object ExitKids : ParentAction
    data object ToggleStartInKids : ParentAction
    data class SetMaximumAge(val age: Int) : ParentAction
    data object OpenParentalControls : ParentAction
    data class SetKidsOverride(
        val titleId: TitleId,
        val titleName: String,
        val decision: KidsOverrideDecision?,
        val allowsOverAge: Boolean = false,
    ) : ParentAction
}

private sealed interface PinFlow {
    val action: ParentAction
    val error: String?

    data class Setup(
        override val action: ParentAction,
        val firstPin: String? = null,
        override val error: String? = null,
    ) : PinFlow

    data class Verify(
        override val action: ParentAction,
        override val error: String? = null,
    ) : PinFlow
}

private data class CatalogueStatus(
    val detail: String,
    val badge: String,
    val highlighted: Boolean,
) {
    val railLabel: String
        get() = when (badge) {
            "Live" -> "DK catalogue live"
            "Loading" -> "DK catalogue loading"
            "Cached" -> "DK catalogue cached"
            "Stale" -> "DK catalogue stale"
            "Offline" -> "DK catalogue offline"
            "Ready" -> "DK catalogue ready"
            else -> "DK catalogue sample"
        }
}

@Composable
private fun CouchIndexApp(
    requestedTitleId: TitleId?,
    onRequestedTitleHandled: () -> Unit,
) {
    val rowBuilder = remember { BuildHomeRows() }
    val kidsEligibilityPolicy = remember { KidsEligibilityPolicy() }
    val context = LocalContext.current
    val appConfig = remember { AppConfigLoader.load() }
    val kidsSettingsStore = remember(context) { KidsSettingsStore(context) }
    val kidsOverrideStore = remember(context) { KidsCatalogueOverrideStore(context) }
    var kidsSettings by remember { mutableStateOf(kidsSettingsStore.load()) }
    var viewerProfile by remember { mutableStateOf(kidsSettingsStore.initialProfile()) }
    var pinFlow by remember { mutableStateOf<PinFlow?>(null) }
    val catalogueCacheStore = remember(context) { CatalogueCacheStore(context) }
    val tvHomeChannelPublisher = remember(context) { TvHomeChannelPublisher(context) }
    var cachedSnapshot by remember(appConfig.hasTmdbReadAccessToken) {
        mutableStateOf(
            catalogueCacheStore.load()
                ?.takeIf { appConfig.hasTmdbReadAccessToken && it.region == DEFAULT_REGION },
        )
    }
    val initialProviders = cachedSnapshot?.providers ?: SampleCatalogue.providers
    val initialGenres = cachedSnapshot?.genres?.takeIf { it.isNotEmpty() } ?: SampleCatalogue.genres
    val providerLauncher = remember(context) { AndroidProviderLauncher(context) }
    val imdbRatingAdapter = remember(context) { ImdbDatasetRatingAdapter(context) }
    val recentLaunchStore = remember(context, viewerProfile) { RecentLaunchStore(context, viewerProfile) }
    val kidsRecentLaunchStore = remember(context) { RecentLaunchStore(context, ViewerProfile.Kids) }
    val feedbackStore = remember(context, viewerProfile) { FeedbackStore(context, viewerProfile) }
    val subscriptionStore = remember(context) { SubscriptionStore(context) }
    val watchedStore = remember(context, viewerProfile) { WatchedStore(context, viewerProfile) }
    val watchlistStore = remember(context, viewerProfile) { WatchlistStore(context, viewerProfile) }
    val tmdbClient = remember(appConfig.tmdbReadAccessToken) { TmdbDiscoverClient(appConfig.tmdbReadAccessToken) }
    val providerDirectory = remember(tmdbClient) {
        TmdbProviderDirectory(source = tmdbClient, fallbackProviders = SampleCatalogue.providers)
    }
    val genreDirectory = remember(tmdbClient) { TmdbGenreDirectory(tmdbClient) }
    var providers by remember { mutableStateOf(initialProviders) }
    var genres by remember { mutableStateOf(initialGenres) }
    var providerDirectoryReady by remember {
        mutableStateOf(!appConfig.hasTmdbReadAccessToken || cachedSnapshot != null)
    }
    var genreDirectoryReady by remember {
        mutableStateOf(!appConfig.hasTmdbReadAccessToken || cachedSnapshot?.genres?.isNotEmpty() == true)
    }
    val subscriptions = remember {
        val sampleDefaults = SampleCatalogue.subscriptions.associate { it.providerId to it.enabled }
        val defaults = initialProviders.map { provider ->
            Subscription(provider.id, sampleDefaults[provider.id] ?: false)
        }
        mutableStateListOf(*subscriptionStore.load(defaults).toTypedArray())
    }
    val recentLaunches = remember(viewerProfile) {
        val initial = if (appConfig.hasTmdbReadAccessToken) {
            recentLaunchStore.load()
        } else if (viewerProfile == ViewerProfile.Adult) {
            SampleCatalogue.recentLaunches
        } else {
            emptyList()
        }
        mutableStateListOf(*initial.toTypedArray())
    }
    val watchlistEntries = remember(viewerProfile) {
        mutableStateListOf(*watchlistStore.load().toTypedArray())
    }
    val watchedEntries = remember(viewerProfile) {
        mutableStateListOf(*watchedStore.load().toTypedArray())
    }
    val feedbackEntries = remember(viewerProfile) {
        mutableStateListOf(*feedbackStore.load().toTypedArray())
    }
    val kidsOverrides = remember {
        mutableStateListOf(*kidsOverrideStore.load().toTypedArray())
    }
    var catalogue by remember { mutableStateOf(cachedSnapshot?.titles ?: SampleCatalogue.titles) }
    var catalogueStatus by remember {
        mutableStateOf(
            cachedSnapshot?.toCatalogueStatus(System.currentTimeMillis())
                ?: CatalogueStatus(
                    detail = "Using sample catalogue",
                    badge = "Local",
                    highlighted = false,
                ),
        )
    }
    val visibleCatalogue by remember(viewerProfile, kidsSettings.maximumAge) {
        derivedStateOf {
            if (viewerProfile == ViewerProfile.Kids) {
                kidsEligibilityPolicy.filter(catalogue, kidsSettings.maximumAge, kidsOverrides)
            } else {
                catalogue
            }
        }
    }
    val rows by remember(viewerProfile, kidsSettings.maximumAge) {
        derivedStateOf {
            rowBuilder.invoke(
                catalogue = visibleCatalogue,
                subscriptions = subscriptions,
                recentLaunches = recentLaunches,
                watchlistEntries = watchlistEntries,
                feedbackEntries = feedbackEntries,
            )
        }
    }
    val watchlistedTitleIds = watchlistEntries.map { it.titleId }.toSet()
    val watchedTitleIds = watchedEntries.map { it.titleId }.toSet()
    val recentTitleIds = recentLaunches.map { it.titleId }.toSet()
    val feedbackByTitleId = feedbackEntries.associate { it.titleId to it.value }
    val enabledProviderIds = subscriptions.filter { it.enabled }.map { it.providerId }.toSet()
    val providerSignature = providers.map { it.id to it.tmdbProviderId }
    val genreSignature = genres.map { genre -> genre.id to genre.name }

    var destination by remember { mutableStateOf(Destination.Home) }
    var selectedTitle by remember { mutableStateOf<Title?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var parentalSearchQuery by remember { mutableStateOf("") }
    var browseMediaFilter by remember { mutableStateOf(BrowseMediaFilter.All) }
    var browseGenreId by remember { mutableStateOf<Int?>(null) }
    var browseSort by remember { mutableStateOf(BrowseSort.Title) }

    LaunchedEffect(
        cachedSnapshot?.savedAtEpochMillis,
        viewerProfile,
        kidsSettings.maximumAge,
        kidsOverrides.toList(),
    ) {
        val snapshot = cachedSnapshot ?: return@LaunchedEffect
        val channelTitles = if (viewerProfile == ViewerProfile.Kids) {
            kidsEligibilityPolicy.filter(snapshot.titles, kidsSettings.maximumAge, kidsOverrides)
        } else {
            snapshot.titles
        }
        withContext(Dispatchers.IO) {
            runCatching { tvHomeChannelPublisher.refresh(snapshot, channelTitles) }
                .onFailure { error -> Log.w("CouchIndexTv", "TV home channel refresh failed", error) }
        }
    }

    fun completeParentAction(action: ParentAction) {
        when (action) {
            ParentAction.EnterKids -> {
                viewerProfile = ViewerProfile.Kids
                destination = Destination.Home
                selectedTitle = null
                searchQuery = ""
            }

            ParentAction.ExitKids -> {
                viewerProfile = ViewerProfile.Adult
                destination = Destination.Home
                selectedTitle = null
                searchQuery = ""
            }

            ParentAction.ToggleStartInKids -> {
                kidsSettingsStore.setStartInKidsMode(!kidsSettings.startInKidsMode)
                kidsSettings = kidsSettingsStore.load()
            }

            is ParentAction.SetMaximumAge -> {
                kidsSettingsStore.setMaximumAge(action.age)
                kidsSettings = kidsSettingsStore.load()
            }

            ParentAction.OpenParentalControls -> {
                destination = Destination.ParentalControls
                selectedTitle = null
                parentalSearchQuery = ""
            }

            is ParentAction.SetKidsOverride -> {
                kidsOverrides.clear()
                kidsOverrides.addAll(
                    kidsOverrideStore.set(
                        titleId = action.titleId,
                        titleName = action.titleName,
                        decision = action.decision,
                        allowsOverAge = action.allowsOverAge,
                    ),
                )
                if (action.decision == KidsOverrideDecision.Blocked) {
                    kidsRecentLaunchStore.remove(action.titleId)
                }
            }
        }
    }

    fun requestParentAction(action: ParentAction, requiresPin: Boolean) {
        pinFlow = when {
            !kidsSettingsStore.hasPin() -> PinFlow.Setup(action)
            requiresPin -> PinFlow.Verify(action)
            else -> {
                completeParentAction(action)
                null
            }
        }
    }

    LaunchedEffect(appConfig.hasTmdbReadAccessToken) {
        if (!appConfig.hasTmdbReadAccessToken) {
            providers = SampleCatalogue.providers
            providerDirectoryReady = true
            return@LaunchedEffect
        }

        providerDirectoryReady = false
        catalogueStatus = cachedSnapshot?.toCatalogueStatus(
            nowEpochMillis = System.currentTimeMillis(),
            prefix = "Refreshing provider directory",
        ) ?: CatalogueStatus("Loading Danish provider directory", "Loading", highlighted = true)
        runCatching { providerDirectory.fetchProviders(DEFAULT_REGION) }
            .onSuccess { discoveredProviders ->
                if (discoveredProviders.isNotEmpty()) {
                    val currentSubscriptions = subscriptions.associate { it.providerId to it.enabled }
                    val sampleDefaults = SampleCatalogue.subscriptions.associate { it.providerId to it.enabled }
                    val defaults = discoveredProviders.map { provider ->
                        Subscription(
                            providerId = provider.id,
                            enabled = currentSubscriptions[provider.id] ?: sampleDefaults[provider.id] ?: false,
                        )
                    }
                    providers = discoveredProviders
                    subscriptions.clear()
                    subscriptions.addAll(subscriptionStore.load(defaults))
                }
            }
            .onFailure {
                providers = cachedSnapshot?.providers ?: SampleCatalogue.providers
            }
        providerDirectoryReady = true
    }

    LaunchedEffect(appConfig.hasTmdbReadAccessToken) {
        if (!appConfig.hasTmdbReadAccessToken) {
            genres = SampleCatalogue.genres
            genreDirectoryReady = true
            return@LaunchedEffect
        }

        genreDirectoryReady = false
        runCatching { genreDirectory.fetchGenres() }
            .onSuccess { discoveredGenres ->
                if (discoveredGenres.isNotEmpty()) genres = discoveredGenres
            }
            .onFailure {
                genres = cachedSnapshot?.genres?.takeIf { cached -> cached.isNotEmpty() } ?: SampleCatalogue.genres
            }
        genreDirectoryReady = true
    }

    LaunchedEffect(
        appConfig.hasTmdbReadAccessToken,
        providerDirectoryReady,
        genreDirectoryReady,
        enabledProviderIds,
        providerSignature,
        genreSignature,
    ) {
        if (!appConfig.hasTmdbReadAccessToken) {
            catalogue = SampleCatalogue.titles
            catalogueStatus = CatalogueStatus("Using sample catalogue", "Local", highlighted = false)
            return@LaunchedEffect
        }

        if (!providerDirectoryReady || !genreDirectoryReady) return@LaunchedEffect

        if (enabledProviderIds.isEmpty()) {
            catalogue = emptyList()
            catalogueStatus = CatalogueStatus("No subscriptions enabled", "Ready", highlighted = true)
            return@LaunchedEffect
        }

        catalogueStatus = cachedSnapshot?.toCatalogueStatus(
            nowEpochMillis = System.currentTimeMillis(),
            prefix = "Refreshing Danish availability",
        ) ?: CatalogueStatus("Refreshing Danish availability", "Loading", highlighted = true)
        val refreshResult = runCatching {
            TmdbCatalogueRepository(
                source = tmdbClient,
                providers = providers,
                batchRatingAdapters = listOf(imdbRatingAdapter),
            ).discoverSubscriptionTitles(
                region = DEFAULT_REGION,
                providerIds = enabledProviderIds,
            )
        }
        refreshResult.onSuccess { discoveredTitles ->
            catalogue = discoveredTitles
            val snapshot = CatalogueSnapshot(
                region = DEFAULT_REGION,
                savedAtEpochMillis = System.currentTimeMillis(),
                providers = providers,
                titles = discoveredTitles,
                genres = genres,
            )
            cachedSnapshot = snapshot
            withContext(Dispatchers.IO) {
                runCatching { catalogueCacheStore.save(snapshot) }
            }
            catalogueStatus = CatalogueStatus(
                detail = "${discoveredTitles.size} titles - JustWatch availability",
                badge = "Live",
                highlighted = true,
            )
        }.onFailure {
            val cached = cachedSnapshot
            if (cached == null) {
                catalogue = SampleCatalogue.titles
                catalogueStatus = CatalogueStatus("Refresh failed; using sample catalogue", "Offline", highlighted = false)
            } else {
                catalogue = cached.titles
                providers = cached.providers
                genres = cached.genres
                catalogueStatus = cached.toCatalogueStatus(
                    nowEpochMillis = System.currentTimeMillis(),
                    prefix = "Refresh failed",
                )
            }
        }
    }

    LaunchedEffect(rows) {
        if (selectedTitle == null || rows.none { row -> row.titles.any { it.id == selectedTitle?.id } }) {
            selectedTitle = rows.firstOrNull { it.titles.isNotEmpty() }?.titles?.firstOrNull()
        }
    }

    LaunchedEffect(
        requestedTitleId,
        visibleCatalogue,
        catalogue,
        providerDirectoryReady,
        genreDirectoryReady,
    ) {
        val titleId = requestedTitleId ?: return@LaunchedEffect
        val requestedTitle = visibleCatalogue.firstOrNull { it.id == titleId }
        when {
            requestedTitle != null -> {
                destination = Destination.Home
                selectedTitle = requestedTitle
                onRequestedTitleHandled()
            }

            catalogue.any { it.id == titleId } -> onRequestedTitleHandled()
            providerDirectoryReady && genreDirectoryReady -> onRequestedTitleHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0E1114),
                        Color(0xFF172023),
                        Color(0xFF101315),
                    ),
                ),
            ),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            DestinationRail(
                selected = destination,
                footerLabel = catalogueStatus.railLabel,
                viewerProfile = viewerProfile,
                onSelect = { destination = it },
                onModeAction = {
                    val action = if (viewerProfile == ViewerProfile.Adult) {
                        ParentAction.EnterKids
                    } else {
                        ParentAction.ExitKids
                    }
                    requestParentAction(action, requiresPin = viewerProfile == ViewerProfile.Kids)
                },
            )
            MainSurface(
                destination = destination,
                rows = rows,
                appConfig = appConfig,
                catalogueStatus = catalogueStatus,
                viewerProfile = viewerProfile,
                kidsSettings = kidsSettings,
                catalogue = catalogue,
                kidsOverrides = kidsOverrides,
                genres = genres,
                providers = providers,
                subscriptions = subscriptions,
                selectedTitle = selectedTitle,
                searchQuery = searchQuery,
                parentalSearchQuery = parentalSearchQuery,
                browseMediaFilter = browseMediaFilter,
                browseGenreId = browseGenreId,
                browseSort = browseSort,
                watchlistedTitleIds = watchlistedTitleIds,
                watchedTitleIds = watchedTitleIds,
                recentTitleIds = recentTitleIds,
                selectedFeedback = selectedTitle?.let { feedbackByTitleId[it.id] },
                onTitleSelected = { selectedTitle = it },
                onSearchQueryChange = { searchQuery = it },
                onParentalSearchQueryChange = { parentalSearchQuery = it },
                onBrowseMediaFilterChange = { filter ->
                    browseMediaFilter = filter
                    val selectedGenre = genres.firstOrNull { it.id == browseGenreId }
                    val selectedKind = when (filter) {
                        BrowseMediaFilter.All -> null
                        BrowseMediaFilter.Movies -> MediaKind.Movie
                        BrowseMediaFilter.Series -> MediaKind.Series
                    }
                    if (
                        browseGenreId != null &&
                        selectedKind != null &&
                        selectedGenre?.mediaKinds?.contains(selectedKind) != true
                    ) {
                        browseGenreId = null
                    }
                },
                onBrowseGenreChange = { browseGenreId = it },
                onBrowseSortChange = { browseSort = it },
                resolveLaunchTarget = providerLauncher::resolve,
                onLaunchTargetSelected = { title, target ->
                    when (providerLauncher.launch(target)) {
                        ProviderLaunchResult.ProviderAppLaunched,
                        ProviderLaunchResult.WatchOptionsLaunched,
                        -> {
                            recentLaunches.clear()
                            recentLaunches.addAll(recentLaunchStore.record(title.id))
                        }

                        ProviderLaunchResult.InstallPageLaunched ->
                            Toast.makeText(context, "Opening provider install page", Toast.LENGTH_SHORT).show()

                        ProviderLaunchResult.ActivityUnavailable ->
                            Toast.makeText(context, "No launch option available", Toast.LENGTH_SHORT).show()
                    }
                },
                onSubscriptionToggle = { providerId ->
                    val index = subscriptions.indexOfFirst { it.providerId == providerId }
                    if (index >= 0) {
                        val existing = subscriptions[index]
                        val updated = existing.copy(enabled = !existing.enabled)
                        subscriptions[index] = updated
                        subscriptionStore.setEnabled(updated.providerId, updated.enabled)
                    }
                },
                onStartInKidsModeToggle = {
                    requestParentAction(ParentAction.ToggleStartInKids, requiresPin = true)
                },
                onMaximumAgeSelected = { age ->
                    requestParentAction(ParentAction.SetMaximumAge(age), requiresPin = true)
                },
                onOpenParentalControls = {
                    requestParentAction(ParentAction.OpenParentalControls, requiresPin = true)
                },
                onPrivacyPolicySelected = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PrivacyPolicy.URL)))
                    }.onFailure {
                        Toast.makeText(context, "Privacy policy is unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                onWatchlistToggle = { title ->
                    val isMember = title.id in watchlistedTitleIds
                    watchlistEntries.clear()
                    watchlistEntries.addAll(watchlistStore.setMembership(title.id, !isMember))
                },
                onWatchedToggle = { title ->
                    val isWatched = title.id in watchedTitleIds
                    watchedEntries.clear()
                    watchedEntries.addAll(watchedStore.setWatched(title.id, !isWatched))
                    if (!isWatched) {
                        recentLaunches.clear()
                        recentLaunches.addAll(recentLaunchStore.remove(title.id))
                    }
                },
                onContinueWatchingRemove = { title ->
                    recentLaunches.clear()
                    recentLaunches.addAll(recentLaunchStore.remove(title.id))
                },
                onFeedbackChange = { title, value ->
                    val updatedValue = value.takeUnless { feedbackByTitleId[title.id] == value }
                    feedbackEntries.clear()
                    feedbackEntries.addAll(feedbackStore.set(title.id, updatedValue))
                },
                onKidsOverrideChange = { title, decision, allowsOverAge ->
                    requestParentAction(
                        ParentAction.SetKidsOverride(
                            titleId = title.id,
                            titleName = title.name,
                            decision = decision,
                            allowsOverAge = allowsOverAge,
                        ),
                        requiresPin = destination != Destination.ParentalControls,
                    )
                },
            )
        }
        pinFlow?.let { flow ->
            ParentPinDialog(
                title = when (flow) {
                    is PinFlow.Setup -> if (flow.firstPin == null) "Create parent PIN" else "Confirm parent PIN"
                    is PinFlow.Verify -> "Enter parent PIN"
                },
                message = when (flow) {
                    is PinFlow.Setup -> "Use four digits. Clearing app data resets a forgotten PIN."
                    is PinFlow.Verify -> flow.action.parentPinMessage(kidsSettings.maximumAge)
                },
                error = flow.error,
                onCancel = { pinFlow = null },
                onSubmit = { pin ->
                    when (flow) {
                        is PinFlow.Setup -> {
                            val firstPin = flow.firstPin
                            if (firstPin == null) {
                                pinFlow = flow.copy(firstPin = pin, error = null)
                            } else if (firstPin == pin) {
                                kidsSettingsStore.setPin(pin)
                                completeParentAction(flow.action)
                                pinFlow = null
                            } else {
                                pinFlow = PinFlow.Setup(flow.action, error = "PINs did not match")
                            }
                        }

                        is PinFlow.Verify -> {
                            if (kidsSettingsStore.verifyPin(pin)) {
                                completeParentAction(flow.action)
                                pinFlow = null
                            } else {
                                pinFlow = flow.copy(error = "Incorrect PIN")
                            }
                        }
                    }
                },
            )
        }
    }
}

private fun CatalogueSnapshot.toCatalogueStatus(
    nowEpochMillis: Long,
    prefix: String? = null,
): CatalogueStatus {
    val status = cacheStatus(nowEpochMillis, prefix)
    return CatalogueStatus(
        detail = status.detail,
        badge = status.badge,
        highlighted = status.highlighted,
    )
}

private const val DEFAULT_REGION = "DK"

@Composable
private fun DestinationRail(
    selected: Destination,
    footerLabel: String,
    viewerProfile: ViewerProfile,
    onSelect: (Destination) -> Unit,
    onModeAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(178.dp)
            .fillMaxHeight()
            .background(Color(0xFF0A0C0E))
            .padding(start = 28.dp, top = 34.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText(
            text = if (viewerProfile == ViewerProfile.Kids) "CouchIndex Kids" else "CouchIndex",
            style = TextStyle(
                color = Color(0xFFF4F1E8),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        BasicText(
            text = if (viewerProfile == ViewerProfile.Kids) "family catalogue" else "personal TV index",
            style = TextStyle(
                color = if (viewerProfile == ViewerProfile.Kids) Color(0xFF8BC7B5) else Color(0xFF8FA0A5),
                fontSize = 12.sp,
            ),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Destination.entries
            .filter { destination ->
                destination != Destination.ParentalControls &&
                    (viewerProfile == ViewerProfile.Adult || destination != Destination.Settings)
            }
            .forEach { destination ->
            FocusButton(
                label = destination.label,
                selected = selected == destination,
                onClick = { onSelect(destination) },
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        FocusButton(
            label = if (viewerProfile == ViewerProfile.Kids) "Adult mode" else "Kids mode",
            selected = viewerProfile == ViewerProfile.Kids,
            onClick = onModeAction,
        )
        Spacer(modifier = Modifier.weight(1f))
        BasicText(
            text = footerLabel,
            style = TextStyle(color = Color(0xFF6D7B80), fontSize = 12.sp),
        )
    }
}

@Composable
private fun MainSurface(
    destination: Destination,
    rows: List<BrowseRow>,
    appConfig: AppConfig,
    catalogueStatus: CatalogueStatus,
    viewerProfile: ViewerProfile,
    kidsSettings: KidsSettings,
    catalogue: List<Title>,
    kidsOverrides: List<KidsCatalogueOverride>,
    genres: List<Genre>,
    providers: List<Provider>,
    subscriptions: List<Subscription>,
    selectedTitle: Title?,
    searchQuery: String,
    parentalSearchQuery: String,
    browseMediaFilter: BrowseMediaFilter,
    browseGenreId: Int?,
    browseSort: BrowseSort,
    watchlistedTitleIds: Set<TitleId>,
    watchedTitleIds: Set<TitleId>,
    recentTitleIds: Set<TitleId>,
    selectedFeedback: FeedbackValue?,
    onTitleSelected: (Title) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onParentalSearchQueryChange: (String) -> Unit,
    onBrowseMediaFilterChange: (BrowseMediaFilter) -> Unit,
    onBrowseGenreChange: (Int?) -> Unit,
    onBrowseSortChange: (BrowseSort) -> Unit,
    resolveLaunchTarget: (LaunchTarget?) -> ResolvedProviderLaunch,
    onLaunchTargetSelected: (Title, LaunchTarget?) -> Unit,
    onSubscriptionToggle: (String) -> Unit,
    onStartInKidsModeToggle: () -> Unit,
    onMaximumAgeSelected: (Int) -> Unit,
    onOpenParentalControls: () -> Unit,
    onPrivacyPolicySelected: () -> Unit,
    onWatchlistToggle: (Title) -> Unit,
    onWatchedToggle: (Title) -> Unit,
    onContinueWatchingRemove: (Title) -> Unit,
    onFeedbackChange: (Title, FeedbackValue) -> Unit,
    onKidsOverrideChange: (Title, KidsOverrideDecision?, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 34.dp, top = 32.dp, end = 34.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ScreenHeader(
                destination = destination,
                subscriptions = subscriptions,
                providers = providers,
                viewerProfile = viewerProfile,
            )
            Spacer(modifier = Modifier.height(22.dp))
            when (destination) {
                Destination.Home -> HomeScreen(
                    rows = rows,
                    providers = providers,
                    onTitleSelected = onTitleSelected,
                )

                Destination.Browse -> BrowseScreen(
                    rows = rows,
                    genres = genres,
                    mediaFilter = browseMediaFilter,
                    genreId = browseGenreId,
                    sort = browseSort,
                    providers = providers,
                    onMediaFilterChange = onBrowseMediaFilterChange,
                    onGenreChange = onBrowseGenreChange,
                    onSortChange = onBrowseSortChange,
                    onTitleSelected = onTitleSelected,
                )

                Destination.Search -> SearchScreen(
                    titles = rows.flatMap { it.titles }.distinctBy { it.id },
                    query = searchQuery,
                    providers = providers,
                    onQueryChange = onSearchQueryChange,
                    onTitleSelected = onTitleSelected,
                )

                Destination.Settings -> SettingsScreen(
                    appConfig = appConfig,
                    catalogueStatus = catalogueStatus,
                    providers = providers,
                    subscriptions = subscriptions,
                    kidsSettings = kidsSettings,
                    onSubscriptionToggle = onSubscriptionToggle,
                    onStartInKidsModeToggle = onStartInKidsModeToggle,
                    onMaximumAgeSelected = onMaximumAgeSelected,
                    onOpenParentalControls = onOpenParentalControls,
                    onPrivacyPolicySelected = onPrivacyPolicySelected,
                )

                Destination.ParentalControls -> ParentalControlsScreen(
                    catalogue = catalogue,
                    query = parentalSearchQuery,
                    overrides = kidsOverrides,
                    providers = providers,
                    onQueryChange = onParentalSearchQueryChange,
                    onTitleSelected = onTitleSelected,
                )
            }
        }
        DetailsPanel(
            title = selectedTitle,
            providers = providers,
            isWatchlisted = selectedTitle?.id in watchlistedTitleIds,
            isWatched = selectedTitle?.id in watchedTitleIds,
            isInContinueWatching = selectedTitle?.id in recentTitleIds,
            feedback = selectedFeedback,
            viewerProfile = viewerProfile,
            kidsMaximumAge = kidsSettings.maximumAge,
            kidsOverride = selectedTitle?.let { title -> kidsOverrides.firstOrNull { it.titleId == title.id } },
            resolveLaunchTarget = resolveLaunchTarget,
            onLaunchTargetSelected = onLaunchTargetSelected,
            onWatchlistToggle = onWatchlistToggle,
            onWatchedToggle = onWatchedToggle,
            onContinueWatchingRemove = onContinueWatchingRemove,
            onFeedbackChange = onFeedbackChange,
            onKidsOverrideChange = onKidsOverrideChange,
        )
    }
}

@Composable
private fun SearchScreen(
    titles: List<Title>,
    query: String,
    providers: List<Provider>,
    onQueryChange: (String) -> Unit,
    onTitleSelected: (Title) -> Unit,
) {
    val searchCatalogue = remember { SearchCatalogue() }
    val results by remember(titles, query) {
        derivedStateOf { searchCatalogue.invoke(titles, query) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SearchInput(query = query, onQueryChange = onQueryChange)
        BasicText(
            text = when {
                query.isBlank() -> ""
                results.isEmpty() -> "No matching titles"
                results.size == 1 -> "1 title"
                else -> "${results.size} titles"
            },
            modifier = Modifier.height(20.dp),
            style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 13.sp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(results, key = { "${it.id.mediaKind}-${it.id.tmdbId}" }) { title ->
                BrowseListItem(
                    title = title,
                    providers = providers,
                    onTitleSelected = onTitleSelected,
                )
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search titles",
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(8.dp)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown
                ) {
                    focusManager.moveFocus(FocusDirection.Down)
                } else {
                    false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Color(0xFF151B1E))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8C468) else Color(0xFF3A484D), shape)
            .padding(horizontal = 18.dp),
        singleLine = true,
        textStyle = TextStyle(color = Color(0xFFF4F1E8), fontSize = 18.sp),
        cursorBrush = SolidColor(Color(0xFFE8C468)),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 18.sp),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ScreenHeader(
    destination: Destination,
    subscriptions: List<Subscription>,
    providers: List<Provider>,
    viewerProfile: ViewerProfile,
) {
    val enabledProviders = subscriptions
        .filter { it.enabled }
        .mapNotNull { subscription -> providers.firstOrNull { it.id == subscription.providerId }?.shortName }
        .joinToString(" / ")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = destination.label,
                style = TextStyle(
                    color = Color(0xFFF4F1E8),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = if (viewerProfile == ViewerProfile.Kids) {
                    "Only parent-approved age ratings. Your activity stays separate."
                } else {
                    "Browse subscribed services first. Ratings stay separate."
                },
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 15.sp),
            )
        }
        BasicText(
            text = enabledProviders.ifBlank { "No providers enabled" },
            modifier = Modifier.padding(start = 24.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color(0xFFE8C468), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun HomeScreen(
    rows: List<BrowseRow>,
    providers: List<Provider>,
    onTitleSelected: (Title) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        items(rows, key = { it.id }) { row ->
            BrowseRowSection(
                row = row,
                providers = providers,
                onTitleSelected = onTitleSelected,
            )
        }
    }
}

@Composable
private fun BrowseScreen(
    rows: List<BrowseRow>,
    genres: List<Genre>,
    mediaFilter: BrowseMediaFilter,
    genreId: Int?,
    sort: BrowseSort,
    providers: List<Provider>,
    onMediaFilterChange: (BrowseMediaFilter) -> Unit,
    onGenreChange: (Int?) -> Unit,
    onSortChange: (BrowseSort) -> Unit,
    onTitleSelected: (Title) -> Unit,
) {
    val browseCatalogue = remember { BrowseCatalogue() }
    val sourceTitles = rows
        .flatMap { it.titles }
        .distinctBy { it.id }
    val titles by remember(sourceTitles, mediaFilter, genreId, sort) {
        derivedStateOf {
            browseCatalogue.invoke(
                sourceTitles,
                BrowseCatalogueQuery(mediaFilter = mediaFilter, genreId = genreId, sort = sort),
            )
        }
    }
    val selectedKind = when (mediaFilter) {
        BrowseMediaFilter.All -> null
        BrowseMediaFilter.Movies -> MediaKind.Movie
        BrowseMediaFilter.Series -> MediaKind.Series
    }
    val availableGenres = genres.filter { genre ->
        (selectedKind == null || selectedKind in genre.mediaKinds) && sourceTitles.any { genre.id in it.genreIds }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BrowseMediaFilter.entries, key = { it.name }) { filter ->
                FocusButton(
                    label = filter.label,
                    selected = filter == mediaFilter,
                    modifier = Modifier.width(100.dp),
                    onClick = { onMediaFilterChange(filter) },
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FocusButton(
                    label = "All genres",
                    selected = genreId == null,
                    modifier = Modifier.width(160.dp),
                    onClick = { onGenreChange(null) },
                )
            }
            items(availableGenres, key = { it.id }) { genre ->
                FocusButton(
                    label = genre.name,
                    selected = genre.id == genreId,
                    modifier = Modifier.width(180.dp),
                    onClick = { onGenreChange(genre.id) },
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BrowseSort.entries, key = { it.name }) { option ->
                FocusButton(
                    label = option.label,
                    selected = option == sort,
                    modifier = Modifier.width(110.dp),
                    onClick = { onSortChange(option) },
                )
            }
        }
        BasicText(
            text = if (titles.size == 1) "1 title" else "${titles.size} titles",
            modifier = Modifier.height(20.dp),
            style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 13.sp),
        )
        if (titles.isEmpty()) {
            EmptyRow(label = "No titles in this category")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(titles, key = { "${it.id.mediaKind}-${it.id.tmdbId}" }) { title ->
                    BrowseListItem(
                        title = title,
                        providers = providers,
                        ratingSource = sort.ratingSource,
                        onTitleSelected = onTitleSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    appConfig: AppConfig,
    catalogueStatus: CatalogueStatus,
    providers: List<Provider>,
    subscriptions: List<Subscription>,
    kidsSettings: KidsSettings,
    onSubscriptionToggle: (String) -> Unit,
    onStartInKidsModeToggle: () -> Unit,
    onMaximumAgeSelected: (Int) -> Unit,
    onOpenParentalControls: () -> Unit,
    onPrivacyPolicySelected: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            BasicText(
                text = "Kids mode",
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        item {
            SettingToggle(
                name = "Start in Kids mode",
                detail = "Open the age-filtered profile on cold launch",
                enabled = kidsSettings.startInKidsMode,
                onClick = onStartInKidsModeToggle,
            )
        }
        item {
            val currentIndex = KidsSettings.SUPPORTED_AGES.indexOf(kidsSettings.maximumAge).coerceAtLeast(0)
            val nextAge = KidsSettings.SUPPORTED_AGES[(currentIndex + 1) % KidsSettings.SUPPORTED_AGES.size]
            FocusButton(
                label = "Maximum age: ${kidsSettings.maximumAge}+",
                selected = false,
                onClick = { onMaximumAgeSelected(nextAge) },
            )
        }
        item {
            FocusButton(
                label = "Manage Kids catalogue",
                selected = false,
                onClick = onOpenParentalControls,
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            BasicText(
                text = "About",
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        item {
            FocusButton(
                label = "Privacy policy",
                selected = false,
                onClick = onPrivacyPolicySelected,
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            BasicText(
                text = "Subscriptions",
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        items(providers, key = { it.id }) { provider ->
            val enabled = subscriptions.firstOrNull { it.providerId == provider.id }?.enabled == true
            ProviderToggle(
                provider = provider,
                enabled = enabled,
                onClick = { onSubscriptionToggle(provider.id) },
            )
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            BasicText(
                text = "Integrations",
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        item {
            IntegrationStatus(
                name = "TMDb",
                configured = appConfig.hasTmdbReadAccessToken,
                status = catalogueStatus,
            )
        }
    }
}

@Composable
private fun ParentalControlsScreen(
    catalogue: List<Title>,
    query: String,
    overrides: List<KidsCatalogueOverride>,
    providers: List<Provider>,
    onQueryChange: (String) -> Unit,
    onTitleSelected: (Title) -> Unit,
) {
    val searchCatalogue = remember { SearchCatalogue() }
    val catalogueById = catalogue.associateBy { it.id }
    val overriddenTitles = overrides.map { override ->
        catalogueById[override.titleId] ?: override.toPlaceholderTitle()
    }
    val titles = if (query.isBlank()) {
        overriddenTitles
    } else {
        searchCatalogue.invoke(catalogue, query)
    }
    val overrideByTitleId = overrides.associateBy { it.titleId }
    val blockedCount = overrides.count { it.decision == KidsOverrideDecision.Blocked }
    val allowedCount = overrides.count { it.decision == KidsOverrideDecision.Allowed }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SearchInput(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Find a title to allow or block",
        )
        BasicText(
            text = if (query.isBlank()) {
                "$blockedCount hidden / $allowedCount manually allowed"
            } else {
                "${titles.size} matching titles"
            },
            modifier = Modifier.height(20.dp),
            style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 13.sp),
        )
        if (titles.isEmpty()) {
            EmptyRow(label = if (query.isBlank()) "No Kids catalogue overrides" else "No matching titles")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(titles, key = { "parent-${it.id.mediaKind}-${it.id.tmdbId}" }) { title ->
                    ParentalControlListItem(
                        title = title,
                        providers = providers,
                        override = overrideByTitleId[title.id],
                        onTitleSelected = onTitleSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentalControlListItem(
    title: Title,
    providers: List<Provider>,
    override: KidsCatalogueOverride?,
    onTitleSelected: (Title) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        BrowseListItem(title = title, providers = providers, onTitleSelected = onTitleSelected)
        BasicText(
            text = when (override?.decision) {
                KidsOverrideDecision.Blocked -> "Hidden from Kids"
                KidsOverrideDecision.Allowed -> if (override.allowsOverAge) {
                    "Allowed with age exception"
                } else {
                    "Manually allowed"
                }
                null -> "Uses age-rating rule"
            },
            modifier = Modifier.padding(start = 16.dp),
            style = TextStyle(
                color = if (override?.decision == KidsOverrideDecision.Blocked) {
                    Color(0xFFE18B78)
                } else {
                    Color(0xFF8BC7B5)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun BrowseRowSection(
    row: BrowseRow,
    providers: List<Provider>,
    onTitleSelected: (Title) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(
            text = row.label,
            style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 22.sp, fontWeight = FontWeight.Bold),
        )
        if (row.titles.isEmpty()) {
            EmptyRow(label = "No titles yet")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(row.titles, key = { "${row.id}-${it.id.mediaKind}-${it.id.tmdbId}" }) { title ->
                    TitleCard(
                        title = title,
                        providers = providers,
                        onClick = { onTitleSelected(title) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRow(label: String) {
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(138.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161C1F))
            .border(1.dp, Color(0xFF2A3438), RoundedCornerShape(8.dp))
            .padding(18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 15.sp),
        )
    }
}

@Composable
private fun TitleCard(
    title: Title,
    providers: List<Provider>,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (focused) Color(0xFFE8C468) else Color(0xFF2B363A)
    Column(
        modifier = Modifier
            .width(156.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Color(0xFF12171A))
            .border(if (focused) 3.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .focusable()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(posterColor(title)),
        ) {
            BasicText(
                text = title.name.take(2).uppercase(),
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    color = Color(0xFF0E1114),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            AsyncImage(
                model = title.posterUrl,
                contentDescription = "${title.name} poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            BasicText(
                text = title.mediaKind.label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color(0xCC0E1114), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
        }
        BasicText(
            text = title.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            text = title.providerLabels(providers).joinToString(" / "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color(0xFF9FB0B5), fontSize = 12.sp),
        )
    }
}

@Composable
private fun BrowseListItem(
    title: Title,
    providers: List<Provider>,
    ratingSource: String? = null,
    onTitleSelected: (Title) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0xFF232E32) else Color(0xFF151B1E))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8C468) else Color(0xFF273236), shape)
            .clickable { onTitleSelected(title) }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 72.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(posterColor(title)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = title.name.take(1).uppercase(),
                style = TextStyle(color = Color(0xFF0E1114), fontSize = 22.sp, fontWeight = FontWeight.Black),
            )
            AsyncImage(
                model = title.posterUrl,
                contentDescription = "${title.name} poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                text = title.name,
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
            )
            BasicText(
                text = "${title.mediaKind.label} ${title.year ?: ""}  ${title.providerLabels(providers).joinToString(" / ")}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 13.sp),
            )
        }
        val displayedRating = ratingSource?.let { source ->
            title.ratings.firstOrNull { it.source.equals(source, ignoreCase = true) }
        } ?: title.ratings.firstOrNull()
        BasicText(
            text = displayedRating?.let { rating ->
                if (ratingSource == null) rating.display() else "${rating.source} ${rating.display()}"
            }.orEmpty(),
            style = TextStyle(color = Color(0xFFE8C468), fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun DetailsPanel(
    title: Title?,
    providers: List<Provider>,
    isWatchlisted: Boolean,
    isWatched: Boolean,
    isInContinueWatching: Boolean,
    feedback: FeedbackValue?,
    viewerProfile: ViewerProfile,
    kidsMaximumAge: Int,
    kidsOverride: KidsCatalogueOverride?,
    resolveLaunchTarget: (LaunchTarget?) -> ResolvedProviderLaunch,
    onLaunchTargetSelected: (Title, LaunchTarget?) -> Unit,
    onWatchlistToggle: (Title) -> Unit,
    onWatchedToggle: (Title) -> Unit,
    onContinueWatchingRemove: (Title) -> Unit,
    onFeedbackChange: (Title, FeedbackValue) -> Unit,
    onKidsOverrideChange: (Title, KidsOverrideDecision?, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(350.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE6111518))
            .border(1.dp, Color(0xFF2B363A), RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title == null) {
            BasicText(
                text = "Select a title",
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 24.sp, fontWeight = FontWeight.Bold),
            )
            BasicText(
                text = "Rows will stay available even while network refreshes are pending.",
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 14.sp),
            )
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(posterColor(title)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = title.name.take(2).uppercase(),
                style = TextStyle(color = Color(0xFF0E1114), fontSize = 42.sp, fontWeight = FontWeight.Black),
            )
            AsyncImage(
                model = title.posterUrl,
                contentDescription = "${title.name} poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        BasicText(
            text = title.name,
            style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 25.sp, fontWeight = FontWeight.Bold),
        )
        BasicText(
            text = listOfNotNull(title.mediaKind.label, title.year?.toString(), title.runtimeLabel()).joinToString(" / "),
            style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 14.sp),
        )
        val launchOptions = title.launchTargets
            .map { target -> target to resolveLaunchTarget(target) }
            .distinctBy { (_, resolved) -> listOf(resolved.mode, resolved.label, resolved.uri) }

        if (launchOptions.isEmpty()) {
            FocusButton(
                label = resolveLaunchTarget(null).label,
                selected = true,
                onClick = { onLaunchTargetSelected(title, null) },
            )
        } else {
            launchOptions.take(3).forEachIndexed { index, (target, resolved) ->
                FocusButton(
                    label = resolved.label,
                    selected = index == 0,
                    onClick = { onLaunchTargetSelected(title, target) },
                )
            }
        }
        FocusButton(
            label = if (isWatchlisted) "Remove from Watchlist" else "Add to Watchlist",
            selected = false,
            onClick = { onWatchlistToggle(title) },
        )
        FocusButton(
            label = if (isWatched) "Mark Unwatched" else "Mark Watched",
            selected = false,
            onClick = { onWatchedToggle(title) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocusButton(
                label = "Like",
                selected = feedback == FeedbackValue.Liked,
                onClick = { onFeedbackChange(title, FeedbackValue.Liked) },
                modifier = Modifier.weight(1f),
            )
            FocusButton(
                label = "Dislike",
                selected = feedback == FeedbackValue.Disliked,
                onClick = { onFeedbackChange(title, FeedbackValue.Disliked) },
                modifier = Modifier.weight(1f),
            )
        }
        if (isInContinueWatching) {
            FocusButton(
                label = "Remove from Continue Watching",
                selected = false,
                onClick = { onContinueWatchingRemove(title) },
            )
        }
        if (viewerProfile == ViewerProfile.Adult) {
            val certificationAge = title.certification?.minimumAge
            val baseEligible = certificationAge != null && certificationAge <= kidsMaximumAge
            val overrideLabel = when (kidsOverride?.decision) {
                KidsOverrideDecision.Blocked -> "Hidden from Kids"
                KidsOverrideDecision.Allowed -> if (kidsOverride.allowsOverAge) {
                    "Allowed in Kids with age exception"
                } else {
                    "Manually allowed in Kids"
                }
                null -> if (baseEligible) "Allowed by age rating" else "Hidden by age-rating rule"
            }
            LabelBlock(label = "Kids catalogue", value = overrideLabel)
            when (kidsOverride?.decision) {
                KidsOverrideDecision.Blocked,
                KidsOverrideDecision.Allowed,
                -> FocusButton(
                    label = "Restore age-rating rule",
                    selected = false,
                    onClick = { onKidsOverrideChange(title, null, false) },
                )

                null -> if (baseEligible) {
                    FocusButton(
                        label = "Hide from Kids",
                        selected = false,
                        onClick = { onKidsOverrideChange(title, KidsOverrideDecision.Blocked, false) },
                    )
                } else {
                    val overAge = certificationAge?.let { it > kidsMaximumAge } == true
                    FocusButton(
                        label = if (overAge) "Allow ${certificationAge}+ title in Kids" else "Allow in Kids",
                        selected = false,
                        onClick = { onKidsOverrideChange(title, KidsOverrideDecision.Allowed, overAge) },
                    )
                }
            }
        }
        LabelBlock(label = "Available on", value = title.providerLabels(providers).joinToString(" / "))
        RatingStack(ratings = title.ratings)
        BasicText(
            text = title.synopsis,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = Color(0xFFD4DADB), fontSize = 14.sp),
        )
    }
}

@Composable
private fun RatingStack(ratings: List<Rating>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = "Ratings",
            style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
        ratings.forEach { rating ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText(
                    text = rating.source,
                    style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 14.sp),
                )
                BasicText(
                    text = "${rating.display()} (${rating.voteCountLabel()})",
                    style = TextStyle(color = Color(0xFFE8C468), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun LabelBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        BasicText(
            text = label,
            style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
        BasicText(
            text = value,
            style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 14.sp),
        )
    }
}

@Composable
private fun ProviderToggle(
    provider: Provider,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0xFF232E32) else Color(0xFF151B1E))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8C468) else Color(0xFF273236), shape)
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            BasicText(
                text = provider.name,
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
            )
            BasicText(
                text = if (enabled) "Included in browse rows" else "Hidden by default",
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 13.sp),
            )
        }
        TogglePill(enabled = enabled)
    }
}

@Composable
private fun SettingToggle(
    name: String,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0xFF232E32) else Color(0xFF151B1E))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8C468) else Color(0xFF273236), shape)
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = name,
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
            )
            BasicText(
                text = detail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 13.sp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        TogglePill(enabled = enabled)
    }
}

@Composable
private fun IntegrationStatus(
    name: String,
    configured: Boolean,
    status: CatalogueStatus,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0xFF232E32) else Color(0xFF151B1E))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8C468) else Color(0xFF273236), shape)
            .focusable()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            BasicText(
                text = name,
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            )
            BasicText(
                text = if (configured) status.detail else "Using sample catalogue",
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 13.sp),
            )
        }
        BasicText(
            text = if (configured) status.badge else "Local",
            style = TextStyle(
                color = if (status.highlighted) Color(0xFFE8C468) else Color(0xFF8FA0A5),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun TogglePill(enabled: Boolean) {
    Box(
        modifier = Modifier
            .width(82.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (enabled) Color(0xFFE8C468) else Color(0xFF293238))
            .padding(4.dp),
        contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (enabled) Color(0xFF101315) else Color(0xFF8FA0A5)),
        )
    }
}

@Composable
private fun FocusButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        focused -> Color(0xFFE8C468)
        selected -> Color(0xFF263136)
        else -> Color.Transparent
    }
    val textColor = if (focused) Color(0xFF0E1114) else Color(0xFFF4F1E8)

    Box(
        modifier = modifier
            .height(44.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(
                width = if (selected || focused) 1.dp else 0.dp,
                color = if (focused) Color(0xFFE8C468) else Color(0xFF38464B),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ParentPinDialog(
    title: String,
    message: String,
    error: String?,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by remember(title) { mutableStateOf("") }
    var localError by remember(title) { mutableStateOf<String?>(null) }
    val firstButton = remember { FocusRequester() }

    LaunchedEffect(title) {
        firstButton.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0C0E)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(430.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF151B1E))
                .border(1.dp, Color(0xFF3A484D), RoundedCornerShape(8.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BasicText(
                text = title,
                style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 24.sp, fontWeight = FontWeight.Bold),
            )
            BasicText(
                text = message,
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 14.sp),
            )
            BasicText(
                text = (0 until 4).joinToString("  ") { index -> if (index < pin.length) "*" else "-" },
                style = TextStyle(color = Color(0xFFE8C468), fontSize = 28.sp, fontWeight = FontWeight.Bold),
            )
            (listOf("1", "2", "3", "4", "5", "6", "7", "8", "9") + listOf("Clear", "0", "OK"))
                .chunked(3)
                .forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEachIndexed { columnIndex, label ->
                            val buttonModifier = if (rowIndex == 0 && columnIndex == 0) {
                                Modifier.width(112.dp).focusRequester(firstButton)
                            } else {
                                Modifier.width(112.dp)
                            }
                            FocusButton(
                                label = label,
                                selected = false,
                                modifier = buttonModifier,
                                onClick = {
                                    localError = null
                                    when (label) {
                                        "Clear" -> pin = ""
                                        "OK" -> if (pin.length == 4) onSubmit(pin) else localError = "Enter four digits"
                                        else -> if (pin.length < 4) pin += label
                                    }
                                },
                            )
                        }
                    }
                }
            BasicText(
                text = localError ?: error.orEmpty(),
                modifier = Modifier.height(20.dp),
                style = TextStyle(color = Color(0xFFE18B78), fontSize = 13.sp),
            )
            FocusButton(
                label = "Cancel",
                selected = false,
                modifier = Modifier.width(356.dp),
                onClick = onCancel,
            )
        }
    }
}

private val MediaKind.label: String
    get() = when (this) {
        MediaKind.Movie -> "Movie"
        MediaKind.Series -> "Series"
    }

private val BrowseMediaFilter.label: String
    get() = when (this) {
        BrowseMediaFilter.All -> "All"
        BrowseMediaFilter.Movies -> "Movies"
        BrowseMediaFilter.Series -> "Series"
    }

private val BrowseSort.label: String
    get() = when (this) {
        BrowseSort.Title -> "Title"
        BrowseSort.Newest -> "Newest"
        BrowseSort.ImdbRating -> "IMDb rating"
        BrowseSort.TmdbRating -> "TMDb rating"
    }

private val BrowseSort.ratingSource: String?
    get() = when (this) {
        BrowseSort.ImdbRating -> "IMDb"
        BrowseSort.TmdbRating -> "TMDb"
        else -> null
    }

private fun ParentAction.parentPinMessage(maximumAge: Int): String = when (this) {
    ParentAction.OpenParentalControls -> "Parent access is required to manage the Kids catalogue."
    is ParentAction.SetKidsOverride -> when (decision) {
        KidsOverrideDecision.Blocked -> "Hide $titleName from every Kids surface?"
        KidsOverrideDecision.Allowed -> if (allowsOverAge) {
            "Allow $titleName in Kids despite the $maximumAge+ age limit?"
        } else {
            "Manually allow $titleName in Kids?"
        }
        null -> "Restore age-rating rules for $titleName?"
    }
    else -> "Parent access is required for this change."
}

private fun KidsCatalogueOverride.toPlaceholderTitle(): Title = Title(
    id = titleId,
    name = titleName.ifBlank { "Unavailable title" },
    year = null,
    mediaKind = titleId.mediaKind,
    runtimeMinutes = null,
    synopsis = "This title is not in the current subscription catalogue.",
    offers = emptyList(),
    ratings = emptyList(),
    launchTargets = emptyList(),
)

private fun Title.providerLabels(providers: List<Provider>): List<String> =
    offers.mapNotNull { offer -> providers.firstOrNull { it.id == offer.providerId }?.shortName }.distinct()

private fun Title.runtimeLabel(): String? =
    runtimeMinutes?.let { "$it min" }

private fun Rating.display(): String =
    if (scale == 10.0) {
        "%.1f/10".format(value)
    } else {
        "${value.toInt()}/${scale.toInt()}"
    }

private fun Rating.voteCountLabel(): String =
    voteCount?.compactCount() ?: "votes unknown"

private fun Int.compactCount(): String =
    when {
        this >= 1_000_000 -> "${this / 1_000_000}m"
        this >= 1_000 -> "${this / 1_000}k"
        else -> toString()
    }

private fun posterColor(title: Title): Brush {
    val colors = when (title.id.tmdbId % 5) {
        0 -> listOf(Color(0xFFE8C468), Color(0xFF4BA3C7))
        1 -> listOf(Color(0xFFD9A06B), Color(0xFF5E8C78))
        2 -> listOf(Color(0xFFC86464), Color(0xFFE8C468))
        3 -> listOf(Color(0xFF8DB7C9), Color(0xFFEEE6D2))
        else -> listOf(Color(0xFFA6B873), Color(0xFF4BA3C7))
    }
    return Brush.verticalGradient(colors)
}
