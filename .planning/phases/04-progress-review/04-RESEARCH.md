# Phase 4: Progress & Review - Research

**Researched:** 2026-10-21
**Domain:** Local persistence (DataStore KMP), calendar-date logic (kotlinx-datetime), Leitner 1/3/7 SRS, Home lesson path with statuses
**Confidence:** HIGH (storage API and date API checked against library sources; SRS/streak rules derived from locked requirements)

## User Constraints

There is no CONTEXT.md for this phase (no discuss-phase run yet). The constraints below are already locked in project documents and are quoted from them.

### Locked Decisions (from PROJECT.md Key Decisions, REQUIREMENTS.md, Phase 3 User Decisions)
- **«Прогресс: DataStore KMP, один версионированный JSON-документ — Решение заказчика; атомарная запись из коробки»** (PROJECT.md Key Decisions). REQUIREMENTS PROG-03 says the same: «(DataStore, один версионированный документ)».
- PROG-04: «1 / 3 / 7 дней: ✅ — шаг вперёд, 💡 — шаг на месте, ❌ — сброс на 1 день; ✅ на последнем шаге = «освоено»».
- PROG-02: streak is counted **by the local calendar date**.
- PROG-05: «Повтор сегодня (N)» on Home starts **the same lesson player**.
- XP (Phase 3 User Decision A1, confirmed 2026-10-21): clean = 10, solved with hint(s) or retry = 5, unsolved = 0. Lesson XP is the sum. This is already implemented in `lesson/Xp.kt`. Reuse it; do not redefine it.
- FSRS/SM-2 are out of scope (REQUIREMENTS Out of Scope). Streak freeze is v2 (AIDV2-04). There are no notifications, and lessons cannot be resumed mid-way.
- Home visuals are locked in 03-UI-SPEC §1. Header: `🔥 N` on the left, `⭐ N` on the right. «Повтор сегодня (N)» is a Secondary ChunkyButton that is hidden when N = 0. The path has 32dp gaps between nodes and x-offsets cycling `0, +48dp, 0, -48dp`. Node states:
  - done: green fill with ✅
  - current: green fill with ▶ and the «Начать» pill
  - locked: outline fill with 🔒; tapping does nothing
  - Caption: Label style, 160dp wide, at most 2 lines.
  - Empty state: «Уроков пока нет».
- All UI uses AlgoTheme tokens only (no raw dp/sp literals; Phase 03-10). User-facing text is in Russian.

### Claude's Discretion (researched below, recommendations given)
- Index format for the lesson catalog, and which lessons it lists in Phase 4.
- Whether a ❌ lesson counts as "пройден" for unlocking.
- What a review session plays (whole lesson vs skip theory) and how it is launched.
- Where and how the write happens.

### Deferred (OUT OF SCOPE)
- Lessons 0.1–0.3 content and the content generator (Phase 5).
- Streak freeze, notifications, FSRS, attempt history/analytics.

## Project Constraints (from CLAUDE.md)
- KMP + Compose Multiplatform. The shared UI lives in `:shared`. Design tokens and components live in `:design`.
- Offline, no backend, no network libraries.
- Version pins: Kotlin 2.4.21, CMP 1.12.2, AGP 9.3.3, kotlinx-serialization 1.12.0, lifecycle 2.11.0, navigation 2.9.2. **kotlinx-datetime 0.8.0** is sanctioned for `LocalDate`/`TimeZone`.
- **Do NOT use `kotlinx.datetime.Instant`/`kotlinx.datetime.Clock`.** Use `kotlin.time.Clock` with `todayIn(TimeZone.currentSystemDefault())`.
- Inject `() -> LocalDate` as the clock. No `Clock` abstraction library.
- No DI framework. Use an `AppGraph`/constructor args.
- Room and SQLDelight are listed under "What NOT to Use" for the MVP.
- CLAUDE.md's stack text recommends a SharedPreferences/NSUserDefaults JSON blob and lists **DataStore KMP as "the first upgrade path"**. DataStore is not forbidden. See "Storage decision" below for how this is resolved.
- Don't commit local.properties, secrets or local paths. The repo is public.
- Work goes through the GSD workflow.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| LESS-01 | Level-0 lesson list with «пройден / доступен / закрыт»; the next lesson opens after the previous one is passed | `files/content/index.json` catalog + pure `lessonStatuses()` + Home path UI (Patterns 4, 5) |
| PROG-01 | XP for solved exercises, less with hints/repeats | Existing `xpFor(LessonResult)`; `Progress.xp += xpFor(result)` in `applyLessonResult`; `⭐ N` header |
| PROG-02 | Streak by local calendar date | `LocalDate` from `Clock.System.todayIn(currentSystemDefault())` at write time; pure `bumpStreak` + `visibleStreak` (Pattern 3) |
| PROG-03 | Progress survives restarts (DataStore, one versioned document) | `DataStore<Progress>` via `OkioStorage` + `OkioSerializer` + kotlinx-serialization; `schemaVersion`, defaults, `encodeDefaults`, `ignoreUnknownKeys`, corruption handler (Patterns 1, 2) |
| PROG-04 | Leitner 1/3/7 with ✅ forward / 💡 stay / ❌ reset; ✅ on the last step = mastered | Pure `schedule(prev, grade, today)` keyed by lesson id (Pattern 3) |
| PROG-05 | «Повтор сегодня (N)» starts the lesson player for due tasks | `dueIds(progress, catalog, today)`; `LessonRoute(id, review = true)` → the ViewModel drops theory; the same LessonHost (Pattern 6) |
</phase_requirements>

