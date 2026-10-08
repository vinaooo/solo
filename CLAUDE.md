# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Solo is an open-source (MIT) Klondike solitaire for Android, to be published on Google Play: Kotlin, Jetpack Compose, Material 3 Expressive with dynamic color, and Hilt. It uses a multi-module Clean Architecture. The UI is in pt-BR and English: every user-facing string lives in `values/strings.xml` and `values-pt-rBR/strings.xml` of its module, never hard-coded. The user makes the product and design decisions (rules, layout, libraries, visuals), so ask before choosing one instead of picking a default.

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
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest   # on-device UI tests (emulator; not part of the gate or CI)
adb shell am start -S -n io.github.vinaooo.solo/.debug.DebugGameActivity   # debug build: a game one move from auto-complete (--es game near_stuck: one move from stuck; tallest: the tallest possible column; --es state <code>: a bug report's "State:" block; --es load game.json: a report's attachment, pushed to /sdcard/Android/data/io.github.vinaooo.solo/files/)
```

The full gate, matching CI: `./gradlew ktlintCheck detekt lint test verifyRoborazziDebug koverVerify`.

- **Low RAM:** `gradle.properties` is sized for a low-RAM dev machine (2 GB Gradle heap, no parallel builds, 1 GB test workers), because the user runs other heavy jobs at the same time. Don't raise these values. CI writes its own larger settings to `~/.gradle/gradle.properties` in `.github/workflows/ci.yml`.
- **Warnings are errors** in every Kotlin compilation. Experimental APIs (most Material 3 Expressive APIs) need a local `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Test compilations already opt in to `ExperimentalCoroutinesApi`.
- **Release signing:** vinkit's `vinkit.android.application` reads the upload key from `local.properties` (`vinkit.signing.storeFile`, `vinkit.signing.storePassword`, `vinkit.signing.keyAlias`, `vinkit.signing.keyPassword`) or, on CI, from the `VINKIT_SIGNING_*` environment variables. Environment variables win.
  - With none of them set, `assembleRelease` builds an unsigned APK. A partial setup fails the build and names what is missing.
  - The keystore and its passwords are never committed. `*.jks` and `local.properties` are ignored.
