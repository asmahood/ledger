# Ledger

A personal Android budgeting app. Tracks income and expenses by category, sets monthly
budget targets, and charts spending over time. Built to replace a Google Sheet.

Single-user and distributed as a sideloaded APK — it is not on Google Play.

## Requirements

- JDK 17
- Android SDK, `minSdk 26` / `targetSdk 36`
- A physical device for instrumented tests

## Build

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (unsigned unless keystore env vars are set)
```

## Test

```bash
./gradlew testDebugUnitTest             # JVM unit tests
./gradlew lintDebug                     # Android lint
./gradlew connectedDebugAndroidTest     # instrumented tests, physical device only
```

Instrumented tests clear app data on the connected device. CI runs unit tests and lint only.

## Release

1. Bump `versionName` and `versionCode` in `app/build.gradle.kts`
2. Commit and push
3. Draft a GitHub Release, tag `v<versionName>` on `main`, and publish it
4. `release.yml` builds, signs, verifies, and uploads the APK to that release

Signing reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` from the
environment; in CI these come from repository secrets. Without them the release build is
unsigned.