## Summary

The phase is mostly pure Kotlin. It needs one `@Serializable Progress` document, and three pure functions over it: `applyLessonResult` (XP + streak + SRS), `lessonStatuses` and `dueIds`. All of them take an injected `today: LocalDate` and are tested in `commonTest`. Around that sits a thin storage shell (DataStore) and a rebuilt Home screen. The engine already produces everything needed. `LessonReducer.result(state)` returns a `LessonResult(grade: Grade {Clean, Hinted, Failed}, perExercise)`, and `xpFor` exists. Grade maps 1:1 onto PROG-04: Clean = ✅, Hinted = 💡, Failed = ❌.

**Storage decision.** CLAUDE.md (a SharedPreferences/NSUserDefaults blob) and PROJECT.md/REQUIREMENTS (DataStore) differ, but this is not a real conflict. The customer's DataStore choice is a later, specific, logged decision. CLAUDE.md itself names DataStore as the sanctioned upgrade path and does not ban it. **Use DataStore KMP 1.2.1 typed storage:**
- `DataStoreFactory.create(OkioStorage(FileSystem.SYSTEM, ProgressSerializer) { path })`, with a kotlinx-serialization JSON body.
- I verified from the 1.2.1 sources that `OkioStorage` writes to `<file>.tmp` and then `atomicMove`s it. That is the atomic write the customer asked for.
- Cost versus the KV blob: about 15 extra lines plus 2 Gradle coordinates (already in the local Gradle cache for both iOS targets).

One wrinkle: the official KMP DataStore page says «Only DataStore Preferences is supported in KMP projects». The typed `OkioStorage`/`OkioSerializer` API is still public in `commonMain` of `datastore-core-okio` and is published for `iosArm64`/`iosSimulatorArm64`/`jvm`. I read the published sources. If the planner wants to stay strictly on the documented path, the fallback is Preferences DataStore with a single `stringPreferencesKey("progress")` holding the same JSON. The pure logic and codec are identical either way.

**The two biggest traps both look fine in the emulator.**
1. **The write gets cancelled.** I verified that `DataStoreImpl.updateData` runs the transform `withContext(callerContext)`. If the write is launched in `viewModelScope` and the user taps «Продолжить» quickly, the ViewModel is cleared, the transform is cancelled, and the result is silently lost. Launch writes in an app-lifetime scope owned by the repository.
2. **Two DataStore instances for the same file.** This happens when the store is created inside `remember{}` in `App()` and the Activity is recreated on rotation. DataStore then throws `IllegalStateException`. Hold the store in a process-level singleton.

**Also:** kotlinx-datetime on Android uses `java.time`. With `minSdk 24`, devices on API 24–25 need core library desugaring. The API 36 emulator will not reveal this.

