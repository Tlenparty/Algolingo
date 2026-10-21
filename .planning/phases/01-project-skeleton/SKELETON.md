# Walking Skeleton — Algolingo (Алголинго)

**Phase:** 1
**Generated:** 2026-10-21

## Capability Proven End-to-End

The user installs and launches one shared Compose app on the Android emulator and on the iOS simulator. On both, it shows a title and subtitle read at runtime from the bundled `files/hello.json` and decoded with kotlinx.serialization.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Framework | Kotlin Multiplatform 2.4.21 + Compose Multiplatform 1.12.2, material3 | The customer is an Android dev and wants one UI codebase for Android and iOS |
| Build | Gradle 9.7.1 (wrapper, sha256-pinned), AGP 9.3.3, JDK 21 (local Homebrew), version catalog `gradle/libs.versions.toml` | Latest stable pins verified together. Removing the daemon-JVM vendor pin avoids a hidden JDK download |
| Module layout | `androidApp` (com.android.application, MainActivity only) + `shared` (KMP library, `kotlin { android { } }`, iosArm64 + iosSimulatorArm64) + `iosApp` (Xcode project, SwiftUI host) | AGP 9 split from the official KMP wizard. No app code in the platform shells |
| Data layer | No DB. Content = JSON in `shared/src/commonMain/composeResources/files/`, read with `Res.readBytes("files/...")` and decoded with kotlinx-serialization-json 1.12.0 through one top-level `Json { ignoreUnknownKeys = true }` | Offline by design (PLAT-03). Compose Resources bundles the same files into the APK and the iOS .app |
| Persistence (later) | Not in skeleton. Decided in Phase 4 | YAGNI |
| Auth | None | Offline single-user app, no backend |
| Network | None at runtime | Offline constraint. Zero infrastructure |
| DI / navigation | None yet. Plain composables. Navigation is added in the phase that first needs a second screen | Minimal-dependency principle |
| UI entry | `App()` in `shared/commonMain` is the single root. Android: `setContent { App() }`. iOS: `MainViewController()` -> `ComposeUIViewController { App() }` | One composable tree for both platforms |
| Design system | Not in skeleton. Comes from 03-UI-SPEC.md (Phase 3 / 01.1) | Phase 1 is a plain hello screen |
| Ids | applicationId / package / bundle id `com.tlenparty.algolingo`. Resources class `algolingo.shared.generated.resources.Res` | Fixed now; renaming later touches many files |
| Tests | `kotlin.test` in `shared/src/commonTest`, run on host with `./gradlew :shared:testAndroidHostTest` (iOS: `:shared:iosSimulatorArm64Test`). The wizard sample tests are kept | Fast JVM loop for the pure-logic engine in Phase 2 |
| Deployment target | Local CLI: Android emulator (`adb -s <emulator serial>`, never bare adb, because a phone is attached) and iOS Simulator iPhone 17 / iOS 26.2 via `xcodebuild ... CODE_SIGNING_ALLOWED=NO` + `simctl`. Xcode 26.2 | No Apple signing for the MVP. Commands are in README |
| Repo hygiene | Public repo: local.properties, .idea, build, .kotlin and xcuserdata are gitignored. Empty TEAM_ID. No machine paths | Prevents leaks |

## Directory Layout

```
androidApp/                         MainActivity -> App()
shared/src/commonMain/kotlin/com/tlenparty/algolingo/   App.kt (+ feature code in later phases)
shared/src/commonMain/composeResources/files/            bundled JSON content
shared/src/commonTest/kotlin/com/tlenparty/algolingo/    kotlin.test
shared/src/androidMain, shared/src/iosMain               platform actuals, MainViewController
iosApp/                             Xcode project + SwiftUI host
gradle/libs.versions.toml           all version pins
```

## Stack Touched in Phase 1

- [ ] Project scaffold: wizard KMP project, Gradle build, kotlin.test host tests (01-01 Task 1)
- [ ] Routing: single root screen `App()` (no navigation needed yet)
- [ ] Data: one real read of a bundled JSON resource + kotlinx.serialization decode (01-01 Task 2). No writes, because there is no DB in the skeleton. Persistence arrives in Phase 4
- [ ] UI: shared Compose screen rendering the decoded data on both platforms
- [ ] Deployment: CLI install+launch on Android emulator (01-01 Task 3) and iOS simulator (01-02 Task 1), with screenshots. Clean-clone builds and README commands (01-02 Task 2)

## Out of Scope (Deferred to Later Slices)

- Design system, theme, fonts, colors (03-UI-SPEC, Phase 3 / 01.1)
- Navigation, ViewModels, DI
- Content schema and engine (Phase 2)
- Persistence: XP, streak, progress (Phase 4)
- Physical-device iOS, signing, store builds, CI
- Knowledge base notes sync (Phase 01.1)

## Subsequent Slice Plan

- Phase 01.1: user reads all 23 Obsidian notes in the app (new `files/` content + markdown screen)
- Phase 2: content schema + hole engine + lesson reducer, proven by commonTest
- Phase 3: user completes a lesson: theory cards, holes, check, hints, summary
- Phase 4: XP, streak, reviews and lesson statuses persist across launches
- Phase 5: content generator + level 0 end-to-end on both platforms
