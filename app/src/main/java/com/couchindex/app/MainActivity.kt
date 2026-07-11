package com.couchindex.app

import android.os.Bundle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couchindex.app.config.AppConfig
import com.couchindex.app.config.AppConfigLoader
import com.couchindex.app.launch.AndroidProviderLauncher
import com.couchindex.app.launch.ProviderLaunchResult
import com.couchindex.core.BrowseRow
import com.couchindex.core.BuildHomeRows
import com.couchindex.core.LaunchTarget
import com.couchindex.core.MediaKind
import com.couchindex.core.Provider
import com.couchindex.core.Rating
import com.couchindex.core.SampleCatalogue
import com.couchindex.core.Subscription
import com.couchindex.core.Title

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CouchIndexApp()
        }
    }
}

private enum class Destination(val label: String) {
    Home("Home"),
    Browse("Browse"),
    Settings("Settings"),
}

@Composable
private fun CouchIndexApp() {
    val rowBuilder = remember { BuildHomeRows() }
    val context = LocalContext.current
    val appConfig = remember { AppConfigLoader.load() }
    val providerLauncher = remember(context) { AndroidProviderLauncher(context) }
    val providers = remember { SampleCatalogue.providers }
    val subscriptions = remember {
        mutableStateListOf(*SampleCatalogue.subscriptions.toTypedArray())
    }
    val rows by remember {
        derivedStateOf {
            rowBuilder.invoke(
                catalogue = SampleCatalogue.titles,
                subscriptions = subscriptions,
                recentLaunches = SampleCatalogue.recentLaunches,
            )
        }
    }

    var destination by remember { mutableStateOf(Destination.Home) }
    var selectedTitle by remember { mutableStateOf<Title?>(null) }

    LaunchedEffect(rows) {
        if (selectedTitle == null || rows.none { row -> row.titles.any { it.id == selectedTitle?.id } }) {
            selectedTitle = rows.firstOrNull { it.titles.isNotEmpty() }?.titles?.firstOrNull()
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
                onSelect = { destination = it },
            )
            MainSurface(
                destination = destination,
                rows = rows,
                appConfig = appConfig,
                providers = providers,
                subscriptions = subscriptions,
                selectedTitle = selectedTitle,
                onTitleSelected = { selectedTitle = it },
                onLaunchTargetSelected = { target ->
                    when (providerLauncher.launch(target)) {
                        ProviderLaunchResult.Launched -> Unit
                        ProviderLaunchResult.MissingTarget,
                        ProviderLaunchResult.MissingUri -> Toast.makeText(
                            context,
                            "Launch target pending",
                            Toast.LENGTH_SHORT,
                        ).show()

                        ProviderLaunchResult.ActivityUnavailable ->
                            Toast.makeText(context, "No app can open this target", Toast.LENGTH_SHORT).show()
                    }
                },
                onSubscriptionToggle = { providerId ->
                    val index = subscriptions.indexOfFirst { it.providerId == providerId }
                    if (index >= 0) {
                        val existing = subscriptions[index]
                        subscriptions[index] = existing.copy(enabled = !existing.enabled)
                    }
                },
            )
        }
    }
}

