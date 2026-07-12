# UX guidelines

CouchIndex is designed for a television, remote control and viewing distance rather than touch or pointer input.

## Home hierarchy

1. Continue Watching
2. My Watchlist
3. New on My Services
4. Highly Rated
5. Movies
6. Series
7. Useful filtered rows such as Under Two Hours or Hidden Gems

## Interaction rules

- Focus must always be visible and predictable.
- Back navigation must never lose the previous browse position.
- Search spans every configured subscription.
- Posters provide recognition; focused cards reveal ratings, runtime and providers.
- Details pages expose explicit actions: Watch on Provider, Add to Watchlist, Mark Watched and Remove from Continue Watching.
- Marking a title watched removes it from Continue Watching without changing its watchlist membership.
- Marking a title unwatched does not add it back to Continue Watching without a new provider launch.
- Missing data is omitted rather than displayed as zero or an empty badge.

## Continue Watching

V1 is based on titles launched through CouchIndex plus manual correction. Exact provider playback position is optional because the provider app resumes playback itself.

## Performance target

Cached home rows should appear immediately. Network refreshes must not block navigation.