- **Versions** come from git (vinkit's `AppVersion`): `versionCode` is `git rev-list --count HEAD` and `versionName` the latest `vX.Y.Z` tag from `git describe` (`0.0.0-g<hash>` before the first tag). A shallow clone fails the build, so both workflows check out with `fetch-depth: 0`. Don't set `versionCode`/`versionName` in `app/build.gradle.kts`.
- **Play upload:** `.github/workflows/release.yml` builds `:app:bundleRelease` and uploads it with `r0adkll/upload-google-play`: to the internal track on a `vX.Y.Z` tag, or by hand with a track and status. The job only runs when the repository variable `PLAY_UPLOAD_ENABLED` is `true` (off for now). It fails early if a signing, ads or service-account secret is missing. The setup steps are in the README. `actionlint` checks workflows; it isn't installed, so download its release binary.
- **Release build:** it uses R8 with resource shrinking. `app/proguard-rules.pro` is empty, because Hilt, Room and kotlinx.serialization bring their own keep rules. Without the upload key, you can still try a release build on the device by signing it with the debug key:
  - run `zipalign -p 4` and then `apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android`, both from the build-tools;
  - `adb install -r` then installs it over a debug install, keeping its data;
  - the saved game, settings and scores are compatible both ways, so a regression shows up as a lost game or reset settings, not as a crash, because a save that can't be read is discarded.
- **detekt** has no baseline, so every finding has to be fixed. The `TooManyFunctions` limit (11 per class, object or file) is usually handled by splitting a file by responsibility, as `GameScreen.kt` / `GameControls.kt` and `DomainRulesModule` / `UseCaseModule` do.

## Build setup

- **vinkit** (`github.com/vinaooo/vinkit`, from JitPack) brings the build: `vinkit.tag` in `gradle.properties` picks the release, always the newest, never a local copy. Modules apply its convention plugins (`vinkit.android.library`, `vinkit.android.compose`, `vinkit.android.feature`, `vinkit.hilt`, `vinkit.jvm.library`, `vinkit.quality`) instead of configuring AGP, Kotlin, detekt (its rules ship with vinkit), ktlint and Kover themselves. `vinkit.android.feature` adds `:domain` and its test fixtures, not `:core:*`: each feature lists those. A gap in vinkit is fixed there first and released as a tag (with a `CHANGELOG.md` entry), then the tag is bumped here.
- AGP 9 has Kotlin built in, so there is no `kotlin-android` plugin. Versions are pinned in vinkit's catalog (`com.github.vinaooo.vinkit:catalog`), the `libs` catalog here.
- `material3` is pinned to a 1.5.0 alpha on purpose, above the Compose BOM: that's where the Expressive APIs (`MaterialExpressiveTheme`, `MotionScheme.expressive()`, floating toolbars, `LoadingIndicator`) are public.
- **Coverage:** the root project applies `vinkit.root.coverage`, which merges every module's Kover data with the modules' filters (composables and Hilt/Room/serializer generated code excluded).
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
- **On-device tests:** `app/src/androidTest` runs the real app under Hilt (`HiltTestRunner`) on an emulator. Test Orchestrator gives each test its own process and clears the app's data, so every test starts on a fresh deal. Without it, the second Hilt graph in one process would open a second DataStore on the same file and crash. `FakeAdsModule` lives in `src/sharedTest`, shared by Robolectric and on-device tests. Checked on `Solo_API_26` (Android 8.0, minSdk) and `Pixel_9a_Android_16`. The latter needs `-skin 1080x2424` to start headless. Run them on emulators, not on the user's phones, which have the upload-key-signed release installed: installing the debug build means uninstalling it and losing the saved game.
- **Whole-app test:** `app/src/test/.../AdBannerGameScreenOnlyTest` launches the real `MainActivity` under Hilt (`HiltTestApplication`, with `FakeAdsModule`) and checks that the banner is on the game screen and not on Scores or Settings.

## Architecture

The dependencies run `:app` → `:feature:*` → `:domain` ← `:data`, and `:feature:*` also depends on `:core:*`. `:domain` is plain Kotlin/JVM with no Android or DI annotations. `:app/di` assembles its classes with `@Provides`, and `:data/di` binds the repository implementations.

**Domain (`:domain`)**
- `GameState` is immutable: stock, waste, 4 foundations, 7 tableau columns, score, moves and time.
- `GameEngine.apply(state, move)` returns `MoveOutcome.Applied` or `Rejected`. The rules sit behind `RuleSet` (`KlondikeRules`, with one small rule per move type) and scoring behind `ScoringStrategy`, so new variants and scoring modes plug in without touching the engine.
- **Game modes:** `GameState.mode` (`GameMode`, default `STANDARD`) travels with the game, like `drawMode`, so a resumed or restarted game keeps its rules whatever Settings say. `scoringFor(mode)` picks the scoring, and `ScoringStrategy.bounded` the floor (Standard points never go below 0; Vegas money does).
  - **Standard** (`StandardScoring`, Windows Standard: time penalty and speed bonus) and **Counter time** use the same points, but Counter time hides them and ranks by the fastest win.
  - **Vegas** (`VegasScoring`): a game starts at -$52, +$5 a card onto a foundation, -$5 when one leaves it (otherwise moving a card up and down makes endless money), nothing else, undo included. Passes are limited (`GameMode.recycleLimit`: 1 pass in Draw 1, 3 in Draw 3), which `RecycleRule` and `DeadEndDetector` (passes used are part of a position) respect.
  - **Vegas cumulative:** Vegas whose balance (`VegasBankRepository`, key `vegas_bank` in the settings DataStore) carries over: a new game starts at bank - 52. The bank is committed only by `FinishGame` (a win) and `StartNewGame` (abandoning, even an untouched game: its $52 is spent). It has no ranking, by the user's choice.
  - **Counter time:** 10 minutes in Draw 1, 15 in Draw 3 (`timeLimitSeconds`). The engine stops the clock at the limit and rejects moves once `isTimeUp`. The ViewModel then records the loss with `LoseGame`, which also deletes the saved game, so the next new game doesn't count it again; it also doesn't save on pause and freezes the clock while auto-complete runs. The time's-up dialog offers a new game or the same deal again.
- **Difficulty** (`Difficulty`, Settings default Normal) travels with the game like `mode`. Hard deals any shuffle. Easy and Normal deal from lists of seeds a solver won (`WinnableDeals`, resources in `domain/src/main/resources/deals/`, one per draw mode × limited/unlimited passes × difficulty): Easy won within 1,000 positions searched, Normal only those that needed more but were won within 20,000, so the lists share no deal (the user's choice: three levels that feel different).
  - The lists are precomputed because a live search took up to ~3 s a deal on a phone (Vegas Draw 1). `./gradlew :domain:generateDeals` rewrites them (about half an hour; a `JavaExec`: run as a test, in parallel, the search ran about 50 times slower, likely because of the coverage agent); the solver (`DealSolver`) lives in the test sources only.
  - Seeds depend on Kotlin's seeded `Random`, which may change between Kotlin versions; `WinnableDealsTest` replays a few of each list and fails if it did, and then the lists must be regenerated.
  - Changing it in Settings starts a new game (with the same confirmation as the modes). Scores share one ranking and show their difficulty; saves from before it read as Hard.
- `GameSession` (a seed, the state and an `UndoHistory`) is the unit that gets played, undone, redone and saved as JSON with kotlinx.serialization. Deals are reproducible from the seed (`SeededShuffler`), which is how "restart this deal" works.
- `UndoHistory` keeps undo and redo stacks. A redo earns the move's points back and counts as a move, but the undo penalty stays. A new move clears the redo stack.
- `MoveResolver` turns a tap or drop into a `Move`. A tapped king that fills its column goes to the next empty column to its right, wrapping around.
- `HintEngine` ranks the legal moves, and `AutoCompleter` finishes a game that can no longer get stuck.
- `DeadEndDetector` searches every position reachable by drawing, recycling and moving between columns for one that puts a card on a foundation or turns one up. It is a cancellable `suspend` search that gives up (says "not stuck") past 5,000 positions, because a real dead end takes the whole search.
- The use cases (`StartNewGame`, `FinishGame`, `LoseGame`, …) own the rules about scores and stats. For example, starting a new game counts the unfinished one as a loss, and an abandoned Vegas game still enters the Vegas ranking with its dollars.

**Data (`:data`)**
- Scores and stats are vinkit's (`vinkit_scores.db`, `RoomScoreRepository` / `RoomStatsRepository`, wired in `data/di/DataModule`). A score is vinkit's `ScoreRecord`: its mode key is `GameMode.name` and its moves, draw mode and difficulty go in `extras` (`model/ScoreRecord.kt` reads them back). Stats are per mode. `GameMode.ranking()` gives each mode's `Ranking` (counter time `FASTEST`, cumulative Vegas none). vinkit's database owns its migrations; never use a destructive fallback there either.
- The game in progress is saved to a file with an atomic temp-file-and-rename and a versioned envelope. A corrupt file is discarded.
- Settings are in one Preferences DataStore (`settings`), shared by two repositories that each write only their own keys: vinkit's `DataStoreAppSettingsRepository` (`AppSettings`: theme, feedback, hand, board position, phone view) and Solo's `DataStoreSettingsRepository` (`Settings`: draw mode, game mode, difficulty, tips shown, deal cursor).
- The Settings screen is vinkit's `SettingsScreen` with Solo's `GameSection` as its game section. Solo's own wording for vinkit's strings ("mesa", "Sons e vibração", "Nova partida", "Relatar um problema") overrides them under the same `vinkit_*` names: the settings ones in `app/src/main/res`, the shell and bug report ones in `:feature:game` (a module's resources win over its dependencies').

**Game screen (`:feature:game`)**
- Unidirectional data flow: `GameViewModel` exposes a `StateFlow<GameUiState>` and receives `GameIntent`s.
- It saves after every move, undo and pause.
- The clock is vinkit's `Ticker`, which runs only while the screen is resumed and the game is in progress.
- Auto-complete steps every 120 ms and must read the latest session on each step, or it overwrites clock ticks that happened in between.
- Sound and haptics sit behind vinkit's `GameFeedback` (`AndroidGameFeedback`, provided in `app/di/FeedbackModule`), and whether they play depends on the settings.
- `DeadEndWatcher` runs the dead-end search after every move on the injected `@SearchDispatcher` (`Dispatchers.Default`; tests pass the test dispatcher). A new move cancels the previous search.
- `GameUiState.deals` counts the games dealt (new game, first game; not a restarted deal or a resumed game). The board plays each new deal once and remembers what it played in `rememberSaveable`, so a draw mode changed in Settings, which starts a new game, is dealt on coming back.
- Changing the draw mode or the scoring mode (vinkit's `IconChoice`) in Settings starts a new game. `SettingsViewModel` asks for confirmation first if a game is in progress (`pendingChange`). The Scores screen is vinkit's (`SoloScoresViewModel`): a tab per mode already played, each with its own stats and ranking; Vegas scores in dollars.

**Board rendering**
- `BoardLayout` is pure geometry. It maps each card to absolute pixel positions, compresses long columns, and does hit-testing (`pileAt`, `dropTarget`).
- The board sits at the top or, with Settings' board position on Bottom, as low as it can while leaving room for the tallest possible column (a bar of 6 face-down cards and a run from king to ace), so it never moves during play. Board position only applies to portrait.
- `GameBoard` keys every card by its identity and animates it to its position with the motion scheme's spatial spring.
- **Layers:** waste 100+, stock 150+, stock cover 198, stock count 199, foundations 200+, columns 300+ (by depth), and in-flight cards `LIFTED_Z` (10 000) + their layer.
  - Only a card arriving in a new pile is lifted. A card shifting within its own pile (the Draw 3 fan closing up, a column re-spacing) keeps its layer, or it covers newer cards.
  - A card counts as flying from the frame its target changes (`departing`), not from when its animation starts.
  - A card turning face up (`appearing`) snaps into place under the card leaving it and is never lifted.
  - A card turned face down by an undo stays drawn until the card coming back covers it.
  - Drawn cards keep the waste's layer and come out from under the `StockCover`. Changing a card's layer as it settles made card faces blink.
  - Known gap: two cards in flight at once are layered by destination depth, not by launch order.
- `Deal` plays the new-game deal: the cards gather into a deck, then each column's bar grows and its top card flies out face up, then the stock. It uses the fast spring, and pauses are timed with `animate()` rather than `delay()` so the device's animation speed scales them.
- **Pitfalls:**
  - `PlayingCard` keeps its face animation (covered strip ↔ center) outside its `BoxWithConstraints`, because Compose sometimes rebuilds that box's content when cards move and resets any state inside it.
  - Tap ripples are drawn by `PlayingCard` from the tap handler's `InteractionSource`, inside the card's clip.
  - To check animations, record the phone (`adb shell screenrecord`), step through the frames (`ffmpeg … -fps_mode passthrough`), and check `adb shell settings get global animator_duration_scale` first.
- The drag adds back the touch slop that `detectDragGestures` leaves out of the first drag amount, so the card stays under the finger.
- **TalkBack:**
  - `CardRole` decides what each card says. Face-down column cards and cards under the top of a pile are hidden and counted by the card above them.
  - Each face-up card offers TalkBack actions for its legal destinations (`GameUiState.destinations`, from `MoveResolver.destinations`).
  - Moves are announced from vinkit's 1dp live region (`Announcer`, in `GameSurface`; Solo's text is `announcementText`), because `View.announceForAccessibility` is deprecated. It must not be a direct child of a `Surface`, which would stretch it over the whole game and block touch exploration.

**Layouts**
- The screen is vinkit's shell: `GameSurface` (the table color, the bug-report screenshot, the TalkBack announcer) around `GameFrame(boardAspectRatio = null, sideWidth = null)`; Solo fills its slots with `GameInfo` (the stats), the board and vinkit's `GameToolbar`.
- Portrait: stats and the Scores/Settings buttons on top, the board below, and a horizontal floating toolbar at the bottom.
- Landscape: stats and buttons on one side, the board centered, and a vertical floating toolbar on the preferred hand's side (vinkit's `CenteredRow`; both sides get the width of the wider one, so the board stays centered). The side fits the stats (`sideWidth = null`), so the board gets the rest.
  - The board is `sideways` (`BoardLayout`, from `LocalFrameInfo.current.landscape`): no top row. On the handedness edge, the stock with the waste under it (the Draw 3 fan grows down); next to it, the four foundations one under the other; then the tableau, which gets the whole height. Right-handed is the mirror image.
  - The stacked foundations set the card size (4 cards tall), so landscape cards are smaller than a top row would allow.
- **Phone view** (Settings, shown only when the short side is 600dp+): on a tablet, the traditional board (top row, never `sideways`), at most 412dp wide like a phone's, at the top of the board's room, in both orientations, on the side Settings' board side picks (left, center or right; in Appearance, revealed under the switch only while phone view is on); vinkit's phone view puts the toolbar in that column too, as in every vinkit game.
- **Toolbar:**
  - The actions are `ToolbarAction`s (`toolbarActions` in `GameControls.kt`). Auto-complete is `visible` only when it can play: vinkit grows it only along the toolbar. Its tip is a `ToolbarTip`, a Material tooltip with a caret.
  - "Report a bug" sits in the new-game menu. The game screen records itself into a graphics layer; on report, it waits for the menu and its scrim to close, then captures the board. vinkit's `BugReportDialog` sends by email to `vrpedrinho+solo@gmail.com` (`SoloReports` in `GameReport.kt`), with the screenshot and the game's JSON attached through vinkit's `FileProvider` (`${applicationId}.reports`; the app declares none), or opens a prefilled issue on `vinaooo/solo`. `gameReport` adds the settings, the game and a "State:" block, the exact board as `BoardCodec` text (gzipped JSON in Base64, about 1,000 characters), so a GitHub issue, which takes text only, can be replayed too. `DebugGameActivity` loads either the block or the attached game.json. There is no token in the app: the repo is public, and an embedded token could be extracted.
  - vinkit's new-game menu (`menuOptions`: new game, restart this deal; vinkit adds "Report a bug") draws its own FAB-menu pills in a popup with 40dp of room around them, because Material's `FloatingActionButtonMenu` clips its items. The toolbar draws a 60% scrim behind itself while the menu is open.

**App shell (`:app`)**
- The manifest declares `android:appCategory="game"` (checked by `AppCategoryTest`).
- `SoloApp` is the type-safe `NavHost`. Only the game route carries the ad banner, in a `Column` under the game: the banner pads for the navigation bar and the game consumes that inset. Scores and Settings have no ads and pad for the system bars themselves.

**Ads (vinkit `ads`)**
- AdMob through vinkit's `AdBannerProvider` (`AdMobBanner`), wired in `app/di/AdsModule`: an inline adaptive banner (`AdSize.getInlineAdaptiveBannerAdSize`; the user found the large anchored size, about 130dp, too big, and the other anchored sizes are deprecated), capped at 60dp tall. A phone in portrait gets it full width; in landscape or on a tablet (600dp+ wide) it is at most 320dp wide (a standard banner) and centered, and in landscape, where height is short, 50dp tall. Its full-width slot always takes that height, so the board never jumps when an ad loads or fails. `SoloApp` gives the slot the table's color (the banner draws on `colorScheme.surface`).
- Consent comes before ads. `MainActivity` calls `AdConsent.gather` once per launch. `DefaultAdConsent` updates the status, shows the consent form if needed, and only then starts the SDK and lets the banner load. The SDKs sit behind `ConsentClient` / `AdsSdk`, so vinkit unit-tests the order with fakes. Settings always links to the privacy policy (`privacy_policy_url`, `#en` or `#pt-br` by language) and shows "Privacy options" only when the consent SDK says it's required.
- IDs: debug builds always use Google's test IDs. Release builds read `vinkit.ads.appId` / `vinkit.ads.bannerId` from `local.properties` (or `VINKIT_ADS_APP_ID` / `VINKIT_ADS_BANNER_ID`), and fall back to test IDs when neither is set (vinkit's `AdIds`; it rejects swapped or partial IDs). `vinkit.ads.testDeviceIds` lists hashed device IDs that always get test ads. Debug builds make the consent SDK act as in the EEA, and that only works on test devices: emulators, or phones listed there.
- **Real IDs on this machine:** the user's `local.properties` holds Solo's real AdMob IDs, so every local release build serves real ads. Install it only on devices listed in `vinkit.ads.testDeviceIds` (the hash is printed in logcat by the ads and consent SDKs), and never tap its ads.
  - To see the real European consent form, make a local release build with `simulateEea = true` in `AdsModule` on a test device, then revert the change and never commit it. Once the form has been answered it doesn't come back on launch; Settings → Privacy options reopens it.
  - A new or unreviewed ad unit often returns "no fill" (code 3). Phones without Google Play services can't load ads or the consent form at all; the game still works and the slot stays empty.
