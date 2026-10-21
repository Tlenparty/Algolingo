# Stack Research

**Domain:** Offline, single-user mobile learning app (Duolingo-style algorithm trainer). Kotlin Multiplatform + Compose Multiplatform, Android + iOS, no backend.
**Researched:** 2026-10-21
**Confidence:** HIGH for versions (read directly from Maven Central / Google Maven metadata and the official CMP changelog on 2026-10-21). MEDIUM-HIGH for the "no library" choices (they rest on platform APIs, but they are judgment calls).

## Guiding Principle

The app is a player for static JSON. It also stores a tiny progress blob: a few KB of completed lessons, XP, streak and SRS due dates. Nothing needs SQL, DI, networking, image loading or a syntax-highlighting engine. The dependency list is therefore deliberately short: **Kotlin stdlib + Compose Multiplatform + 4 small libraries** (lifecycle-viewmodel, navigation, kotlinx-serialization, kotlinx-datetime). Everything else is platform API or roughly 30 lines of our own code.

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| Kotlin (KMP plugin + `plugin.compose` + `plugin.serialization`) | **2.4.21** | Language, multiplatform build, Compose compiler, serialization compiler plugin | This is the current stable release (2.5.0 is only at Beta2). The Compose compiler plugin must use the same version as Kotlin. Instant and Clock live in `kotlin.time` and have been stable since 2.3, so they need no opt-in. | HIGH |
| Compose Multiplatform (`org.jetbrains.compose`) | **1.12.2** (Oct 2026) | Shared UI for Android and iOS | Current stable, based on Jetpack Compose 1.12.1. Includes Compose Resources, which is how the bundled JSON gets loaded. | HIGH |
| CMP Material3 (`org.jetbrains.compose.material3:material3`) | **1.12.0-alpha03** | Buttons, cards, theme | The CMP 1.12.x changelog pins this version. Material3 is versioned separately from CMP and always lags as an "alpha". The official Kotlin/KMP-App-Template also uses an alpha material3. The latest stable is 1.9.0, but it is built against older Compose, so use the pinned one. | HIGH |
| Android Gradle Plugin | **9.3.x** (9.3.3) | Android build | Kotlin 2.4.20–2.4.21 is tested against AGP 8.5.2–9.3.1. 9.4.x exists but is outside the tested range. AGP 9 forces the new module layout described below. | HIGH |
| Gradle | **9.7.1** | Build | 9.7.x is the highest Gradle line tested with KGP 2.4.21. The current Gradle is 9.8.2, which would produce compatibility warnings. | HIGH |
| Android SDK | compileSdk/targetSdk **37**, minSdk **24** | — | These match the current official KMP template. | MEDIUM |
| Xcode | **26.4** | iOS simulator build | This is the Xcode version tested with Kotlin 2.4.20+. A simulator is enough and needs no signing. | HIGH |
| JDK | 17 (JetBrains Runtime bundled with Android Studio) | Gradle daemon | Android target uses `JvmTarget.JVM_11` as in the official samples. | MEDIUM |

### Project Template / Module Layout

Generate the project with the **KMP wizard (kmp.jetbrains.com, or the Android Studio / IntelliJ KMP plugin)**. Select Android + iOS with "Share UI" and no server, then delete the sample code.

AGP 9 requires this layout ([kotlinlang AGP 9 migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)):

```
Algolingo/
├── androidApp/   com.android.application. Only MainActivity, about 15 lines.
├── shared/       (wizard may name it composeApp/sharedUI) KMP module:
│                 com.android.kotlin.multiplatform.library + compose + serialization.
│                 ALL code lives here: UI, model, checker, SRS, content loading.
│                 composeResources/files/ holds the lesson JSON.
└── iosApp/       Xcode project (not Gradle). Only the ComposeUIViewController host.
```

