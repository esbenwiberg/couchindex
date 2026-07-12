# Release Preparation

The Play Console account is ready to use once the local product loop is stable enough
for screenshots and tester builds.

## Before creating the Play app

- Confirm the package name. `com.couchindex.app` becomes the permanent app identity
  once published.
- Create a release signing plan. New apps should use Play App Signing and keep the
  upload key recoverable.
- Build release artifacts as Android App Bundles (`.aab`), not debug APKs.
- Prepare TV screenshots from the Google TV emulator.
- Prepare privacy, data safety, store listing and app access answers.

## First tracks

- Use internal testing for fast personal/device validation.
- Use closed testing when inviting external testers.
- Re-check current Play Console requirements before production access. Personal
  developer accounts may need a closed test with a minimum tester count and continuous
  opt-in duration before production is available.

## Useful references

- Create and set up a Play Console app:
  https://support.google.com/googleplay/android-developer/answer/9859152
- Play App Signing:
  https://support.google.com/googleplay/android-developer/answer/9842756
- Personal developer account testing requirements:
  https://support.google.com/googleplay/android-developer/answer/14151465
