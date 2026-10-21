<!-- GSD:project-start source:PROJECT.md -->
## Project

**Algolingo (Алголинго)**

Duolingo для алгоритмов: мобильный тренажёр (Android + iOS на KMP + Compose Multiplatform), где решение задачи на Kotlin не печатается, а собирается из облачков-токенов — тап по облачку, и оно встаёт в «дырку» шаблона кода. Урок = короткая теория (карточки) → упражнения от простого к сложному. Сначала для себя (подготовка к алгоритмическим собеседованиям Android-разработчика), позже — в стор.

**Core Value:** Быстро, с телефона, без клавиатуры прорешать алгоритмическую задачу: собрать решение из облачков, нажать «Проверить» и сразу понять, где грабли.

### Constraints

- **Tech stack**: Kotlin Multiplatform + Compose Multiplatform, общий UI для Android и iOS — заказчик Android-разработчик, один код на обе платформы
- **Offline**: всё работает без сети, без бэкенда — бесплатная версия, ноль инфраструктуры
- **Content language**: интерфейс и теория на русском; код на Kotlin
- **iOS**: на этапе MVP достаточно симулятора — нет Apple Developer подписи
- **Repo**: https://github.com/Tlenparty/Algolingo (private), ветка main
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Guiding Principle
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
- Use **one shared module**. Do not split into sharedLogic and sharedUI: that split only matters when some app uses native UI, and here none does ([recommended structure](https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html)).
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
# plus wizard's: kotlinMultiplatform, composeCompiler, composeMultiplatform, androidApplication, androidMultiplatformLibrary
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
| `com.android.library` / `com.android.application` inside the KMP module, `androidTarget {}` | Incompatible with the KMP plugin under AGP 9. The legacy flag `android.enableLegacyVariantApi` goes away in AGP 10. | `com.android.kotlin.multiplatform.library` in shared; a separate `androidApp` module |
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
- Move to DataStore (typed, JSON serializer) first. Move to SQLDelight/Room only if you need queries.
- Why: each step adds infrastructure only when a real query or size need appears.
- Stay with foundation: `Modifier.pointerInput` / `detectDragGestures`, or the `Modifier.dragAndDropSource/Target` APIs in CMP. Do not add a reorder library until the built-in approach has been tried.
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
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
