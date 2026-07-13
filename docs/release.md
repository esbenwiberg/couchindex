# Release Preparation

Milestone 8 targets a Play internal-testing release that meets the Android TV Ready
requirements. TV Optimized features are worthwhile follow-up work, but are not allowed
to obscure the smaller submission contract.

## Readiness audit

Audited on 2026-07-13 against the current Android and Play documentation.

| Area | Current state | Release gate |
| --- | --- | --- |
| Package identity | `com.couchindex.app` | Confirm before the Play app is created; it becomes permanent after publication. |
| SDK compatibility | `minSdk 28`, `targetSdk 37` | Ready. Current TV submissions require target API 34 or newer and common-device support requires minimum API 31 or lower. |
| TV manifest | Leanback required, touchscreen optional, landscape launcher activity | Ready. Preserve the TV-only form-factor declarations. |
| Launcher assets | Scalable icon and 320x180 raster banner containing `CouchIndex` | Ready for emulator and physical-launcher review. |
| Release artifact | The validator builds and cryptographically verifies a signed 7.2 MB AAB; upload credentials are backed up separately | Ready for Play upload after developer verification. |
| Native compatibility | `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` are packaged | Retain 64-bit support and verify 16 KB page compatibility when native dependencies change. |
| Personal data | Subscriptions, feedback, history and catalogue cache remain on device and are excluded from backup and transfer | Ready. Revisit only if explicit synchronization is introduced. |
| Network data | TMDb catalogue requests and IMDb dataset downloads only | Document declarations and publish a privacy policy before closed or production testing. |
| Store media | Four curated 1920x1080 TV screenshots and a reproducible capture script exist | Ready for owner review and Play upload. |
| Device behavior | Google TV emulator validated | Verify launcher artwork and real provider handoffs on physical Google TV hardware. |

## Signing contract

Use Play App Signing. Google holds the app-signing key and the developer retains a
separate upload key. Keep the keystore and passwords outside the repository, backed up
in recoverable secure storage. The Gradle release configuration may read paths and
credentials from ignored local properties, but must still permit an unsigned local
bundle for deterministic CI validation.

Local release validation is automated by `scripts/validate-release.sh`. The default
mode accepts an unsigned artifact; `--require-signed` is mandatory before a Play upload.
Signing configuration uses the four `COUCHINDEX_UPLOAD_*` values documented in
`docs/development.md` and fails configuration when only a partial set is supplied.
On macOS, `scripts/create-upload-key.sh` creates the default upload key and stores its
generated password in Keychain; signed validation loads it without writing the secret
to the repository or build logs.

The current upload certificate uses alias `couchindex-upload`, expires on 2053-11-28,
and has SHA-256 fingerprint:

```text
A6:07:64:59:FE:41:DC:A0:99:E6:7B:C4:B1:DD:39:B2:B3:49:EB:DB:E1:15:A0:16:11:F3:2F:24:B4:AE:27:6C
```

The fingerprint is public release metadata. The keystore and Keychain password are the
private recovery materials and must never be committed.

The TMDb API read token is application-level authentication and is compiled into the
client artifact. It must remain read-only; no TMDb user token or other account secret
may be embedded. Build logs, documentation and committed configuration must never
contain the token.

## Play declarations

Before moving beyond internal testing:

- Publish a privacy policy describing local personal state, TMDb requests, IMDb dataset
  downloads, artwork loading and provider handoff.
- Complete the Data safety form from the behavior of the final artifact and its SDKs.
- Retain visible JustWatch attribution wherever live watch-provider availability appears.
- Supply an Android TV banner and at least one accurate Android TV screenshot.
- Re-check target API, testing-track and developer-account requirements immediately
  before submission because these policies change independently of the codebase.

## First tracks

- Use internal testing for personal installation and physical-device validation.
- Use closed testing when inviting external testers.
- Treat production access as a separate gate after internal stability and any current
  Play Console testing requirement are satisfied.

## Current references

- Android TV app quality:
  https://developer.android.com/docs/quality-guidelines/tv-app-quality
- Target API level requirements:
  https://support.google.com/googleplay/android-developer/answer/11926878
- Android App Bundles:
  https://support.google.com/googleplay/android-developer/answer/9844279
- Play App Signing:
  https://developer.android.com/studio/publish/app-signing
- Store preview assets:
  https://support.google.com/googleplay/android-developer/answer/9866151
- Data safety:
  https://support.google.com/googleplay/android-developer/answer/10787469
- TMDb application authentication:
  https://developer.themoviedb.org/docs/authentication-application
- TMDb watch-provider attribution:
  https://developer.themoviedb.org/reference/movie-watch-providers
