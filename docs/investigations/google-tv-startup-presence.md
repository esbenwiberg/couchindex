# Google TV startup presence

Investigation date: 2026-07-13

## Recommendation

Ship the CouchIndex default preview channel and canonical title app links. Do not add a
boot receiver, foreground service, or Home-app role to force CouchIndex into the
foreground. Treat resume-last-app behavior and power-on settings as device-specific
conveniences that must be verified on each physical TV.

## Implemented prototype

- AndroidX TV Provider `1.1.0` publishes one default `CouchIndex` channel.
- The channel is derived from at most 12 useful titles in the last valid atomic
  catalogue snapshot. A live refresh replaces the old programs.
- `android.media.tv.action.INITIALIZE_PROGRAMS` initializes from the local snapshot after
  installation. CouchIndex does not request `RECEIVE_BOOT_COMPLETED`.
- A missing snapshot clears CouchIndex-owned programs rather than leaving dead cards.
- Program links use `couchindex://title/{movie|series}/{tmdbId}`. The app resolves the
  canonical title through the active catalogue boundary before selecting it.
- Entering Kids mode republishes the channel from the Kids-eligible catalogue. A stale
  adult card still cannot open a blocked or otherwise hidden title in Kids mode.

## Emulator results

Test target: `CouchIndex_Google_TV`, Google TV LauncherX, build
`BT2A.260319.001`, 1920x1080.

| Scenario | Observed result |
| --- | --- |
| Cold device boot | Boot count advanced to 6. LauncherX Home owned focus and no CouchIndex process existed. |
| Wake after CouchIndex was foreground | `MainActivity` regained focus without a remote selection. |
| Default channel publication | Channel ID 4 was retained and 12 preview programs were published. |
| Catalogue unavailable | The initialization receiver reduced the published program receipt from 12 to 0. |
| Catalogue restored | The same channel republished 12 new program IDs. |
| Valid card link | `couchindex://title/movie/157336` selected Interstellar in the running single activity. |
| Kids blocked card link | `couchindex://title/movie/122` was ignored; the blocked title did not replace the visible Kids title. |

The emulator was not signed into a Google account. LauncherX therefore stopped at its
Google TV setup screen instead of rendering the personalized Home rows. TvProvider
publication, reconciliation, intent resolution, process state, and app screenshots were
validated, but the final channel artwork and row placement still require a configured
physical Google TV.

## Click comparison

The comparable number is the minimum activation selections after the destination is in
focus. D-pad travel varies with launcher personalization and cannot be promised.

| Path | Minimum activation selections | Portability |
| --- | ---: | --- |
| Resume with CouchIndex already foreground | 0 | Observed on the emulator; device and power-mode specific. |
| CouchIndex default-channel card | 1 | Portable API where the installed TV launcher surfaces preview channels. |
| CouchIndex launcher icon | 1 | Portable Leanback launcher contract; row position is device specific. |
| Forced app launch at boot | 0 | Deliberately unsupported and not implemented. |

## Support boundary

Portable:

- The Leanback launcher entry, one default preview channel, local card reconciliation,
  and canonical title app links.
- Android background-activity launch restrictions. Since Android 10, an ordinary
  background app cannot rely on starting an activity over the launcher.

Device-specific:

- Whether a Google TV launcher displays legacy preview channels, where it places them,
  and whether account setup or user approval affects visibility.
- Whether sleep and wake restore the last foreground app.
- Manufacturer settings such as power-on behavior, last used input, a favorite-app
  remote button, or an owner-controlled startup-app feature. These are configuration
  options, not CouchIndex guarantees.

Unsupported:

- Starting `MainActivity` from `BOOT_COMPLETED`.
- Keeping a foreground service solely to force open the CouchIndex UI.
- Claiming the system Home role or replacing the Google TV launcher.

## Physical TV gate

On the target Google TV device:

1. Install the internal-testing build and complete normal Google account setup.
2. Confirm the CouchIndex row is visible, has the expected logo and artwork, and opens
   the matching title.
3. Count D-pad presses and Select presses from cold boot to the launcher icon and channel
   card.
4. Put CouchIndex in the foreground, sleep and wake the TV, and record whether it resumes.
5. Restart the TV and confirm Google TV Home remains foreground.
6. Inspect `Settings > System > Power & Energy` and manufacturer settings for power-on
   or last-input behavior. Record the model and firmware because these options vary.

## Primary references

- Android background activity launch restrictions:
  https://developer.android.com/guide/components/activities/secure-bal
- Android TV home-screen channels:
  https://developer.android.com/training/tv/discovery/recommendations-channel
- AndroidX TV Provider releases:
  https://developer.android.com/jetpack/androidx/releases/tvprovider
- `ACTION_INITIALIZE_PROGRAMS` API contract:
  https://developer.android.com/reference/androidx/tvprovider/media/tv/TvContractCompat#ACTION_INITIALIZE_PROGRAMS
- Google TV setup requirements:
  https://support.google.com/googletv/answer/10050221