- Modules: `shared` (app logic + screens: lesson, kb, home, navigation, content loading), `design` (AlgoTheme design tokens — colors, typography, spacing, sizes, shapes — plus fonts, syntax highlight, and reusable components with no lesson/kb domain types), `androidApp`, `iosApp`. `shared` depends on `design`; `design` depends on nothing in the project. Both `shared` and `design` use `com.android.kotlin.multiplatform.library`. UI code in `shared` reads sizes and colors from `AlgoTheme.*` and does not hard-code dp/sp/`Color(0x...)`.
- The Android config lives in a `kotlin { androidLibrary { namespace = ...; androidResources { enable = true } } }` block. The old `androidTarget()` and `com.android.library` do not work with AGP 9.
- `debugImplementation(uiTooling)` becomes `androidRuntimeClasspath(...)`, because the new plugin has no build variants.

### Supporting Libraries (the complete list)

| Library | Version | Purpose | When to Use | Confidence |
|---------|---------|---------|-------------|------------|
| `org.jetbrains.compose.components:components-resources` | 1.12.2 (same as CMP) | Bundles `composeResources/files/*.json` into both apps. Read with `Res.readBytes("files/content.json")`, which is a suspend function. | Always. This is how the lesson content ships. | HIGH |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | **1.12.0** | Decodes lesson JSON into `@Serializable` data classes. Encodes and decodes the progress blob. Navigation route keys. | Always. Use `Json { ignoreUnknownKeys = true }` so the content generator can add fields without breaking older builds. | HIGH |
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (and `lifecycle-runtime-compose` for `collectAsStateWithLifecycle`) | **2.11.0** | `ViewModel` + `viewModelScope` in commonMain. Create them with `viewModel { LessonViewModel(lessonId, repo) }`. | Always. One ViewModel per screen holds `StateFlow<UiState>`. This is the version paired with CMP 1.12.x. | HIGH |
| `org.jetbrains.androidx.navigation:navigation-compose` | **2.9.2** (stable) | Screen navigation: lesson list → lesson → result. Type-safe `@Serializable` routes, Android back and iOS swipe-back. | Always. See the navigation note below. | MEDIUM-HIGH |
| `org.jetbrains.kotlinx:kotlinx-datetime` | **0.8.0** | `LocalDate`, `TimeZone.currentSystemDefault()`, `plus(DatePeriod(days = 3))` for the 1/3/7-day SRS schedule and the day-streak. | Always. The stdlib `kotlin.time.Instant` alone cannot answer "what calendar day is it locally", and streaks and "due in 3 days" are calendar-day concepts. | HIGH |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.11.0 | Pulled in transitively by Compose and lifecycle. | Do not declare it explicitly unless you need `kotlinx-coroutines-test` (1.11.0) in tests. | HIGH |

**Navigation choice:** Nav2 `navigation-compose` 2.9.2 is the last stable CMP release. 2.10.0 is at rc01 and is the version CMP 1.12.2 lists, so bump to 2.10.0 when it ships stable. Navigation 3 (`navigation3-ui` 1.1.2 stable) is the newer API with a user-owned back stack. It is a valid choice, but it needs a polymorphic `SerializersModule` config for iOS, and it buys nothing for three screens. Nav2 with type-safe routes is less code here. A bare `when(screen)` with no library would lose system back handling and iOS swipe-back, so do not do that.

### Built In-House (no dependency)

