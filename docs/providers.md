# Provider strategy

## Catalogue

TMDb is the initial canonical catalogue and identity source. Denmark is the default watch region. Provider availability is normalized into CouchIndex offers and filtered by configured subscriptions.

The provider list is discovered from TMDb's regional movie and TV provider directories. Netflix, Disney+, Max and Viaplay are offline starter defaults, not an allowlist. Additional Danish providers receive stable local IDs based on their TMDb provider identity and are disabled until the user selects them.

TMDb watch-provider availability is supplied by JustWatch. CouchIndex attributes JustWatch anywhere live availability status is presented, as required by the TMDb watch-provider API terms.

## Ratings

IMDb and TMDb are initial rating sources. Rotten Tomatoes or other sources may be added through replaceable adapters when access and usage terms are suitable.

Every rating retains:

- source
- value and scale
- vote count when available
- scope: title, season or episode
- retrieval timestamp

`Highly Rated` requires a normalized score of at least 7/10 backed by at least 100 votes, then ranks by score and logarithmic vote confidence. `Hidden Gems` uses the same score floor with 100 to 5,000 votes, while retaining explicit editorial flags for offline sample data. These are internal browse rules, never presented as an external consensus score.

## Playback handoff

CouchIndex does not retrieve or play protected streams. A launch resolver selects the best available destination for the device:

1. Installed provider TV application
2. Title-specific TMDb/JustWatch watch page
3. Google Play install page for a known provider application

TMDb does not return universal provider deep links. Opening an installed provider currently hands off to its application home; exact title or episode links remain provider-specific enhancements where documented support exists. Opening a title watch page is recorded as a recent launch, while opening an install page is not.

The provider application handles sign-in, DRM, playback, subtitles, casting and exact resume position.

## Reliability

Availability and launchability are different facts. A provider may offer a title without exposing a reliable deep link. These must remain separate in the data model and UI.