- **Website:** the privacy policy (`/solo/privacy.html`) and `app-ads.txt` live in the public `vinaooo.github.io` repo, not here. Its default branch is `main`, but Pages serves `master`, so push the same commit to both. Pages sometimes doesn't rebuild on push; `gh api -X POST repos/vinaooo/vinaooo.github.io/pages/builds` forces it. Then confirm the file with `curl`.
- App tests replace `AdsModule` with `FakeAdsModule` (`@TestInstallIn`), so the real SDKs never run under Robolectric.

**Theme**
- `SoloTheme` (`:core:designsystem`) wraps vinkit's `VinkitTheme`, which picks light or dark from the Settings choice or the system, with dynamic color on Android 12+ or the chosen `ThemeColor` otherwise, and provides `LocalCardColors` from its scheme. Card faces and backs are drawn in Compose.
- `ThemeColor` (vinkit `core`) offers eight colors, shown in Settings as vinkit's `ColorChoice`, a row of circles revealed when dynamic color is turned off (always shown below Android 12). Green is Solo's brand. The palettes live in vinkit (`PaletteColors.kt`, generated once with Google's material-color-utilities and committed as constants), so there is no runtime color library.
- The window theme has a dark `values-night` variant. Otherwise Android 16's "make more apps dark" setting inverts the app, because it ignores `forceDarkAllowed` and inverts any light window theme at night.
- Suit symbols carry U+FE0E so they render as text, not emoji.
- **Launcher icon:** an adaptive vector (`app/src/main/res/drawable/ic_launcher_*`, generated from Roboto Bold's "S" and the spade path). On Android 12+ its background and "S" take the wallpaper accent (`values-v31`); below that, the brand green. Many launchers (Launcher3 and its forks) cache the icon until the app updates, so a wallpaper change shows up late there. The separate monochrome layer, with the marks cut out, is used when the user turns on themed icons. `LauncherIconScreenshotTest` holds the golden.

## Git

Only `master` is long-lived: branch from `master` and open the PR against `master`. The user merges PRs from their own account (GitHub won't let them approve their own PRs), using merge commits. CI (`.github/workflows/ci.yml`) runs on every PR and on pushes to `master`, and a separate nightly workflow runs Pitest. The repository is public and MIT-licensed; signing keys and ad unit IDs go in `local.properties` or GitHub Secrets and are never committed.
