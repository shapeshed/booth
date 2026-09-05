# Developer Guide

This guide covers local development, testing, release tasks, and signing for Booth.

## Requirements

- JDK 17
- Android SDK with platform 37 installed
- Android Studio or the Gradle wrapper

The app targets SDK 37 and has a minimum SDK of 26.

## Build and test

```sh
./gradlew quality
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For a production-like device build using release shrinking and the debug signing key:

```sh
./gradlew installProductionDebugKey
```

This preserves the package id and app data, while allowing installation without release signing
credentials.

## Install on a device

Connect a device with USB debugging enabled, then run:

```sh
adb devices
./gradlew installDebug
```

For instrumentation tests that preserve the installed app and its data:

```sh
./gradlew preservingDebugAndroidTest
```

## Architecture

Booth keeps subscriptions, episodes, playback state, downloads, and settings local. Room owns
durable podcast data and search indexes; Preferences DataStore owns lightweight settings. OkHttp
handles feed, catalogue, artwork, and media requests. WorkManager performs refresh, subscription,
parsing, indexing, and download work in the background. Media3 owns playback and Android media
controls.

RSS/Atom parsing, URL handling, sorting, persistence mappings, search transformations, and playback
state transitions should remain deterministic and testable in `data/`. Compose screens should
collect state with lifecycle-aware APIs and delegate side effects to view models, repositories,
workers, or services.

## Release process

GitHub Actions runs CI on pushes to `main` and pull requests. Nightly builds run when there have
been commits in the previous 24 hours and publish a signed APK to the replaceable `nightly` GitHub
Release. Tagged releases use `v*` tags and publish a draft GitHub Release containing a signed APK
and SHA-256 checksum.

Local release preparation does not tag, push, or publish anything:

```sh
./gradlew test lint assembleRelease
```

Before a release, update `versionCode` and `versionName` in `app/build.gradle`, review the user-
facing changes, and create a signed tag only when ready:

```sh
git tag -s v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```

Booth is distributed through GitHub Releases only. Google Play, F-Droid, and Fastlane publishing
are not configured.

## Signing

GitHub releases and nightlies use a private Android release keystore. The same key must be used for
future releases so Android can install updates over existing installations.

Required environment variables and GitHub repository secrets are:

```text
BOOTH_KEYSTORE_FILE
BOOTH_KEYSTORE_PASSWORD
BOOTH_KEY_ALIAS
BOOTH_KEY_PASSWORD
BOOTH_KEYSTORE_BASE64
```

Create a local keystore and signing environment:

```sh
scripts/create-release-keystore.sh
source local/release-signing.env
./gradlew assembleRelease
```

The helper can also upload the values as GitHub secrets when the authenticated `gh` CLI is
available:

```sh
scripts/create-release-keystore.sh --set-github-secrets
```

Never commit keystores, passwords, signed APKs, or generated local signing files. The `local/`
directory is intentionally ignored.

## License

Booth is licensed under the Apache License, Version 2.0. Contributions are made under the same
license.
