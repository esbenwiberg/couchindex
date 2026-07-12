# Domain model

## Core concepts

### Title
A discoverable movie or series with a canonical TMDb identity.

### Season and episode
Children of a series. Episode-level identity is optional in early V1 flows but the model must not prevent it.

### Provider
A streaming service such as Netflix, Disney+, Max or Viaplay.

Providers retain their external TMDb provider identity. The app discovers the regional directory dynamically; built-in providers exist only as offline defaults.

### Offer
A title's availability from a provider in a region and monetization type, such as subscription, rental or purchase.

### Launch target
A platform-specific destination used to open a title, series or episode in a provider application or web fallback.

### Rating
A score from a named source with its own scale, scope, vote count and retrieval timestamp.

Titles retain explicit external identifiers so source adapters can join ratings without fuzzy title matching.

### Subscription
A provider enabled by the user. Browse excludes unrelated providers by default.

### Watchlist entry
A locally persisted title identity and the time it was added. The newest saved title appears first in My Watchlist.

### Watched entry
A locally persisted title identity and the time it was marked watched. Watched history is independent from watchlist membership.

### Feedback entry
A locally persisted Like or Dislike for one canonical title. Selecting the active value again clears it.
Feedback is an input to CouchIndex recommendations and is not sent to a rating source.

### Watch state
CouchIndex-owned state describing recent launch, watchlist membership, completion or the next known episode.

## Important invariants

- External ratings are never silently combined.
- An offer is region-specific.
- A launch target may be absent or less specific than an offer.
- Missing playback progress must not be represented as zero.
- Provider playback state is not assumed to be observable.
- Recommendations never imply an external rating or provider endorsement.
