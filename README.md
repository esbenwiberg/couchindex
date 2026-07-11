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
- Additional rating adapter: Rotten Tomatoes and other sources where legally and technically available
- Local database: subscriptions, watchlist, recent launches and Continue Watching state

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

## Status

Milestone 1 scaffold in progress. The repo now contains a native Android TV app module, an Android-free
core module, placeholder browse data and a Compose shell with Home, Browse and Settings destinations.
