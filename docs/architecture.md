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

Local storage owns subscriptions, watchlist entries, watched history, recent launches, explicit title feedback,
CouchIndex Continue Watching state and recommendation inputs. External APIs own catalogue metadata,
availability and third-party ratings.

## Runtime behavior

Browse and search initialize from the last valid provider and catalogue snapshot. Background refreshes update
metadata and availability without clearing cached rows. A successful refresh atomically replaces the versioned
snapshot. A failed refresh retains cached data and exposes its age; snapshots become stale after 24 hours.
Sample data is used only when no valid snapshot exists. Selecting a provider resolves a launch target and hands
control to the installed provider app.

The Android TV Provider contains only a derived discovery channel. CouchIndex rebuilds its
programs from the local catalogue snapshot and removes them when that snapshot is unavailable.
Preview-program app links resolve canonical `TitleId` values through the current Adult or Kids
catalogue boundary; the TV Provider never becomes catalogue or parental-control state.

## Security

API keys are supplied through uncommitted local configuration. CouchIndex does not store streaming-provider credentials.
