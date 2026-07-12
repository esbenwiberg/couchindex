# CouchIndex

A personal Google TV app for browsing everything available across your streaming subscriptions without the promotional clutter.

## Product principles

1. Continue Watching is always first.
2. Browse the combined catalogue, not one provider at a time.
3. Show only services you subscribe to by default.
4. Keep ratings transparent and separate by source.
5. Hand playback to the installed provider app; CouchIndex does not proxy video.
6. Prefer local-first behavior and fast TV navigation.

## V1

The first version focuses on a thin vertical slice:

- Google TV app shell built with Kotlin and Compose for TV
- Browse movies and series available in Denmark
- Filter availability by configured subscriptions
- Show title metadata, provider availability and external ratings
- Launch the selected title in the provider app
- Maintain a local watchlist and recent launches
- Use recent launches as the first Continue Watching implementation

## Planned browse rows

- Continue Watching
- My Watchlist
- New on My Services
- Highly Rated
- Movies
- Series
- Under Two Hours
- Hidden Gems

## Architecture

```text
app/
  Android TV UI, Compose shell and platform integration
core/
  Domain model, sample catalogue and browse row use cases
infrastructure/
  Catalogue APIs, ratings, persistence and provider launching (added when useful)
```

The app is local-first. A backend is intentionally excluded from V1.

## Data strategy

- TMDb: canonical catalogue identity, metadata, artwork and Danish provider availability
- IMDb datasets: rating and vote count
- Future rating adapters: Rotten Tomatoes and other sources where legally and technically available
- Local database: subscriptions, watchlist, watched history, recent launches and Continue Watching state

External scores are never blended into a fake universal rating. Vote count is retained so browse ranking
can avoid promoting titles with tiny samples.

## First technical milestone

```text
Browse TMDb titles available in Denmark
→ show poster, title, rating and provider
→ open the selected provider app on Google TV
```

## Local configuration

API credentials must be supplied through uncommitted local configuration. Do not commit secrets.

For TMDb, add a read access token to `local.properties`:

```properties
TMDB_READ_ACCESS_TOKEN=your_token_here
```

## Development

The Android scaffold is pinned to the current Android Gradle plugin family documented for API 37:

- JDK 17 or newer
- Android SDK API 37
- Android SDK Build Tools 36.0.0
- Gradle 9.4.1 or an Android Studio version that can import AGP 9.2.0 projects

Once the local Android toolchain is installed, build from Android Studio or from the repository root:

```sh
./gradlew :app:assembleDebug
./gradlew :core:test
```

For emulator setup and TV smoke checks, see [docs/development.md](docs/development.md).
For Google Play preparation, see [docs/release.md](docs/release.md).

## Status

Milestone 1 is complete. Milestone 2 now discovers the Danish provider directory and subscription
catalogues from TMDb when a token is configured, merges per-provider availability by canonical
title identity, and falls back to four starter providers plus sample titles when configuration or
connectivity is unavailable. Provider launches now resolve to an installed TV app, a title watch
page or an install page, and successful content handoffs feed persistent Continue Watching state.
Milestone 3 now also includes a persistent local watchlist surfaced directly after Continue Watching,
manual removal from Continue Watching and independent watched history.
Milestone 4 now uses source vote counts to keep tiny samples out of Highly Rated and to discover Hidden Gems on the live catalogue.
TMDb titles also retain explicit IMDb and Wikidata identifiers for source-safe rating enrichment.
Milestone 4 is complete with daily cached IMDb imports, separate TMDb and IMDb vote counts, and vote-aware quality rows.
Milestone 5 now includes real TMDb poster artwork across Home, Browse and Details, cached locally with generated fallbacks,
plus movie and series runtime metadata loaded during the existing title-details enrichment pass.
