# Post-V1 integration investigation

Investigated on 2026-07-12 against the current Android and service documentation.

## Decisions

| Area | Finding | Decision |
| --- | --- | --- |
| Media-session playback observation | Android exposes other apps' active media sessions only to privileged media controllers or an enabled notification-listener service. Providers also choose whether their sessions expose useful metadata and position. | Do not require notification access. Consider a clearly optional prototype after testing real provider apps on physical Google TV hardware. |
| Exact title and episode deep links | Android can launch explicit or implicit intents, but streaming providers do not publish one stable cross-provider title/episode URI contract. Availability and launchability remain separate. | Add provider-specific resolvers only when an official contract can be documented and retain the existing app-home/watch-page fallback. |
| Usage-based completion heuristics | `UsageStatsManager` requires Usage Access granted in system Settings. It reports package activity, not verified playback, and event history is retained only briefly. | Do not auto-mark titles watched from app dwell time. Keep explicit watched state and Continue Watching correction. |
| Phone companion | Cross-device state requires a pairing, identity and synchronization protocol. The current local-only architecture intentionally has no backend. | Treat this as a separate architecture milestone, not an extension of the TV process. |
| Trakt integration | Trakt supports OAuth 2.0 Device Code Flow for TVs and can synchronize user state, but CouchIndex needs a registered API client and product decisions for conflict resolution. | Preferred first cloud integration after credentials and sync semantics are agreed. |
| Letterboxd integration | Letterboxd supports third-party OAuth Authorization Code Flow and TMDb film IDs, but requires registered client credentials and is film-focused. | Consider after Trakt; do not embed credentials or fake an integration locally. |
| Additional rating source | No additional source currently meets the combined access, attribution, redistribution and update requirements better than the existing TMDb and official IMDb dataset adapters. | Keep the adapter boundary and add a source only after its terms and access are verified. |
| Recommendations from feedback | TMDb discover results already contain genre IDs. Explicit local Like/Dislike feedback can produce useful, explainable ranking without an account or another request. | Implement locally as Milestone 6. |

## Platform references

- [Android MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager)
- [Android UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Android package visibility use cases](https://developer.android.com/training/package-visibility/use-cases)
- [Trakt OAuth authentication](https://docs.trakt.tv/docs/authentication-oauth)
- [Letterboxd API authentication](https://api-docs.letterboxd.com/)
- [TMDb movie details and append-to-response](https://developer.themoviedb.org/reference/movie-details)

## Safety boundary

CouchIndex must not infer that a title was completed merely because a provider app stayed foregrounded. It must not request notification or usage access until a concrete user-facing benefit is proven on physical TV hardware. Provider credentials, viewing traffic and DRM data remain outside CouchIndex.
