# Domain model

## Core concepts

### Title
A discoverable movie or series with a canonical TMDb identity.

### Season and episode
Children of a series. Episode-level identity is optional in early V1 flows but the model must not prevent it.

### Provider
A streaming service such as Netflix, Disney+, Max or Viaplay.

### Offer
A title's availability from a provider in a region and monetization type, such as subscription, rental or purchase.

### Launch target
A platform-specific destination used to open a title, series or episode in a provider application or web fallback.

### Rating
A score from a named source with its own scale, scope, vote count and retrieval timestamp.

### Subscription
A provider enabled by the user. Browse excludes unrelated providers by default.

### Watch state
CouchIndex-owned state describing recent launch, watchlist membership, completion or the next known episode.

## Important invariants

- External ratings are never silently combined.
- An offer is region-specific.
- A launch target may be absent or less specific than an offer.
- Missing playback progress must not be represented as zero.
- Provider playback state is not assumed to be observable.