# Solo

Klondike solitaire for Android — Kotlin, Jetpack Compose, Material 3 Expressive.

> Proprietary software. All rights reserved. See [LICENSE](LICENSE).

## Modules

| Module | Responsibility |
|---|---|
| `:domain` | Pure Kotlin game rules, scoring, use cases, repository interfaces |
| `:data` | Room (scores/stats), DataStore (settings), saved game serialization |
| `:core:designsystem` | `SoloTheme` (dynamic color, light/dark, expressive motion), card composables |
| `:core:ui` | Shared UI helpers |
| `:core:ads` | AdMob banner and consent (UMP) behind `AdBannerProvider` / `AdConsent` |
| `:feature:game` / `:feature:scores` / `:feature:settings` | Screens + ViewModels |
| `:app` | Application, navigation, app scaffold with the bottom banner |
| `build-logic` | Gradle convention plugins |

## Requirements

- JDK 21
- Android SDK Platform 37 (compileSdk); targetSdk 36; minSdk 26

## Common tasks

```bash
./gradlew assembleDebug            # build
./gradlew ktlintCheck detekt lint  # static analysis
./gradlew test koverVerify         # unit tests + coverage gates
./gradlew :domain:pitest           # mutation testing
./gradlew recordRoborazziDebug     # update the screenshot goldens (every test run verifies them)
./gradlew connectedDebugAndroidTest
```

## Secrets

Signing keys and ad unit IDs live in `local.properties` / GitHub Secrets and are never committed.

To sign release builds, keep the upload keystore outside the repository (and backed up) and add to `local.properties`:

```properties
solo.signing.storeFile=/path/to/solo-upload.jks
solo.signing.storePassword=…
solo.signing.keyAlias=solo-upload
solo.signing.keyPassword=…
```

On CI, set the same values as the `SOLO_SIGNING_STORE_FILE`, `SOLO_SIGNING_STORE_PASSWORD`, `SOLO_SIGNING_KEY_ALIAS` and `SOLO_SIGNING_KEY_PASSWORD` environment variables. Without them, `./gradlew :app:assembleRelease` builds an unsigned APK.

### Ads

Debug builds always show Google's test ads. For release builds, add your AdMob IDs to `local.properties` (or set `SOLO_ADS_APP_ID` / `SOLO_ADS_BANNER_ID` on CI); without them, release builds show test ads too:

```properties
solo.ads.appId=ca-app-pub-…~…        # the app ID, with a ~
solo.ads.bannerId=ca-app-pub-…/…     # the banner ad unit ID, with a /
solo.ads.testDeviceIds=…             # optional: hashed IDs of your own devices (from logcat), which always get test ads
```

### Versions

The version comes from git, so there is nothing to edit before a release:

- `versionCode` is the number of commits up to the built commit (`git rev-list --count HEAD`). It grows with every merge into `master`.
- `versionName` comes from the latest `vMAJOR.MINOR.PATCH` tag. A tagged commit is `1.2.0`, a later one `1.2.0-3-gabc1234`, and before the first tag `0.0.0-gabc1234`.

To release, tag the commit on `master` and push the tag: `git tag v1.0.0 && git push origin v1.0.0`. Build from a full clone: a shallow one fails the build, because its commit count would be too low for Play.