@Composable
private fun DestinationRail(
    selected: Destination,
    onSelect: (Destination) -> Unit,
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
            text = "CouchIndex",
            style = TextStyle(
                color = Color(0xFFF4F1E8),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        BasicText(
            text = "personal TV index",
            style = TextStyle(color = Color(0xFF8FA0A5), fontSize = 12.sp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Destination.entries.forEach { destination ->
            FocusButton(
                label = destination.label,
                selected = selected == destination,
                onClick = { onSelect(destination) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        BasicText(
            text = "DK catalogue sample",
            style = TextStyle(color = Color(0xFF6D7B80), fontSize = 12.sp),
        )
    }
}

@Composable
private fun MainSurface(
    destination: Destination,
    rows: List<BrowseRow>,
    appConfig: AppConfig,
    providers: List<Provider>,
    subscriptions: List<Subscription>,
    selectedTitle: Title?,
    onTitleSelected: (Title) -> Unit,
    onLaunchTargetSelected: (LaunchTarget?) -> Unit,
    onSubscriptionToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 34.dp, top = 32.dp, end = 34.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ScreenHeader(destination = destination, subscriptions = subscriptions, providers = providers)
            Spacer(modifier = Modifier.height(22.dp))
            when (destination) {
                Destination.Home -> HomeScreen(
                    rows = rows,
                    providers = providers,
                    onTitleSelected = onTitleSelected,
                )

                Destination.Browse -> BrowseScreen(
                    rows = rows,
                    providers = providers,
                    onTitleSelected = onTitleSelected,
                )

                Destination.Settings -> SettingsScreen(
                    appConfig = appConfig,
                    providers = providers,
                    subscriptions = subscriptions,
                    onSubscriptionToggle = onSubscriptionToggle,
                )
            }
        }
        DetailsPanel(
            title = selectedTitle,
            providers = providers,
            onLaunchTargetSelected = onLaunchTargetSelected,
        )
    }
}

@Composable
private fun ScreenHeader(
    destination: Destination,
    subscriptions: List<Subscription>,
    providers: List<Provider>,
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
        Column {
            BasicText(
                text = destination.label,
                style = TextStyle(
                    color = Color(0xFFF4F1E8),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = "Browse subscribed services first. Ratings stay separate.",
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 15.sp),
            )
        }
        BasicText(
            text = enabledProviders.ifBlank { "No providers enabled" },
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
    providers: List<Provider>,
    onTitleSelected: (Title) -> Unit,
) {
    val titles = rows
        .flatMap { it.titles }
        .distinctBy { it.id }
        .sortedWith(compareBy<Title> { it.mediaKind.name }.thenBy { it.name })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(titles, key = { "${it.id.mediaKind}-${it.id.tmdbId}" }) { title ->
            BrowseListItem(
                title = title,
                providers = providers,
                onTitleSelected = onTitleSelected,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    appConfig: AppConfig,
    providers: List<Provider>,
    subscriptions: List<Subscription>,
    onSubscriptionToggle: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText(
            text = "Subscriptions",
            style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        )
        providers.forEach { provider ->
            val enabled = subscriptions.firstOrNull { it.providerId == provider.id }?.enabled == true
            ProviderToggle(
                provider = provider,
                enabled = enabled,
                onClick = { onSubscriptionToggle(provider.id) },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        BasicText(
            text = "Integrations",
            style = TextStyle(color = Color(0xFFF4F1E8), fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        )
        IntegrationStatus(
            name = "TMDb",
            configured = appConfig.hasTmdbReadAccessToken,
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
    val background = posterColor(title)

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
                .background(background),
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
            BasicText(
                text = title.mediaKind.label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                style = TextStyle(color = Color(0xFF0E1114), fontSize = 11.sp, fontWeight = FontWeight.Bold),
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
        BasicText(
            text = title.ratings.firstOrNull()?.display() ?: "",
            style = TextStyle(color = Color(0xFFE8C468), fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun DetailsPanel(
    title: Title?,
    providers: List<Provider>,
    onLaunchTargetSelected: (LaunchTarget?) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(350.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE6111518))
            .border(1.dp, Color(0xFF2B363A), RoundedCornerShape(8.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(posterColor(title)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = title.name.take(2).uppercase(),
                style = TextStyle(color = Color(0xFF0E1114), fontSize = 42.sp, fontWeight = FontWeight.Black),
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
        BasicText(
            text = title.synopsis,
            style = TextStyle(color = Color(0xFFD4DADB), fontSize = 14.sp),
        )
        LabelBlock(label = "Available on", value = title.providerLabels(providers).joinToString(" / "))
        RatingStack(ratings = title.ratings)
        FocusButton(
            label = title.launchTargets.firstOrNull()?.label ?: "No launch target",
            selected = true,
            onClick = { onLaunchTargetSelected(title.launchTargets.firstOrNull()) },
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
private fun IntegrationStatus(
    name: String,
    configured: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151B1E))
            .border(1.dp, Color(0xFF273236), RoundedCornerShape(8.dp))
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
                text = if (configured) "Read token configured" else "Using sample catalogue",
                style = TextStyle(color = Color(0xFFA8B3B7), fontSize = 13.sp),
            )
        }
        BasicText(
            text = if (configured) "Ready" else "Local",
            style = TextStyle(
                color = if (configured) Color(0xFFE8C468) else Color(0xFF8FA0A5),
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
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        focused -> Color(0xFFE8C468)
        selected -> Color(0xFF263136)
        else -> Color.Transparent
    }
    val textColor = if (focused) Color(0xFF0E1114) else Color(0xFFF4F1E8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
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

private val MediaKind.label: String
    get() = when (this) {
        MediaKind.Movie -> "Movie"
        MediaKind.Series -> "Series"
    }

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
