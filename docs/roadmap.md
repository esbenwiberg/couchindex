# Roadmap

## Milestone 1: runnable TV shell

- [x] Android TV project
- [x] Compose for TV shell
- [x] Home, Browse and Settings destinations
- [x] Remote focus behavior baseline
- [x] Verified build on a local Android toolchain

## Milestone 2: catalogue vertical slice

- [x] TMDb authentication
- [x] Dynamic Danish provider directory
- [x] Danish availability discovery
- [x] Subscription configuration with local persistence
- [x] Browse rows and title details
- [x] Provider launch handoff boundary
- [x] Provider app, title watch-page and install fallback resolution

## Milestone 3: personal state

- [x] Local watchlist
- [x] Persistent recent launches
- [x] Continue Watching derived from recent launches
- [x] Manual Continue Watching removal
- [x] Manual watched state

## Milestone 4: rating enrichment

- [x] IMDb rating import
- [x] TMDb vote counts
- [x] Replaceable rating adapter pipeline
- [x] Vote-aware quality browse filters

## Milestone 5: catalogue experience

- [x] Real TMDb artwork with cached loading and resilient fallbacks
- [x] Runtime metadata
- [x] Cross-provider search

## Milestone 6: private recommendations

- [x] TMDb genre metadata without additional requests
- [x] Persistent local Like and Dislike feedback
- [x] Feedback-driven For You row across enabled subscriptions
- [x] Disliked-genre suppression and reversible feedback

## Milestone 7: offline reliability

- [x] Atomic, versioned provider and catalogue snapshots
- [x] Immediate cached startup with background refresh
- [x] Cached catalogue retention after refresh failure
- [x] Cache age with explicit Cached and Stale states
- [x] Corrupt, unsupported and stale snapshot tests

## Milestone 8: release readiness

- [ ] Confirm `com.couchindex.app` as the permanent Play package identity
- [ ] Replace the launcher banner with a Play-compliant 320x180 asset containing the app name
- [ ] Define upload-key signing without committing credentials
- [ ] Produce and validate a release Android App Bundle
- [ ] Add modern backup and device-transfer exclusions for local personal state
- [ ] Capture final Android TV store screenshots
- [ ] Validate provider handoffs on physical Google TV hardware
- [ ] Complete Play listing, privacy and data-safety declarations
- [ ] Publish and install an internal-testing build

## Later investigations

- [x] Media-session playback observation feasibility
- [x] Exact title and episode deep-link feasibility
- [x] Usage-based completion heuristic feasibility
- [x] Phone companion architecture boundary
- [x] Trakt and Letterboxd integration requirements
- [x] Additional rating-source requirements
- [x] Recommendations based on personal feedback

See [the post-V1 integration investigation](investigations/post-v1-integrations.md) for decisions and remaining gates.