| Need | Solution | Why no library |
|------|----------|----------------|
| **Progress persistence** (completed lessons, XP, streak, SRS due dates per task) | A `Progress` `@Serializable` data class serialized to **one JSON string**, stored via an `expect`/`actual` `KeyValueStore`. Android: `SharedPreferences` (`edit { putString(...) }` with `commit()`). iOS: `NSUserDefaults.standardUserDefaults.setObject(...)`. About 15 lines total. `ProgressRepository` keeps a `MutableStateFlow<Progress>` and writes through on every change. | The data is a few KB with no queries, joins or migrations beyond `ignoreUnknownKeys` and default values. Both platforms provide atomic key-value storage natively. See the rejected options below. |
| **Syntax-highlighted Kotlin** | A tiny highlighter: a Kotlin keyword `Set` plus 3 regexes (string literal, number, `//` comment). It produces `SpanStyle`s via `buildAnnotatedString`, applied **per token**. Font: `FontFamily.Monospace`. | The code template is not one text block. It is a sequence of fixed tokens and holes (slots), and each piece is a separate composable, so a whole-text highlighter cannot color it anyway. Coloring per token is trivial, and the content generator could even pre-tag token kinds in the JSON. |
| **Tappable token chips that wrap** | `FlowRow` (compose-foundation, stable since Compose 1.8, no opt-in) holding `Surface`/`AssistChip`-style chips with `Modifier.clickable`. Use it for the answer bank and for the code line with holes. Animate chip moves with `animateContentSize`, or just let recomposition handle it for MVP. | It is built into foundation. Do not use the deprecated `overflow` overloads or `ContextualFlowRow`. |
| **DI** | None. A top-level `AppGraph` object (or a `remember`ed instance passed from `App()`) creates `ContentRepository` and `ProgressRepository` once. ViewModels get them through constructor args in the `viewModel { }` factory lambda. | With 2 repositories and 3 ViewModels, a DI container is pure ceremony. |
| **UI strings** | Hard-coded Russian string literals in composables (or one `Strings.kt` object). | The app is Russian-only (Constraint). Move to `composeResources/values/strings.xml` only if a second locale appears. |
| **Clock for SRS/streak** | Inject `() -> LocalDate` (default: `Clock.System.todayIn(TimeZone.currentSystemDefault())`) into `ProgressRepository`. | Tests can pass a fixed date. No `Clock` abstraction library is needed. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Android Studio (current stable, at least Otter 3 Feature Drop) + Kotlin Multiplatform plugin | IDE, run on emulator and iOS simulator | AGP 9 KMP support needs Otter 3 FD or newer. The KMP plugin adds iOS run configurations. |
| Xcode 26.4 | iOS simulator build and run | Open `iosApp/iosApp.xcodeproj` once. Afterwards it can be run from Android Studio. |
| `kotlin-test` (comes from the Kotlin plugin) | Unit tests in `commonTest` | Tests target: the answer checker (token sequence vs 1–3 references), the SRS scheduler (1/3/7 days), streak math, and JSON decoding of every bundled lesson. These run with `./gradlew :shared:allTests`. |
| `withHostTest {}` inside `androidLibrary {}` | Enables Android JVM unit tests under the new AGP 9 KMP plugin | Without it, there is no `testAndroidHostTest` task. `iosSimulatorArm64Test` runs the same common tests on iOS. |
| Compose UI test (`org.jetbrains.compose.ui:ui-test`, `runComposeUiTest`) | Optional | Skip for MVP. Add when you need a regression test for the tap-chip-into-hole interaction. |
| Compose Hot Reload | — | It is desktop-JVM only. Skip it, because there is no desktop target. |

## Installation

`gradle/libs.versions.toml` (delta over the wizard output):

```toml
[versions]
kotlin = "2.4.21"
agp = "9.3.3"
compose-multiplatform = "1.12.2"
compose-material3 = "1.12.0-alpha03"
androidx-lifecycle = "2.11.0"
androidx-navigation = "2.9.2"        # -> 2.10.0 when stable
kotlinx-serialization = "1.12.0"
kotlinx-datetime = "0.8.0"
android-compileSdk = "37"
android-targetSdk = "37"
android-minSdk = "24"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
lifecycle-runtime-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "androidx-navigation" }
compose-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "compose-material3" }
compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "compose-multiplatform" }

[plugins]
kotlinxSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
# plus wizard's: kotlinMultiplatform, composeCompiler, composeMultiplatform, androidApplication, androidMultiplatformLibrary
```

