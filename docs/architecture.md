# Architecture

## V1 shape

CouchIndex is a native Android/Google TV application written in Kotlin with Compose for TV.

```text
Compose for TV UI
        ↓
Application use cases
        ↓
Domain model
        ↓
Repositories and adapters
   ↙        ↓        ↘
Room      TMDb      Rating sources
        
Provider launcher → installed streaming app
```

## Modules

- `app`: Android entry point, Compose UI, navigation, focus handling and platform integrations.
- `core`: domain types, repository contracts and use cases without Android dependencies where practical.
- `infrastructure`: catalogue clients, persistence, rating adapters and provider launch resolution. This may begin inside `app` and be extracted only when useful.

Rating adapters enrich canonical titles independently. Ratings replace older values only for the same source and scope; adapter failures do not block the base catalogue.

## Data ownership

Local storage owns subscriptions, watchlist entries, watched history, recent launches and CouchIndex Continue Watching state. External APIs own catalogue metadata, availability and third-party ratings.

## Runtime behavior

Browse and search prefer locally cached data. Background refreshes update metadata and availability. Selecting a provider resolves a launch target and hands control to the installed provider app.

## Security

API keys are supplied through uncommitted local configuration. CouchIndex does not store streaming-provider credentials.
