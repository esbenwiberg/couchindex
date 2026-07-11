# Provider strategy

## Catalogue

TMDb is the initial canonical catalogue and identity source. Denmark is the default watch region. Provider availability is normalized into CouchIndex offers and filtered by configured subscriptions.

## Ratings

IMDb and TMDb are initial rating sources. Rotten Tomatoes or other sources may be added through replaceable adapters when access and usage terms are suitable.

Every rating retains:

- source
- value and scale
- vote count when available
- scope: title, season or episode
- retrieval timestamp

## Playback handoff

CouchIndex does not retrieve or play protected streams. A launch resolver selects the best available destination for the device:

1. Exact episode deep link
2. Title or series deep link
3. Provider search or catalogue link
4. Web fallback

The provider application handles sign-in, DRM, playback, subtitles, casting and exact resume position.

## Reliability

Availability and launchability are different facts. A provider may offer a title without exposing a reliable deep link. These must remain separate in the data model and UI.