`gradle/wrapper/gradle-wrapper.properties`: `distributionUrl=...gradle-9.7.1-bin.zip`

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| SharedPreferences / NSUserDefaults + JSON blob | **DataStore KMP** 1.2.1 (stable, Google) with a JSON serializer | When progress grows past about 100 KB, needs Flow-observed partial updates, or multi-process access. It still needs `expect`/`actual` file paths and pulls in okio, so it is more code than the KV approach today. This is the first upgrade path. |
| (same) | **Room KMP** | Only if you need relational queries over history, for example analytics on every attempt. Note that the official docs now show `androidx.room3:room3-*` **3.1.0-alpha01**. That is API churn plus KSP setup for every target. Not for MVP. |
| (same) | **SQLDelight** 2.4.1 | Same trigger as Room. Choose it over Room if you prefer SQL-first schemas. Overkill for a progress blob. |
| (same) | **multiplatform-settings** 1.3.0 | Wraps exactly the SharedPreferences/NSUserDefaults pair. Its last release was Nov 2024 and the repo was last pushed Sep 2025, so maintenance is slowing. Our 15 lines of `expect`/`actual` remove that dependency. Acceptable if you would rather not write `expect`/`actual`. |
| Hand-rolled per-token highlighter | **Highlights** (`dev.snipme:highlights` 1.1.0, KMP, active Aug 2026) | If theory cards later show long, free-form code listings in many languages. For Kotlin-only tokens it is unnecessary. |
| No DI | **Koin** 4.2.2 | When the object graph exceeds about 5–6 collaborators, or when you need scoped instances. Not before. |
| Nav2 `navigation-compose` | **Navigation 3** (`navigation3-ui` 1.1.2) | If you later need adaptive list-detail layouts, or you want the back stack as plain state (for example, deep "repeat at end of lesson" flows across screens). Requires a `SavedStateConfiguration` with a polymorphic `SerializersModule` on iOS. |
| Compose Resources `files/` | Generating Kotlin source (`val lessons = listOf(...)`) from the Obsidian notes | Never. JSON keeps the notes, the generator script and the app decoupled, and lets content be reviewed without rebuilding code logic. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `com.android.library` / `com.android.application` inside the KMP module, `androidTarget {}` | Incompatible with the KMP plugin under AGP 9. The legacy flag `android.enableLegacyVariantApi` goes away in AGP 10. | `com.android.kotlin.multiplatform.library` in shared and design; a separate `androidApp` module |
| Ktor, Coil, any network library | Offline app, no images from the network | Nothing |
| Koin/Kodein/kotlin-inject/Metro | A DI container for about 5 objects | Constructor args + one `AppGraph` |
| Room / SQLDelight for MVP | Schema, KSP/codegen, migrations and a driver per platform, all for a few KB of state | JSON blob in the platform key-value store |
| `kotlinx.datetime.Instant` / `kotlinx.datetime.Clock` | Removed and moved to stdlib in kotlinx-datetime 0.7+. Old tutorials still use them. | `kotlin.time.Instant`, `kotlin.time.Clock` (stable since Kotlin 2.3) plus kotlinx-datetime only for `LocalDate`/`TimeZone` |
| `@OptIn(ExperimentalLayoutApi::class)` + `FlowRow(overflow = ...)`, `ContextualFlowRow` | Deprecated APIs. Plain `FlowRow` is stable. | `FlowRow { }` |
| Java resources (`src/commonMain/resources`) for content | No multiplatform accessor and no iOS bundling | `composeResources/files/` + `Res.readBytes` |
| Enumerating JSON files at runtime | Compose Resources has **no directory listing API** for `files/` | Ship one `content.json` (all of level 0), or an `index.json` that lists lesson file paths |
| Gradle 9.8 / AGP 9.4 / Kotlin 2.5 Beta | Outside the tested KGP 2.4.21 compatibility matrix | Gradle 9.7.1, AGP 9.3.x, Kotlin 2.4.21 |
| Compose string resources for every label | The UI is Russian-only, so they add indirection with no benefit now | Literals, migrated later if localization arrives |

## Stack Patterns by Variant

**If the progress model grows (attempt history, per-token error stats for "грабли" analytics):**
- Move to DataStore (typed, JSON serializer) first. Move to SQLDelight/Room only if you need queries.
- Why: each step adds infrastructure only when a real query or size need appears.

