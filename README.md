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
  Google TV UI and Android integration
core/
  Domain model and use cases
infrastructure/
  Catalogue APIs, ratings, persistence and provider launching
```

The app is local-first. A backend is intentionally excluded from V1.

## Data strategy

- TMDb: canonical catalogue identity, metadata, artwork and Danish provider availability
- IMDb datasets: rating and vote count
- Additional rating adapter: Rotten Tomatoes and other sources where legally and technically available
- Local database: subscriptions, watchlist, recent launches and Continue Watching state

External scores are never blended into a fake universal rating. Vote count is retained so browse ranking can avoid promoting titles with tiny samples.

## First technical milestone

```text
Browse TMDb titles available in Denmark
→ show poster, title, rating and provider
→ open the selected provider app on Google TV
```

## Local configuration

API credentials must be supplied through uncommitted local configuration. Do not commit secrets.

## Status

Initial scaffold. The dependency versions in the first Android project commit should be verified against the installed Android Studio version before implementation begins.
