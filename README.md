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
| `:core:ads` | `AdBannerProvider` abstraction (placeholder for now) |
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
./gradlew verifyRoborazziDebug     # screenshot tests (recordRoborazziDebug to update baselines)
./gradlew connectedDebugAndroidTest
```

## Secrets

Signing keys and ad unit IDs live in `local.properties` / GitHub Secrets and are never committed.
