# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Solo is a closed-source Klondike solitaire for Android, to be published on Google Play: Kotlin, Jetpack Compose, Material 3 Expressive with dynamic color, and Hilt. It uses a multi-module Clean Architecture. The UI is in pt-BR and English: every user-facing string lives in `values/strings.xml` and `values-pt-rBR/strings.xml` of its module, never hard-coded. The user makes the product and design decisions (rules, layout, libraries, visuals), so ask before choosing one instead of picking a default.

## Commands

JDK 21 and Android SDK Platform 37 are required (compileSdk 37, targetSdk 36, minSdk 26).

```bash
./gradlew assembleDebug                          # build
./gradlew installDebug                           # build + install on the connected device
./gradlew ktlintCheck detekt lint                # static analysis (ktlintFormat fixes formatting)
./gradlew test koverVerify                       # all unit tests (screenshots included) + coverage gates
./gradlew :domain:test --tests "io.github.vinaooo.solo.domain.rules.GameEngineTest"          # one JVM test class
./gradlew :feature:game:testDebugUnitTest --tests "*GameViewModelTest*"                    # one Android-module test
./gradlew recordRoborazziDebug                   # re-record screenshot goldens after an intended UI change
./gradlew :domain:pitest                         # mutation testing (gate: 80% killed, 90% coverage)
./gradlew :app:assembleRelease                   # minified (R8) release APK, signed when the upload key is configured
```

The full gate, matching CI: `./gradlew ktlintCheck detekt lint test verifyRoborazziDebug koverVerify`.

