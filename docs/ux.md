# UX guidelines

CouchIndex is designed for a television, remote control and viewing distance rather than touch or pointer input.

## Home hierarchy

1. Continue Watching
2. My Watchlist
3. For You, when explicit feedback produces recommendations
4. New on My Services
5. Highly Rated
6. Movies
7. Series
8. Useful filtered rows such as Under Two Hours or Hidden Gems

## Interaction rules

- Focus must always be visible and predictable.
- Back navigation must never lose the previous browse position.
- Search spans every configured subscription.
- Posters provide recognition; focused cards reveal ratings, runtime and providers.
- Details pages expose explicit actions: Watch on Provider, Add to Watchlist, Mark Watched, Like or Dislike, and Remove from Continue Watching.
- Selecting the active Like or Dislike choice clears that feedback.
- Marking a title watched removes it from Continue Watching without changing its watchlist membership.
- Marking a title unwatched does not add it back to Continue Watching without a new provider launch.
- Missing data is omitted rather than displayed as zero or an empty badge.
- Cached and stale catalogue states remain navigable and disclose cache age in Settings.

## Continue Watching

V1 is based on titles launched through CouchIndex plus manual correction. Exact provider playback position is optional because the provider app resumes playback itself.

## Performance target

Cached home rows should appear immediately. Network refreshes must not block navigation.
