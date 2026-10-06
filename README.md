# Solo

Klondike solitaire for Android — Kotlin, Jetpack Compose, Material 3 Expressive.

> Open source under the [MIT License](LICENSE).

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

## Building

1. Clone the whole history. The version is computed from git, so a shallow clone (`--depth`) fails the build:

   ```bash
   git clone https://github.com/vinaooo/solo.git
   cd solo
   ```

2. Point Gradle at the Android SDK, either with `ANDROID_HOME` or a `local.properties` file in the project root (Android Studio writes it for you):

   ```properties
   sdk.dir=/path/to/Android/Sdk
   ```

3. Build, or build and install on a connected device or emulator:

   ```bash
   ./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
   ./gradlew installDebug
   ```

   Debug builds show Google's test ads and need no keys.

4. Optionally, build the minified release APK. Without an upload key (see [Secrets](#secrets)) it is unsigned:

   ```bash
   ./gradlew :app:assembleRelease   # app/build/outputs/apk/release/
   ```

Or open the project in Android Studio and run the `app` configuration.

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

### Releasing to Google Play

`.github/workflows/release.yml` builds the signed bundle and uploads it to Google Play: to the **internal** track when a `vX.Y.Z` tag is pushed, or to any track and status when started by hand (Actions → Release to Google Play → Run workflow). It's **off** until the repository variable `PLAY_UPLOAD_ENABLED` is `true`; until then every run is skipped.

To turn it on:

1. **Upload the first release by hand** in Play Console. Google Play only accepts API uploads for an app that already exists there.
2. **Create a service account.**
   - In Google Cloud Console, create (or pick) a project and enable the **Google Play Android Developer API**.
   - Under IAM & Admin → Service accounts, create an account and add a **JSON key**.
3. **Give it access in Play Console.**
   - In Users and permissions, invite the service account's email.
   - Give it the Solo app with **Release to testing tracks** and, if CI should also release to production, **Release to production**.
4. **Add the repository secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `SOLO_UPLOAD_KEYSTORE_BASE64` | `base64 -w0 /path/to/solo-upload.jks` |
| `SOLO_SIGNING_STORE_PASSWORD`, `SOLO_SIGNING_KEY_ALIAS`, `SOLO_SIGNING_KEY_PASSWORD` | same as in `local.properties` |
| `SOLO_ADS_APP_ID`, `SOLO_ADS_BANNER_ID` | the AdMob IDs |
| `PLAY_SERVICE_ACCOUNT_JSON` | the whole JSON key file |

5. **Add the repository variable** `PLAY_UPLOAD_ENABLED` = `true` (same page, Variables tab).

The workflow stops before building if any secret is missing, so a release can never go out unsigned or with test ads. The bundle and the R8 mapping file are also attached to each run.

### Versions

The version comes from git, so there is nothing to edit before a release:

- `versionCode` is the number of commits up to the built commit (`git rev-list --count HEAD`). It grows with every merge into `master`.
- `versionName` comes from the latest `vMAJOR.MINOR.PATCH` tag. A tagged commit is `1.2.0`, a later one `1.2.0-3-gabc1234`, and before the first tag `0.0.0-gabc1234`.

To release, tag the commit on `master` and push the tag: `git tag v1.0.0 && git push origin v1.0.0`. Build from a full clone: a shallow one fails the build, because its commit count would be too low for Play.
