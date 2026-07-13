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

- [x] Confirm `com.couchindex.app` as the permanent Play package identity
- [x] Replace the launcher banner with a Play-compliant 320x180 asset containing the app name
- [x] Define upload-key signing without committing credentials
- [x] Produce and validate a release Android App Bundle
- [x] Create the upload key and validate a signed release bundle
- [x] Back up the upload keystore and its Keychain password separately
- [x] Add modern backup and device-transfer exclusions for local personal state
- [x] Capture final Android TV store screenshots
- [x] Prepare Play icon, feature graphic and Android TV listing banner
- [ ] Validate provider handoffs on physical Google TV hardware
- [ ] Complete Play listing, privacy and data-safety declarations
- [x] Draft Play listing copy, privacy policy and data-flow evidence
- [x] Add a monitored release support email
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

## Sidequest milestones

These can proceed while Play developer verification is pending. Their detailed scope and
acceptance gates live in [the sidequest roadmap](sidequests.md).

### SQ1: Kids mode foundation

- [x] Adult and Kids application modes
- [x] One-action entry and PIN-protected exit
- [x] Start-in-Kids-mode setting without adult-content flash
- [x] Fail-closed certification eligibility across every catalogue surface
- [x] Separate Adult and Kids personal state

### SQ2: Parent catalogue controls

- [x] Hide a movie or whole series from Kids mode
- [x] PIN-protected blocked and allowed title management
- [x] Persistent canonical-title overrides with block-first precedence
- [x] Immediate removal from every Kids surface

### SQ3: Category browse and transparent sorting

- [ ] Movie and TV genre directory metadata
- [ ] Exact release or first-air date in titles and snapshots
- [ ] Media-kind and genre browse filters
- [ ] Newest, IMDb, TMDb and title sort modes
- [ ] D-pad position restoration across details navigation

### SQ4: Google TV startup presence

- [ ] Record boot and wake behavior on emulator and physical Google TV
- [ ] Confirm portable background-launch limitations
- [ ] Prototype a CouchIndex home-screen channel
- [ ] Compare launcher, channel and resume-last-app click counts
- [ ] Document portable, device-specific and unsupported behavior
