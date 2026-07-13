# Sidequest roadmap

These milestones can move while Play developer verification is pending. They remain
separate from Milestone 8 so release work stays frozen and reviewable.

## Product decisions

- Kids mode is a parental boundary, not merely a visual theme.
- Entering Kids mode is immediate. Leaving it and changing its catalogue require the
  local parent PIN once parental controls are configured.
- Kids mode fails closed: a title with unknown eligibility is hidden until a parent
  explicitly allows it.
- A parent block always wins over certification, genre or a previous allow override.
- Adult and Kids watchlists, recent launches, watched state and feedback do not mix.
- Provider apps own playback after handoff. CouchIndex cannot enforce restrictions
  inside Netflix, Disney+, Max or another provider app.
- Category sorting names its rating source. CouchIndex never presents a blended score.

## SQ1: Kids mode foundation

Deliver a mode that can be safely selected before showing an adult catalogue.

### Scope

- Add Adult and Kids application modes with an unmistakable but restrained mode label.
- Add a one-action `Switch to Kids` command available from the adult destination rail.
- Add local parent PIN setup and a PIN gate for leaving Kids mode or opening parental
  settings. Clearing app data is the recovery path for a forgotten PIN in the first
  version.
- Add a `Start in Kids mode` setting. Apply it before selecting or rendering a title on
  cold launch so adult artwork never flashes first.
- Keep personal state profile-scoped, including watchlist, recent launches, watched
  state and Like or Dislike feedback.
- Enrich titles with regional certification evidence. TMDb exposes movie certification
  through release dates and TV certification through content ratings.
- Define a single core eligibility use case used by Home, Browse, Search, details and
  recommendations. Unknown or over-threshold titles are excluded.
- Persist certification evidence in a versioned catalogue snapshot and retain the last
  valid evidence offline.

### Acceptance gates

- A child can enter Kids mode with one remote action and cannot leave it without the
  configured PIN.
- Cold launch into Kids mode never renders an adult title, including while the catalogue
  refreshes or when only a stale snapshot is available.
- Search, cached rows and deep links cannot bypass the eligibility filter.
- Missing certification is treated as unknown and hidden, with no inference from genre.
- Kids activity does not change Adult recommendations or Continue Watching.
- Emulator tests cover launch mode, PIN boundaries, profile isolation, stale cache and
  unknown certification behavior.

## SQ2: Parent catalogue controls

Give the parent final say when external certification data is incomplete or a
technically age-eligible title is unwelcome.

### Scope

- Add `Hide from Kids` to adult title details for movies and whole series.
- Add a PIN-protected parental-controls screen listing hidden and manually allowed
  titles, with search, remove and restore actions.
- Persist overrides by canonical `TitleId`; catalogue refreshes must not lose them.
- Model overrides explicitly as `Allowed` or `Blocked`. A block has highest precedence;
  an allow may admit an otherwise unknown title but never an over-threshold title unless
  the parent confirms that exception explicitly.
- Hide blocked titles from every Kids surface, including search results, recommendations,
  watchlist and recent launches.
- Remove a newly blocked title from Kids Continue Watching without changing Adult state.

### Acceptance gates

- Blocking a visible title removes it from all Kids surfaces immediately and offline.
- A blocked title cannot be opened through a stale card or restored navigation state.
- Unblocking is PIN protected and restores the title only if another eligibility rule
  allows it.
- Series-level blocks cover the complete series identity; episode-level overrides remain
  out of scope until CouchIndex owns episode catalogue data.
- Tests prove override precedence, persistence, profile isolation and stale-state safety.

## SQ3: Category browse and transparent sorting

Turn Browse into a filterable catalogue without adding network work to each interaction.

### Scope

- Fetch the official TMDb movie and TV genre directories and map existing `genreIds` to
  display names.
- Retain exact movie release date or TV first-air date in the `Title` model and versioned
  snapshot. The current model stores only the year.
- Add media controls for All, Movies and Series plus a genre selector.
- Add sort choices for Newest, IMDb rating, TMDb rating and Title. Missing values appear
  last; rating ties use vote support and then title name.
- Filter the already loaded subscription catalogue locally. Changing category or sort
  must not trigger a TMDb request.
- Preserve selected category, sort order and list position when opening details and
  returning with Back.
- Apply the active Adult or Kids eligibility boundary before category filtering.

### Acceptance gates

- Genre labels work for both movies and series and survive offline startup.
- Newest uses full dates where available rather than comparing only release years.
- IMDb and TMDb ordering remain distinct and visibly named.
- Empty categories explain the result without moving focus unpredictably.
- D-pad validation covers long genre names, every sort mode and restored browse position.

## SQ4: Google TV startup presence

Investigate the shortest supported path from TV startup to CouchIndex without promising
an automatic foreground launch that Android may block.

### Current finding

An ordinary Play-distributed app can receive `BOOT_COMPLETED` for initialization, subject
to background restrictions, but Android has restricted background activity launches
since Android 10. A boot receiver is therefore not a reliable or appropriate way to force
CouchIndex over the Google TV home screen. Becoming the device Home application would be
a different product with much larger ownership and recovery risks.

Android TV does support app channels and programs on the system home screen. A default
channel can appear automatically, while additional channels require user approval. This
is the supported direction for reducing startup clicks.

### Investigation scope

- Verify background-activity behavior on the project emulator and physical Google TV
  hardware across cold boot, wake and restart.
- Check whether the target Google TV exposes an owner-controlled startup or resume-last-
  app setting; document it as device configuration, not an app guarantee.
- Prototype a CouchIndex home-screen channel using `TvProvider`, with useful local rows
  and app-link intents back into CouchIndex.
- Measure startup-to-CouchIndex remote clicks with launcher icon, default channel and
  resume-last-app behavior.
- Do not ship `RECEIVE_BOOT_COMPLETED`, a foreground service or a default-launcher role
  unless the investigation proves a required, policy-compliant use beyond opening UI.

### Acceptance gates

- The investigation records results from at least one physical Google TV device.
- Any shipped integration follows current background-launch and TV quality guidance.
- Selecting a home-screen CouchIndex card opens the matching title or useful browse row.
- Unavailable or stale catalogue data does not leave broken cards on the TV home screen.
- The final recommendation states what is portable, what is device-specific and what is
  deliberately unsupported.

## Suggested order

1. SQ1 and SQ2 together form the first shippable Kids mode release.
2. SQ3 is independent and can be built while the Kids catalogue model is reviewed.
3. SQ4 research can start immediately, but its physical-device gate remains external.

## Primary references

- Android background activity launch restrictions:
  https://developer.android.com/guide/components/activities/secure-bal
- Android boot broadcast contract:
  https://developer.android.com/reference/android/content/Intent#ACTION_BOOT_COMPLETED
- Android TV home-screen channels:
  https://developer.android.com/training/tv/discovery/recommendations-channel
- TMDb movie discovery and sort/filter fields:
  https://developer.themoviedb.org/reference/discover-movie
- TMDb movie genres:
  https://developer.themoviedb.org/reference/genre-movie-list
- TMDb TV genres:
  https://developer.themoviedb.org/reference/genre-tv-list
- TMDb movie release dates and certifications:
  https://developer.themoviedb.org/reference/movie-release-dates
- TMDb TV content ratings:
  https://developer.themoviedb.org/reference/tv-series-content-ratings