**Primary recommendation:**
- `progress/Progress.kt`: pure model, codec, SRS, streak, statuses and due list, plus tests.
- `progress/ProgressRepository.kt`: a DataStore singleton with an app-scope `record()`.
- A hand-written `files/content/index.json` listing only `l0.4` for now (Phase 5's generator will overwrite it with 0.1–0.4).
- Home rebuilt per UI-SPEC §1, driven by a pure `homeModel(catalog, progress, today)`.
- Review = `LessonRoute(id, review = true)`, which skips theory.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| XP / streak / SRS / statuses / due list | Domain (pure Kotlin, `progress/`) | — | Must be testable with a fake date. No Compose, IO or clock |
| Today's date | Platform edge (`Clock.System.todayIn(currentSystemDefault())`) | — | Injected as `() -> LocalDate`, evaluated at **write time** and at Home render |
| Progress persistence | Storage (DataStore file in app sandbox) | Platform entry (path) | Android `filesDir` needs a Context; iOS uses NSDocumentDirectory. Both are passed in from the entry points |
| Lesson catalog | Bundled resource `files/content/index.json` | Domain (decode/validate) | No directory listing in Compose Resources |
| Home path / header / review button | UI (Compose, `home/`) | `:design` tokens | Stateless render of `HomeModel` |
| Recording a finished lesson | Repository (app-lifetime scope) | LessonViewModel (detects the Done transition) | Must outlive the ViewModel (cancellation trap) |

## Standard Stack

### Core (new in this phase)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.datastore:datastore-core-okio` | 1.2.1 (latest stable; 1.3.0-rc01 exists) | `OkioStorage`, `OkioSerializer`; brings `datastore-core` and okio 3.9+ as API deps | Google's KMP DataStore; atomic tmp+rename write `[VERIFIED: Google Maven metadata + 1.2.1 sources]` |
| `androidx.datastore:datastore-core` | 1.2.1 | `DataStoreFactory`, `CorruptionException`, `ReplaceFileCorruptionHandler` (all in commonMain) | Declare it explicitly. It is transitive from core-okio, but explicit is clearer `[VERIFIED: sources]` |
| `org.jetbrains.kotlinx:kotlinx-datetime` | 0.8.0 | `LocalDate`, `TimeZone`, `Clock.todayIn`, `plus(n, DateTimeUnit.DAY)`, `daysUntil` | Sanctioned by CLAUDE.md. **Not on the classpath yet** (`libs.versions.toml` has no entry) `[VERIFIED: Maven Central metadata, README, sources]` |
| `com.android.tools:desugar_jdk_libs` | 2.1.5 | `java.time` on API 24–25 for kotlinx-datetime | Required by the kotlinx-datetime README for API < 26 `[CITED: github.com/Kotlin/kotlinx-datetime README]` |

### Already present (reuse)
kotlinx-serialization-json 1.12.0 (codec), lifecycle-viewmodel/runtime-compose 2.11.0 (`collectAsStateWithLifecycle`), navigation-compose 2.9.2 (`LessonRoute`), components-resources (`Res.readBytes`), `kotlin.time.Clock` (stdlib).

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Typed `OkioStorage<Progress>` | Preferences DataStore (`datastore-preferences-core` 1.2.1) with one string key holding the JSON | This is the only type the KMP doc page officially "supports". It has the same codec, an extra key-lookup layer, and a less typed `Flow`. Pick it only if the strict-docs reading matters |
| DataStore | SharedPreferences/NSUserDefaults KV blob (CLAUDE.md stack text) | Less code, but it contradicts the customer's logged decision. Don't switch without asking |
| Desugaring | Raise `minSdk` to 26 | One-line change, but it is a product decision (the user's call). Desugaring keeps the current minSdk |

**Installation (`libs.versions.toml` + `shared/build.gradle.kts` commonMain):**
```toml
[versions]
datastore = "1.2.1"
kotlinx-datetime = "0.8.0"
desugar = "2.1.5"
[libraries]
androidx-datastore-core = { module = "androidx.datastore:datastore-core", version.ref = "datastore" }
androidx-datastore-core-okio = { module = "androidx.datastore:datastore-core-okio", version.ref = "datastore" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
desugar-jdk-libs = { module = "com.android.tools:desugar_jdk_libs", version.ref = "desugar" }
```
Desugaring goes into `androidApp/build.gradle.kts`: `compileOptions { isCoreLibraryDesugaringEnabled = true }` + `dependencies { coreLibraryDesugaring(libs.desugar.jdk.libs) }`. The app module dexes all library code, so this covers `:shared`. If AGP's library check complains, also set `enableCoreLibraryDesugaring = true` inside `kotlin { android { } }` in `:shared`, plus a top-level `coreLibraryDesugaring(...)` there `[CITED: kotlinlang Slack thread via WebSearch; MEDIUM]`.

## Package Legitimacy Audit

slopcheck could not be installed in this environment (no `slopcheck` on PATH after pip). Per protocol, packages are tagged `[ASSUMED]`. All four are first-party (Google `androidx.*` / `com.android.tools`, JetBrains `org.jetbrains.kotlinx`), are named in official docs, resolve on their registries, and three are already in `~/.gradle/caches`.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| androidx.datastore:datastore-core(-okio) 1.2.1 | Google Maven | multi-year (1.1.x→1.2.1) | n/a (androidx) | android.googlesource.com/platform/frameworks/support | unavailable | Approved [ASSUMED]; official docs developer.android.com/kotlin/multiplatform/datastore |
| org.jetbrains.kotlinx:kotlinx-datetime 0.8.0 | Maven Central | multi-year | n/a | github.com/Kotlin/kotlinx-datetime | unavailable | Approved [ASSUMED]; in CLAUDE.md stack |
| com.android.tools:desugar_jdk_libs 2.1.5 | Google Maven | multi-year | n/a | github.com/google/desugar_jdk_libs | unavailable | Approved [ASSUMED]; official Android desugaring docs |

**Packages removed:** none. **Flagged [SUS]:** none. Low risk, but the planner may add a human-verify gate per protocol.

## Architecture Patterns

### System Architecture Diagram
```
 App start (MainActivity / MainViewController)
   │  dataDir: filesDir | NSDocumentDirectory
   ▼
 AppGraph.progress(dataDir) ──(process singleton)──► ProgressRepository
   │                                                   ├─ store: DataStore<Progress>  (OkioStorage → progress.json, tmp+atomicMove)
   │                                                   ├─ progress: Flow<Progress>    (store.data)
   │                                                   └─ record(id, result): scope.launch { store.updateData { applyLessonResult(it, id, result, today()) } }
   ▼
 Home composable
   catalog = produceState { loadCatalog() }  ◄── Res.readBytes("files/content/index.json")
   progress = repo.progress.collectAsStateWithLifecycle(null)
   model = homeModel(catalog, progress, today())   (pure: statuses, visibleStreak, xp, dueIds)
   ├─ tap available/done node ──► LessonRoute(id)
   └─ tap «Повтор сегодня (N)» ──► LessonRoute(dueIds.first(), review = true)
                                        │
                                        ▼
 LessonHost → LessonViewModel(load = review ? lesson.copy(theory = []) : lesson)
   reducer runs unchanged … phase becomes Done (once) ──► onFinish(result) ──► repo.record(id, result)
   SummaryScreen shows +xpFor(result) ──► «Продолжить» pops back to Home, which already sees the new Progress via Flow
```

### Recommended Project Structure
```
shared/src/commonMain/kotlin/com/tlenparty/algolingo/
├── progress/
│   ├── Progress.kt            # @Serializable Progress, LessonProgress; encode/decode; applyLessonResult, schedule, bumpStreak, visibleStreak
│   ├── Catalog.kt             # @Serializable Catalog(entries); loadCatalog(); lessonStatuses(); dueIds(); homeModel()
│   └── ProgressRepository.kt  # DataStore creation + ProgressSerializer + record(); AppGraph singleton
├── home/HomeScreen.kt         # rewritten: header, review button, path
├── lesson/LessonViewModel.kt  # + review flag, + onFinish on Done transition
└── App.kt                     # App(dataDir), LessonRoute(id, review = false), wiring
shared/src/commonMain/composeResources/files/content/index.json
shared/src/commonTest/.../progress/ProgressTest.kt, CatalogTest.kt
shared/src/androidHostTest/.../lesson/LessonContentTest.kt   # + every index id has a lesson file
androidApp/.../MainActivity.kt   # App(filesDir.absolutePath)
shared/src/iosMain/.../MainViewController.kt  # App(documentsPath())
design/.../AlgoTheme.kt          # + path offset (48dp) / bottom inset (64dp) tokens if missing
```
Two `progress/` files plus the repository is enough. Don't create separate `Srs.kt`/`Streak.kt` files for 30 lines.

### Pattern 1: One versioned document, defaults everywhere
```kotlin
// progress/Progress.kt
@Serializable
data class Progress(
    val schemaVersion: Int = 1,
    val xp: Int = 0,
    val streak: Int = 0,
    val lastActiveDay: LocalDate? = null,              // ISO "2026-10-21" (LocalDateSerializer is STRING) [VERIFIED: sources]
    val lessons: Map<String, LessonProgress> = emptyMap(), // key = stable lesson id; present = completed at least once
)

@Serializable
data class LessonProgress(val step: Int = 0, val due: LocalDate? = null) // due == null ⇒ mastered

private val progressJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
fun encodeProgress(p: Progress): String = progressJson.encodeToString(p)
fun decodeProgress(s: String): Progress? = try { progressJson.decodeFromString<Progress>(s) }
    catch (e: SerializationException) { null } catch (e: IllegalArgumentException) { null }
```
`encodeDefaults = true` is required. Without it, kotlinx-serialization omits `schemaVersion = 1` and every other default-valued field, so the file carries no version `[ASSUMED: kotlinx-serialization default encodeDefaults=false — well-known; verify by a round-trip test asserting "schemaVersion" in output]`. Future schema changes: add defaulted fields (no migration needed); bump `schemaVersion` and do a `when` in `decodeProgress` only for a breaking change. Unknown lesson ids in the file are ignored at render time (filtered by catalog).

### Pattern 2: DataStore singleton with an app-lifetime write scope
```kotlin
// progress/ProgressRepository.kt
object ProgressSerializer : OkioSerializer<Progress> {
    override val defaultValue = Progress()
    override suspend fun readFrom(source: BufferedSource): Progress =
        decodeProgress(source.readUtf8()) ?: throw CorruptionException("progress.json unreadable")
    override suspend fun writeTo(t: Progress, sink: BufferedSink) { sink.writeUtf8(encodeProgress(t)) }
}

class ProgressRepository(path: String, private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) }) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default) // ponytail: process-lifetime, never cancelled
    private val store = DataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, ProgressSerializer) { path.toPath() }, // path must be absolute (check() in OkioStorage)
        corruptionHandler = ReplaceFileCorruptionHandler { Progress() },             // corrupt file → fresh progress, not a crash loop
    )
    val progress: Flow<Progress> = store.data
    fun today() = today.invoke()
    fun record(id: String, r: LessonResult) { scope.launch { store.updateData { applyLessonResult(it, id, r, today()) } } }
}

object AppGraph { // one DataStore per file per process: survives Activity recreation
    private var repo: ProgressRepository? = null
    fun progress(dataDir: String) = repo ?: ProgressRepository("$dataDir/progress.json").also { repo = it }
}
```
Sources: `OkioStorage(fileSystem, serializer, coordinatorProducer = default, producePath)` and `DataStoreFactory.create(storage, corruptionHandler, migrations, scope)` are common API in 1.2.1 `[VERIFIED: datastore-core(-okio) 1.2.1 multiplatform sources]`. iOS path from the official doc: `NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)!!.path` `[CITED: developer.android.com/kotlin/multiplatform/datastore]`. `Clock` is `kotlin.time.Clock`. Import it explicitly to avoid ambiguity with `kotlinx.datetime.*` `[CITED: kotlinx-datetime README]`.

### Pattern 3: Pure SRS + streak (the whole PROG-02/04 logic)
```kotlin
val INTERVALS = listOf(1, 3, 7)
private fun LocalDate.plusDays(n: Int) = plus(n, DateTimeUnit.DAY)

/** First pass of a lesson (any grade) starts the ladder at +1 day. Reviews only count when due. */
fun schedule(prev: LessonProgress?, grade: Grade, today: LocalDate): LessonProgress = when {
    prev == null -> LessonProgress(0, today.plusDays(1))
    prev.due == null || prev.due > today -> prev           // mastered, or practice replay before due / clock moved back
    grade == Grade.Clean -> if (prev.step == INTERVALS.lastIndex) LessonProgress(INTERVALS.size, null)
                            else LessonProgress(prev.step + 1, today.plusDays(INTERVALS[prev.step + 1]))
    grade == Grade.Hinted -> prev.copy(due = today.plusDays(INTERVALS[prev.step]))
    else -> LessonProgress(0, today.plusDays(1))           // ❌ reset
}

fun bumpStreak(p: Progress, today: LocalDate): Progress {
    val last = p.lastActiveDay
    val s = when {
        last == null -> 1
        last >= today -> maxOf(p.streak, 1)                  // same day, or clock moved back: no change, no break
        last.plusDays(1) == today -> p.streak + 1
        else -> 1
    }
    return p.copy(streak = s, lastActiveDay = if (last != null && last > today) last else today)
}

/** What Home shows: a streak whose last day is before yesterday is already broken. */
fun visibleStreak(p: Progress, today: LocalDate): Int =
    p.lastActiveDay?.takeIf { it >= today.minus(1, DateTimeUnit.DAY) }?.let { p.streak } ?: 0

fun applyLessonResult(p: Progress, id: String, r: LessonResult, today: LocalDate): Progress =
    bumpStreak(p, today).let { it.copy(xp = it.xp + xpFor(r), lessons = it.lessons + (id to schedule(it.lessons[id], r.grade, today))) }
```
Ladder walk-through:
1. First pass → step 0, due +1.
2. ✅ → step 1, due +3.
3. ✅ → step 2, due +7.
4. ✅ → step 3, mastered (due = null).

That is "умею = 3 раза с интервалами 1/3/7" (PROJECT.md Context). 💡 repeats the same interval. ❌ goes back to step 0 at +1. The Grade mapping is already defined by `LessonReducer.result`: any unsolved → Failed, any hint or retry → Hinted.

### Pattern 4: Catalog (`index.json`) forward-compatible with the Phase 5 generator
```json
{ "schema": 1, "lessons": [ { "id": "l0.4", "title": "Бинарный поиск" } ] }
```
- Array order is the unlock order. The caption is `id.removePrefix("l") + " " + title` → «0.4 Бинарный поиск», or add an explicit `caption`.
- Decode with `ignoreUnknownKeys = true` so Phase 5 can add `level`, `file` etc.
- Load it the same way `KbRepository` loads `files/kb/index.json`.
- **Phase 4 lists only `l0.4`.** With linear unlocking, listing 0.1–0.3 first would lock the only playable lesson behind lessons with no files. Phase 5's generator overwrites `index.json` with all four (record this in the Phase 5 handoff).
- Add an `androidHostTest` check: every catalog id has `files/content/lessons/<id>.json`, and ids are unique.

### Pattern 5: Statuses and Home model (pure)
```kotlin
enum class LessonStatus { Done, Available, Locked }
fun lessonStatuses(ids: List<String>, p: Progress): List<LessonStatus> = ids.mapIndexed { i, id ->
    when { id in p.lessons -> LessonStatus.Done; i == 0 || ids[i - 1] in p.lessons -> LessonStatus.Available; else -> LessonStatus.Locked }
}
fun dueIds(ids: List<String>, p: Progress, today: LocalDate) = ids.filter { p.lessons[it]?.due?.let { d -> d <= today } == true }
```
`lessonStatuses` handles a gap safely. If a later lesson is Done while an earlier one isn't (content reordered), the later one stays Done and its successor is Available. UI-SPEC "current" = `Available` (green ▶ + «Начать» pill). Done nodes stay tappable and replay the full lesson. A replay earns XP and streak but does not move SRS unless the lesson is due.

### Pattern 6: Review = same player, theory dropped, write on the Done transition
- `@Serializable data class LessonRoute(val id: String, val review: Boolean = false)`.
- In `LessonViewModel`, load as `load(id).map { if (review) it.copy(theory = emptyList()) else it }`. The reducer is unchanged: `start()` already goes straight to `Solving` when theory is empty.
- Add an `onFinish: (LessonResult) -> Unit` constructor param. In `on(a)`, compare the phase before and after the update; on `!is Done → is Done` call `onFinish(LessonReducer.result(new))` exactly once. A ViewModel survives rotation, so there is no double write.
- `LessonHost(id, review, onFinish, onHome)`. `App` passes `{ repo.record(id, it) }`.
- Review launches `dueIds.first()`. After Summary → Home, N drops by one, and the user taps again for the next. Fine for ≤4 lessons. Skipped: chaining several due lessons into one session. Add that once N regularly > 3.

### Anti-Patterns to Avoid
- **`viewModelScope.launch { store.updateData {…} }` for the final write.** `updateData` runs the transform in the caller's context `[VERIFIED: DataStoreImpl.kt:251,351 in 1.2.1]`, so clearing the ViewModel cancels the write.
- **Creating the DataStore in `remember {}` / in `App()` / per screen.** That gives a second instance after Activity recreation → `IllegalStateException` `[CITED: developer.android.com/topic/libraries/architecture/datastore "Never create more than one instance"]`.
- **`epochMillis / 86_400_000`, `Instant + 24h`, `86400` anywhere.** These compute UTC days, not local ones (PITFALLS #7).
- **Showing the stored `streak` as-is.** After a skipped day it must read 0 before the next lesson (`visibleStreak`).
- **Computing `today` at lesson start.** A lesson started at 23:59 and finished at 00:01 belongs to the new day. Call `today()` inside `record`.
- **Storing content (titles, answers) in Progress.** Only ids, counters and dates go there (PITFALLS #8).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Atomic file write / crash safety | Own tmp-file + rename | `OkioStorage` (does tmp + `atomicMove`) | Verified in sources; serialized writes via actor |
| Local calendar day, +N days | Millis arithmetic, own timezone code | kotlinx-datetime `todayIn`, `plus(n, DateTimeUnit.DAY)` | DST, timezones, month ends |
| Corrupt-file recovery | try/catch around reads in UI | `CorruptionException` + `ReplaceFileCorruptionHandler` | DataStore re-reads cleanly |
| XP numbers | New XP rules | Existing `xpFor` in `lesson/Xp.kt` | Locked user decision A1 |
| Lesson flow for review | A separate review player | Existing `LessonHost`/`LessonReducer` with `theory = emptyList()` | PROG-05 says "тот же плеер" |

## Common Pitfalls

### Pitfall 1: The write is lost when leaving the summary quickly
**What goes wrong:** XP/streak/SRS are not saved, and only sometimes.
**Why:** Caller cancellation propagates into `updateData`'s transform.
**How to avoid:** Use the repository-owned `CoroutineScope(SupervisorJob())`. Record on the Done transition, not on «Продолжить».
**Warning signs:** `viewModelScope` near `updateData`. Flaky "XP didn't increase".

### Pitfall 2: Multiple DataStore instances
**What goes wrong:** A crash on rotation or on returning to the app.
**How to avoid:** `AppGraph` process singleton keyed by path. Also, the Android `@Preview AppAndroidPreview()` in MainActivity.kt calls `App()`. Update or delete it when `App` gains a `dataDir` param.

### Pitfall 3: kotlinx-datetime crashes on API 24–25
**What goes wrong:** `NoClassDefFoundError: java.time.*` on old devices. The API 36 emulator hides it.
**How to avoid:** Core library desugaring in `androidApp` (or raise minSdk to 26, which is a user decision).

### Pitfall 4: The version field is never written
**What goes wrong:** `schemaVersion` is missing from the JSON, so future migrations can't branch.
**How to avoid:** `encodeDefaults = true`. A round-trip test asserts `"schemaVersion":1` is in the output and that `{}` decodes to `Progress()`.

### Pitfall 5: Stale "today" on Home
**What goes wrong:** If the app stays open across midnight, the due count and streak are from yesterday until something recomposes.
**How to avoid (MVP):** Compute `today()` on each Home composition. Progress Flow emissions and navigation back trigger recomposition. `ponytail:` comment: add a `LifecycleResumeEffect` refresh if it ever matters.

### Pitfall 6: Locked lesson reachable / only lesson locked
**What goes wrong:** Listing 0.1–0.3 in Phase 4's `index.json` locks l0.4.
**How to avoid:** Phase 4 catalog = `[l0.4]`. Unit-test unlocking with a fake 4-id list. Phase 5 replaces the index.

### Pitfall 7: Clock moved back
**What goes wrong:** The streak goes negative or resets, `lastActiveDay` moves backwards, or a review gets scheduled in the past.
**How to avoid:** `bumpStreak` keeps the max `lastActiveDay` and does not change the streak when `last > today`. `schedule` leaves a not-yet-due card alone. Tests cover both.

### Pitfall 8: Simulator/emulator data wiped between checks
Only `adb uninstall` or `xcrun simctl uninstall` wipes data; `install -r` / reinstall over the top keeps it. For the restart check, use `adb shell am force-stop com.tlenparty.algolingo` / `xcrun simctl terminate booted com.tlenparty.algolingo` and then relaunch. `[ASSUMED: standard tool behavior]`

## Code Examples

The patterns above contain the code. Two platform snippets remain:
```kotlin
// androidApp MainActivity.onCreate
setContent { App(filesDir.absolutePath) }

// shared/src/iosMain MainViewController.kt   [CITED: developer.android.com/kotlin/multiplatform/datastore]
fun MainViewController() = ComposeUIViewController { App(documentsDir()) }
@OptIn(ExperimentalForeignApi::class)
private fun documentsDir(): String = requireNotNull(
    NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)?.path
)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `kotlinx.datetime.Clock/Instant` | `kotlin.time.Clock/Instant` + `kotlinx.datetime.todayIn` extension on `kotlin.time.Clock` | kotlinx-datetime 0.7.0 | Import `kotlin.time.Clock` explicitly `[VERIFIED: 0.8.0 sources Clock.kt]` |
| Android-only DataStore with `Context.dataStore` delegate | KMP `DataStoreFactory.create(OkioStorage…)` with platform path | DataStore 1.1.0 | Path passed in from the platform entry |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Typed `OkioStorage<Progress>` is fine even though the KMP doc page says "Only DataStore Preferences is supported in KMP" (the API is public and published for iOS) | Summary, Pattern 2 | Low: fall back to Preferences DataStore with one string key; the codec and logic are unchanged |
| A2 | The first pass of a lesson (any grade) starts SRS at step 0, due +1. A replay before the due date doesn't move SRS. A mastered lesson stays mastered on replay | Pattern 3 | Behavioral; the user may want a ✅ first pass to skip to +3. Confirm in discuss/plan |
| A3 | A lesson finished with ❌ still counts as «пройден» (unlocks the next lesson, counts for the streak) — soft mode, no hearts | Pattern 5 | If the user wants solve-to-unlock, change one condition (`grade != Failed`) |
| A4 | Review skips theory and plays all 2–3 exercises; the review button opens the first due lesson only | Pattern 6 | UX preference; cheap to change |
| A5 | Phase 4 `index.json` lists only `l0.4`; Phase 5's generator emits the full `{schema, lessons:[{id,title}]}` | Pattern 4 | Phase 5 must follow this format (note it in the handoff) |
| A6 | `encodeDefaults` is false by default in kotlinx-serialization | Pitfall 4 | None if the round-trip test exists |
| A7 | App-level desugaring in `androidApp` is sufficient for `:shared` code under the AGP 9 KMP library plugin | Standard Stack | Build/lint error → also enable it in the `:shared` android block |

## Open Questions

1. **Does the user accept A2/A3 (SRS start and ❌ unlocking)?** Recommendation: proceed with the defaults and list them in the plan for the user to veto.
2. **Desugaring vs minSdk 26.** Recommendation: desugaring (no product change).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Android emulator | Device check | ✓ | emulator-5554, API 36 | — |
| iOS simulator | Device check | ✓ | iPhone 17, iOS 26.2 (booted) | — |
| idb | iOS taps | partial (`idb_companion` in ~/.local/bin; client used in Phase 3 debug) | — | `xcrun simctl` for launch/terminate; edge swipes need CGEvent (see .planning/debug/resolved/ios-swipe-back-dead.md) |
| JDK | Gradle | ✓ | 21.0.10 | — |
| DataStore 1.2.1 (all targets) | Build | ✓ in Gradle cache | 1.2.1 | network |
| kotlinx-datetime 0.8.0 | Build | partial (jvm/common cached; iOS 0.8.0 klibs not cached) | — | first build needs network |
| slopcheck | Package audit | ✗ | — | [ASSUMED] tags |

**Device verification recipe (orchestrator):**
1. Finish l0.4 and note ⭐/🔥.
2. Force-stop and relaunch the app. Check that ⭐, 🔥, the done node and N are unchanged.
3. Inspect the file: Android `adb shell run-as com.tlenparty.algolingo cat files/progress.json`; iOS `$(xcrun simctl get_app_container booted com.tlenparty.algolingo data)/Documents/progress.json`.
4. To exercise PROG-05 without waiting a day: stop the app, edit `lessons["l0.4"].due` to yesterday in the file (`run-as … sh -c 'cat > files/progress.json'` / write into the container), and relaunch. «Повтор сегодня (1)» must appear and open the lesson **without theory**. After ✅, step → 1 and due = today + 3.

## Validation Architecture

`workflow.nyquist_validation` is `false` in `.planning/config.json`, so this section is skipped. Test notes for the planner:
- `commonTest/progress/ProgressTest.kt` must cover these fake-date cases:
  - 23:59 → 00:01 (`today` = D, then D+1 → streak +1)
  - two lessons the same day → +1 once
  - a skipped day → 1, and `visibleStreak` = 0 before the next lesson
  - clock back → no change, `lastActiveDay` kept
  - the SRS ladder ✅✅✅ → mastered; 💡 keeps the interval; ❌ resets to +1; a not-due replay leaves the card unchanged
  - XP accumulates via `xpFor`
  - codec: `{}` → `Progress()`, unknown keys ignored, garbage → null, round-trip contains `schemaVersion`
- `CatalogTest`: statuses for 4 fake ids (none / first done / gap); `dueIds` filters unknown ids and future dates.
- Run with `./gradlew :shared:testAndroidHostTest` (fast) and `:shared:iosSimulatorArm64Test` (iOS native date code).

## Security Domain

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2/V3/V4 Auth/Session/Access | no | Single local user, no accounts |
| V5 Input Validation | yes | Bundled `index.json` and the on-disk `progress.json` are decoded with kotlinx-serialization and never throw to the UI (null → `CorruptionException` → reset handler); catalog validated by a host test |
| V6 Cryptography | no | Progress is non-sensitive learning data; no encryption |

| Threat | STRIDE | Mitigation |
|--------|--------|-----------|
| Corrupted or hand-edited progress file crashes the app at startup | DoS | `ReplaceFileCorruptionHandler { Progress() }`; decode catches `SerializationException`/`IllegalArgumentException` |
| Progress file leaks via Android Auto Backup | Info disclosure | Accepted (non-sensitive) |

## Sources

### Primary (HIGH)
- Google Maven `maven-metadata.xml` for `androidx.datastore:*` (1.2.1 stable, 1.3.0-rc01) and `com.android.tools:desugar_jdk_libs` (2.1.5); Maven Central for kotlinx-datetime 0.8.0. Fetched 2026-10-21.
- `datastore-core-okio-1.2.1-multiplatform-sources.jar`: `OkioStorage` constructor, the tmp + `atomicMove` write, the absolute-path check, `OkioSerializer`.
- `datastore-core-1.2.1-multiplatform-sources.jar`: `DataStoreFactory.create` (common), `CorruptionException`, `ReplaceFileCorruptionHandler`, `updateData` running the transform in `callerContext` (DataStoreImpl.kt:251, 351), and `Dispatchers.IO` on native.
- `kotlinx-datetime-0.8.0-sources.jar`: `Clock.todayIn` on `kotlin.time.Clock`, `LocalDate` serialized as an ISO STRING, `plus/minus(Int, DateTimeUnit.DateBased)`, `daysUntil`.
- https://developer.android.com/kotlin/multiplatform/datastore (updated 2026-10-12): KMP setup, iOS NSDocumentDirectory path, the "Only Preferences supported" note.
- https://developer.android.com/topic/libraries/architecture/datastore: JSON serializer pattern, the single-instance rule, the corruption handler.
- https://github.com/Kotlin/kotlinx-datetime README: API < 26 desugaring requirement, 0.8.0 coordinates, Clock/Instant migration.
- Codebase: `lesson/LessonReducer.kt` (Grade, LessonResult, `start` skipping empty theory), `lesson/Xp.kt`, `lesson/LessonViewModel.kt`, `lesson/ui/LessonHost.kt`, `App.kt`, `home/HomeScreen.kt`, `kb/KbRepository.kt` (index loading pattern), `design/AlgoTheme.kt` (no 48/64dp spacing tokens yet), `gradle/libs.versions.toml`.

### Secondary (MEDIUM)
- Project research: `.planning/research/ARCHITECTURE.md` (Pattern 3, DataStore doc), `PITFALLS.md` #7/#8/#9/#14, `FEATURES.md` (Leitner 1/3/7), `SUMMARY.md` ("Customer chose DataStore KMP").
- [kotlinlang Slack: coreLibraryDesugaring with KMP](https://slack-chats.kotlinlang.org/t/33217150/hey-anyone-know-how-to-enable-corelibrarydesugaring-with-kmp) and [Android KMP plugin setup](https://developer.android.com/kotlin/multiplatform/plugin): `enableCoreLibraryDesugaring` in the KMP android block plus top-level `coreLibraryDesugaring`.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH (versions from registries, APIs from published sources)
- Architecture: HIGH (reuses existing reducer/VM/host; write-path trap verified in DataStore source)
- SRS/streak rules: MEDIUM-HIGH (derived from locked PROG-04 text; A2/A3 are defaults to confirm)
- Pitfalls: HIGH for cancellation, single instance and desugaring (source/doc verified); MEDIUM for the AGP 9 desugaring placement detail

**Research date:** 2026-10-21
**Valid until:** 2026-11-20

## User Decisions (confirmed 2026-10-22, during plan-phase)

- **A2 confirmed (SRS movement):** only the first pass of a lesson and on-time (due) reviews move the schedule. Replaying a lesson before its due date, or a mastered lesson, is allowed but does NOT change step/due (XP and streak still count).
- **A3 confirmed (❌ lesson):** a lesson finished with grade ❌ counts as «пройден» (unlocks the next lesson, counts for streak); SRS resets to +1 day so it comes back tomorrow.
- **minSdk → 26** (Android 8.0+) instead of core library desugaring. Update androidApp/shared/design minSdk and CLAUDE.md Android SDK row accordingly.
- **Storage:** DataStore (typed, OkioStorage + JSON serializer) per PROG-03; fallback to Preferences DataStore with one JSON string key only if the typed API fails to build/run on iOS. Single process-wide instance via AppGraph; writes in an app-lifetime scope.
- No CONTEXT.md; Home/review visuals per 03-UI-SPEC §1 (new tokens go to :design).