**If v2 adds Parsons (reorder lines) or drag-and-drop:**
- Stay with foundation: `Modifier.pointerInput` / `detectDragGestures`, or the `Modifier.dragAndDropSource/Target` APIs in CMP. Do not add a reorder library until the built-in approach has been tried.

**If on-device compilation/run arrives in v2 (separate VPS per PROJECT.md):**
- Only then add Ktor client (`ktor-client-okhttp` / `ktor-client-darwin`) with `ktor-serialization-kotlinx-json`.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Kotlin 2.4.21 | Gradle 7.6.3–9.7.0, AGP 8.5.2–9.3.1, Xcode 26.4 | From the official KMP compatibility guide. The Gradle 9.7.1 and AGP 9.3.3 patch releases are within those lines. |
| CMP 1.12.2 | Kotlin ≥ 2.1.0 (latest Kotlin always supported), Jetpack Compose 1.12.1 | Compose compiler plugin version = Kotlin version |
| CMP 1.12.2 | material3 1.12.0-alpha03, lifecycle 2.11.0, navigation 2.10.0-rc01 / navigation3 1.2.0-rc01, savedstate 1.4.0 | Pairings from the CMP 1.12.2 changelog. Nav 2.9.2 stable works too. |
| AGP 9.x KMP library plugin | Gradle ≥ 9.1, Android Studio Otter 3 FD+ | No build variants; host tests via `withHostTest {}` |
| kotlinx-datetime 0.8.0 | Kotlin 2.3+ stdlib `kotlin.time.Instant` | Do not mix in the `-0.6.x-compat` artifact |
| iOS min | iOS 14 | CMP requirement; simulator only for MVP |

## Sources

- Maven Central / Google Maven `maven-metadata.xml`, fetched 2026-10-21. Latest versions: kotlin-gradle-plugin (2.4.21 stable, 2.5.0-Beta2), compose-gradle-plugin (1.12.2), AGP (9.4.1 stable, 9.3.3), lifecycle 2.11.0, navigation-compose 2.9.2 / 2.10.0-rc01, navigation3-ui 1.1.2, kotlinx-serialization-json 1.12.0, kotlinx-datetime 0.8.0, coroutines 1.11.0, DataStore 1.2.1, SQLDelight 2.4.1, multiplatform-settings 1.3.0 (Nov 2024), Koin 4.2.2, Highlights 1.1.0. Confidence HIGH.
- https://raw.githubusercontent.com/JetBrains/compose-multiplatform/master/CHANGELOG.md: CMP 1.12.2 (Oct 2026) library pairings, including material3 1.12.0-alpha03. HIGH.
- https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html: Kotlin ↔ Gradle/AGP/Xcode matrix; current stable Kotlin is 2.4.21. HIGH.
- https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html: CMP 1.12.2 minimum Kotlin, iOS 14 / API 21 minimums. HIGH.
- https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html (May 2026): mandatory androidApp split and `com.android.kotlin.multiplatform.library`. HIGH.
- https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html: module layout guidance. HIGH.
- https://github.com/Kotlin/KMP-App-Template `gradle/libs.versions.toml`: the official template uses an alpha material3, compileSdk 37 and minSdk 24, and a separate androidApp module. HIGH.
- https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html: `Res.readBytes("files/...")` is a suspend function; there is no directory listing. HIGH.
- https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html (Jul 2026): Nav3 on CMP needs a polymorphic SerializersModule for non-JVM targets. HIGH.
- https://developer.android.com/kotlin/multiplatform/room (updated 2026-09-09): Room KMP is now documented as `androidx.room3` 3.1.0-alpha01. HIGH.
- https://kotlinlang.org/docs/whatsnew24.html, https://youtrack.jetbrains.com/projects/KT/issues/KT-80778: `kotlin.time.Instant`/`Clock` stable since 2.3. MEDIUM-HIGH.
- https://android-developers.googleblog.com/2025/04/whats-new-in-jetpack-compose-april-25.html: FlowRow/FlowColumn stable in Foundation 1.8. HIGH.

---
*Stack research for: offline KMP + Compose Multiplatform learning app (Algolingo)*
*Researched: 2026-10-21*