- **Low RAM:** `gradle.properties` is sized for a low-RAM dev machine (2 GB Gradle heap, no parallel builds, 1 GB test workers), because the user runs other heavy jobs at the same time. Don't raise these values. CI writes its own larger settings to `~/.gradle/gradle.properties` in `.github/workflows/ci.yml`.
- **Warnings are errors** in every Kotlin compilation. Experimental APIs (most Material 3 Expressive APIs) need a local `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Test compilations already opt in to `ExperimentalCoroutinesApi`.
- **Release signing:** `ReleaseSigning` (in `build-logic`, applied by `solo.android.application`) reads the upload key from `local.properties` (`solo.signing.storeFile`, `solo.signing.storePassword`, `solo.signing.keyAlias`, `solo.signing.keyPassword`) or, on CI, from the `SOLO_SIGNING_*` environment variables. Environment variables win.
  - With none of them set, `assembleRelease` builds an unsigned APK. A partial setup fails the build and names what is missing.
  - The keystore and its passwords are never committed. `*.jks` and `local.properties` are ignored.
  - `build-logic` has its own tests, which `./gradlew test` runs.
- **Release build:** it uses R8 with resource shrinking. `app/proguard-rules.pro` is empty, because Hilt, Room and kotlinx.serialization bring their own keep rules. Without the upload key, you can still try a release build on the device by signing it with the debug key:
  - run `zipalign -p 4` and then `apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android`, both from the build-tools;
  - `adb install -r` then installs it over a debug install, keeping its data;
  - the saved game, settings and scores are compatible both ways, so a regression shows up as a lost game or reset settings, not as a crash, because a save that can't be read is discarded.
- **detekt** has no baseline, so every finding has to be fixed. The `TooManyFunctions` limit (11 per class, object or file) is usually handled by splitting a file by responsibility, as `GameScreen.kt` / `GameControls.kt` and `DomainRulesModule` / `UseCaseModule` do.

## Build setup

- Modules apply convention plugins from `build-logic/convention` (`solo.android.library`, `solo.android.compose`, `solo.android.feature`, `solo.hilt`, `solo.jvm.library`, `solo.quality`) instead of configuring AGP, Kotlin, detekt, ktlint and Kover themselves.
- AGP 9 has Kotlin built in, so there is no `kotlin-android` plugin. Versions are pinned in `gradle/libs.versions.toml`.
- `material3` is pinned to a 1.5.0 alpha on purpose, above the Compose BOM: that's where the Expressive APIs (`MaterialExpressiveTheme`, `MotionScheme.expressive()`, floating toolbars, `LoadingIndicator`) are public.
- **Coverage:** the root project applies `solo.root.coverage`, which merges every module's Kover data. It needs the same filters as the modules (`KoverFilters.kt`, which excludes composables and Hilt/Room/serializer generated code), because the root report doesn't inherit the modules' filters.
  - The root floor is 70% of lines.
  - `:domain` has its own 90% rule.
  - UI is covered by Compose UI and screenshot tests instead.

## Tests

- **Test engines:** all tests run on the JUnit Platform.
  - Plain unit tests use JUnit 5 (`org.junit.jupiter.api.Test`) with Kotest assertions and property tests (`checkAll`).
  - Robolectric tests (Room, Compose UI, screenshots) are JUnit 4 (`org.junit.Test`, `@RunWith(RobolectricTestRunner::class)`) and run through the vintage engine.
- **Robolectric setup:** Robolectric runs at SDK 36 (`src/test/resources/robolectric.properties`). The JDK `--add-opens` flags it needs are set in `configureJUnitPlatform`.
- **Fakes:** repository fakes live in `:domain`'s `testFixtures` (`FakeSavedGameRepository`, etc.). Feature modules get them through `testImplementation(testFixtures(project(":domain")))`. Prefer these fakes to mocks.
- **Screenshots:** Roborazzi goldens live in each module's `src/test/screenshots/`. `roborazzi.test.verify=true`, so any pixel change fails `test`. Record and verify on Linux, and keep dates and other machine-dependent values out of captures (the scores screenshot uses noon-UTC timestamps).
- **ViewModel tests:** `GameViewModel` runs an endless clock loop, so its tests go through the `gameTest {}` helper, which pauses every ViewModel in a `finally`. Without it, `runTest` waits for the clock forever and a failing assertion hangs instead of failing.
- **Whole-app test:** `app/src/test/.../AdBannerEveryScreenTest` launches the real `MainActivity` under Hilt (`HiltTestApplication`) and checks that the ad banner is on every screen.

## Architecture

The dependencies run `:app` → `:feature:*` → `:domain` ← `:data`, and `:feature:*` also depends on `:core:*`. `:domain` is plain Kotlin/JVM with no Android or DI annotations. `:app/di` assembles its classes with `@Provides`, and `:data/di` binds the repository implementations.

**Domain (`:domain`)**
- `GameState` is immutable: stock, waste, 4 foundations, 7 tableau columns, score, moves and time.
- `GameEngine.apply(state, move)` returns `MoveOutcome.Applied` or `Rejected`. The rules sit behind `RuleSet` (`KlondikeRules`, with one small rule per move type) and scoring behind `ScoringStrategy` (`StandardScoring`, Windows Standard scoring), so new variants and scoring modes plug in without touching the engine.
- `GameSession` (a seed, the state and an `UndoHistory`) is the unit that gets played, undone and saved as JSON with kotlinx.serialization. Deals are reproducible from the seed (`SeededShuffler`), which is how "restart this deal" works.
- `MoveResolver` turns a tap or drop into a `Move`. `HintEngine` ranks the legal moves, and `AutoCompleter` finishes a game that can no longer get stuck.
- The use cases (`StartNewGame`, `FinishGame`, …) own the rules about scores and stats. For example, starting a new game counts the unfinished one as a loss.

**Data (`:data`)**
- Room stores the top-10 scores and a single stats row.
- The game in progress is saved to a file with an atomic temp-file-and-rename and a versioned envelope. A corrupt file is discarded.
- Settings are in Preferences DataStore.

**Game screen (`:feature:game`)**
- Unidirectional data flow: `GameViewModel` exposes a `StateFlow<GameUiState>` and receives `GameIntent`s.
- It saves after every move, undo and pause.
- The clock is a `Ticker` that runs only while the screen is resumed and the game is in progress.
- Auto-complete steps every 120 ms and must read the latest session on each step, or it overwrites clock ticks that happened in between.
- Sound and haptics sit behind `GameFeedback`, and whether they play depends on the settings.

**Board rendering**
- `BoardLayout` is pure geometry. It maps each card to absolute pixel positions, compresses long columns, and does hit-testing (`pileAt`, `dropTarget`).
- `GameBoard` keys every card by its identity and animates it to its position with the motion scheme's spatial spring.
- The drag adds back the touch slop that `detectDragGestures` leaves out of the first drag amount, so the card stays under the finger.
- **TalkBack:**
  - `CardRole` decides what each card says. Face-down column cards and cards under the top of a pile are hidden and counted by the card above them.
  - Each face-up card offers TalkBack actions for its legal destinations (`GameUiState.destinations`, from `MoveResolver.destinations`).
  - Moves are announced from a 1dp live region (`Announcer`), because `View.announceForAccessibility` is deprecated. It must not be a direct child of a `Surface`, which would stretch it over the whole game and block touch exploration.

**Layouts**
- Portrait: stats and the Scores/Settings buttons on top, the board below, and a horizontal floating toolbar at the bottom.
- Landscape: `CenteredRow` puts stats and buttons on the left, the board centered, and a vertical floating toolbar on the right. Both sides get the width of the wider one, so the board stays centered.

**App shell (`:app`)**
- `SoloApp` owns the only ad banner, in the `Scaffold`'s `bottomBar` around a type-safe `NavHost`. It consumes only the bottom inset, and each screen pads for the status bar itself.
- Ads go through `AdBannerProvider` in `:core:ads`, which is a placeholder for now; AdMob comes later.

**Theme**
- `SoloTheme` (`:core:designsystem`) picks light or dark from the Settings choice or the system, with dynamic color on Android 12+ or the brand colors otherwise. Card faces and backs are drawn in Compose.
- The window theme has a dark `values-night` variant. Otherwise Android 16's "make more apps dark" setting inverts the app, because it ignores `forceDarkAllowed` and inverts any light window theme at night.
- Suit symbols carry U+FE0E so they render as text, not emoji.

## Git

Only `master` is long-lived: branch from `master` and open the PR against `master`. The user merges PRs from their own account (GitHub won't let them approve their own PRs), using merge commits. CI (`.github/workflows/ci.yml`) runs on every PR and on pushes to `master`, and a separate nightly workflow runs Pitest. The repository is private and proprietary ("All rights reserved"); signing keys and ad unit IDs go in `local.properties` or GitHub Secrets and are never committed.
