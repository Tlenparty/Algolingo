# Architecture Research

**Domain:** Offline Duolingo-style algorithm trainer (KMP + Compose Multiplatform, Android + iOS). Code is assembled from token chips into holes and checked locally against 1–3 accepted token sequences.
**Researched:** 2026-10-21
**Confidence:** MEDIUM-HIGH. The module layout and resources API are checked against official docs. The engine, schema and SRS design come from the project's source notes and are design decisions, not ecosystem facts.

## Standard Architecture

The app is a content player. All intelligence lives in a pure-Kotlin engine. Content is static JSON baked into the app, and the only mutable state is one progress document on disk.

### System Overview

```
 BUILD TIME (on the author's Mac, run by hand, output committed to git)
 ┌──────────────────────────────┐   ┌──────────────────────────────┐
 │ Obsidian vault (read-only)   │   │ repo: content/authoring/*.md │
 │ 05 Тренажёр решений.md       │   │ hole markup ⟦…⟧ + distractors│
 │ (condition, hints, solution, │   │ + why + alt answers          │
 │  complexity, pitfalls)       │   │ (hand-written, reviewed)     │
 └──────────────┬───────────────┘   └──────────────┬───────────────┘
                └───────────────┬──────────────────┘
                                ▼
                 tools/gen_content.py  (parse + merge + VALIDATE)
                                ▼
          composeApp/src/commonMain/composeResources/files/content/*.json
 ═══════════════════════════════════════════════════════════════════════
 RUNTIME (composeApp module, commonMain unless noted)
 ┌─────────────────────────────────────────────────────────────────────┐
 │ UI layer (Compose)                                                  │
 │  HomeScreen   LessonScreen{TheoryPager | ExerciseView | Reveal}     │
 │       │              SummaryScreen                                  │
 │       ▼ intents            ▲ StateFlow<UiState>                     │
 │  HomeViewModel        LessonViewModel   (thin: reduce + side effects)│
 ├───────┼──────────────────┼──────────────────────────────────────────┤
 │ Domain (pure Kotlin, no Compose / no IO / no clock)                 │
 │  engine/  ExerciseEngine (place/remove/check)                       │
 │           LessonSession  (queue, retry-at-end, hints, score)        │
 │  progress/ Srs (1/3/7), Streak, Xp   — pure functions of (state,today)│
 ├───────┼──────────────────┼──────────────────────────────────────────┤
 │ Data                                                                │
 │  ContentRepository ── Res.readBytes + kotlinx.serialization (read-only)│
 │  ProgressStore     ── DataStore<Progress> (one JSON doc, atomic)    │
 │                        path via expect/actual (androidMain/iosMain) │
 └─────────────────────────────────────────────────────────────────────┘
   androidApp (MainActivity only)        iosApp (Xcode, MainViewController)
```

### Component Responsibilities

| Component | Responsibility | Implementation |
|-----------|----------------|----------------|
| `gen_content.py` | Parses the Obsidian note and the authoring sidecar, merges them, validates invariants, writes JSON | Single Python 3 stdlib script (re, json). Run by hand. Output is committed |
| Content model | Single schema definition used at runtime | `@Serializable` data classes in `content/Model.kt` |
| `ContentRepository` | Loads `index.json` and lazily loads lesson files, then caches them in memory | `Res.readBytes("files/content/…")` + `Json { ignoreUnknownKeys = true }` |
| `ExerciseEngine` | Places and removes tokens, checks against accepted answers, picks the mistake explanation | Pure functions over immutable data. 100% commonTest |
| `LessonSession` | Runs the lesson: theory, then the exercise queue. A wrong answer goes to the end of the queue. Tracks hints, score and first-try results | Pure reducer `(State, Action) -> State` |
| `Srs` / streak / XP | Next due date (1/3/7 days), mastery, streak update, XP award | Pure functions that take `today: LocalDate` |
| `ProgressStore` | Persists one `Progress` document | `DataStore<Progress>` with `OkioStorage` + JSON serializer |
| ViewModels | Hold `MutableStateFlow`, call reducers, persist on lesson end | `androidx.lifecycle` ViewModel (KMP) |
| Screens | Render state and emit intents. No logic | Compose Multiplatform |
| `AppGraph` | Manual wiring of repository and store. Built once per platform entry | Plain class. No DI framework |

## Recommended Project Structure

**Module count: one shared module + two thin platform shells.** "Single composeApp only" is no longer the minimum on current tooling. With **AGP 9+, the Kotlin Multiplatform plugin cannot be combined with `com.android.application` in one module**. The Android entry point has to live in its own `androidApp` module, and `composeApp` uses `com.android.kotlin.multiplatform.library` (HIGH confidence, from the official kotlinlang AGP 9 migration guide). Do not split further. Separate `engine`/`data`/`ui` modules give nothing at this size. Enforce the boundary through packages and one rule: `engine/` and `progress/` never import `androidx.compose.*` or `org.jetbrains.compose.*`.

```
Algolingo/
├── androidApp/                     # com.android.application: MainActivity + manifest + icon only
├── iosApp/                         # Xcode project; calls MainViewController()
├── composeApp/                     # KMP library (android-kmp-library + iOS framework)
│   └── src/
│       ├── commonMain/kotlin/…/algolingo/
│       │   ├── content/            # Model.kt (@Serializable), ContentRepository.kt
│       │   ├── engine/             # ExerciseEngine.kt, LessonSession.kt   — PURE
│       │   ├── progress/           # Progress.kt (model), Srs.kt, ProgressStore.kt
│       │   ├── ui/                 # App.kt (NavHost), home/, lesson/, components/ (Chip, CodeWithHoles)
│       │   └── AppGraph.kt
│       ├── commonMain/composeResources/files/content/
│       │   ├── index.json
│       │   └── lessons/l0.1.json … l0.4.json
│       ├── commonTest/             # engine + session + srs tests (the bulk of tests)
│       ├── androidHostTest/        # ContentFilesTest: decodes every committed JSON with the real model
│       ├── androidMain/            # actual fun dataStorePath(), AppGraph factory
│       └── iosMain/                # actual fun dataStorePath() (NSDocumentDirectory), MainViewController
├── content/authoring/              # hand markup sidecars, one per task (reviewed by customer)
│   └── l0.4.md
└── tools/gen_content.py            # vault path passed as argument, never hardcoded in Gradle
```

### Structure Rationale

- **`androidApp` separate:** required by AGP 9. Keep it to `MainActivity` and `setContent { App(graph) }`. Note `androidResources { enable = true }` in the `composeApp` `androidLibrary {}` block. Without it, Compose resources can be packed incorrectly after the migration (one community report, MEDIUM confidence).
- **Generated JSON committed to git:** the Gradle build must not depend on `$HOME/Yandex.Disk.localized/...`. CI, a second machine or a Yandex.Disk sync glitch would break builds. Regenerating is an explicit author action, and the diff is reviewable.
- **Sidecar in repo, not in the vault:** the notes stay clean prose for reading in Obsidian. The markup (holes, distractors) is app-specific and versioned with the app.
- **Tests mostly in commonTest:** the engine and session are pure, so tests run on JVM and iOS without emulators.

## Content Schema

Grounded in `05 Тренажёр решений.md` level 0 (fields: condition, 🎯 Тренируем, ⏱ Цель, `[!question]-` hints, `[!success]-` solution, Сложность, ⚠️ Грабли, 🔁 Вариации) and in the `06` ladder idea of one skeleton with progressively more holes ("каждый шаг меняет ровно одну деталь").

**One task from the notes = one lesson.** The lesson's exercises are the **same reference solution with more and more holes** (easy → hard). That gives "от простого к сложному" with almost no extra authoring.

### Runtime JSON (`files/content/lessons/l0.4.json`)

```json
{
  "schema": 1,
  "id": "l0.4",
  "level": 0,
  "title": "Бинарный поиск",
  "source": { "note": "05 Тренажёр решений", "heading": "0.4 Бинарный поиск" },
  "statement": "Найди индекс `target` в отсортированном массиве за O(log n), иначе `-1`.",
  "trains": "каркас бинпоиска и инвариант «ответ всегда внутри [lo, hi]»",
  "targetTimeSec": 180,
  "theory": [
    { "title": "Идея", "body": "Ответ всегда внутри [lo, hi]. Сужаем вдвое.", "code": null },
    { "title": "Три ветки", "body": "нашли / ушли вправо / ушли влево", "code": "when { … }" }
  ],
  "exercises": [
    {
      "id": "l0.4.e1",
      "segments": [
        "fun binarySearch(nums: IntArray, target: Int): Int {\n    var lo = 0\n    var hi = nums.size - 1\n    while (",
        ") {\n        val mid = ",
        "\n        …\n}"
      ],
      "bank": [
        { "text": "lo <= hi" },
        { "text": "lo < hi", "why": "При lo == hi цикл не зайдёт: пропустишь последний кандидат." },
        { "text": "lo + (hi - lo) / 2" },
        { "text": "(lo + hi) / 2", "why": "lo + hi может переполнить Int на больших индексах." }
      ],
      "accepted": [
        ["lo <= hi", "lo + (hi - lo) / 2"],
        ["lo <= hi", "(lo + hi) ushr 1"]
      ],
      "hints": [
        "while (lo <= hi): отрезок из одного элемента тоже надо проверить.",
        "mid = lo + (hi - lo) / 2: защита от переполнения."
      ]
    }
  ],
  "solution": {
    "code": "fun binarySearch(…) { … }",
    "time": "O(log n)", "space": "O(1)",
    "pitfalls": ["`lo < hi` вместо `<=` пропустит последний элемент", "`(lo + hi) / 2` может переполниться"],
    "variations": []
  }
}
```

Schema decisions:
- **`segments` instead of a template string with markers.** `holes = segments.size - 1`. No runtime parsing and no escaping clash with Kotlin's `$`, `{`, `[[`. The human-readable markup lives in the authoring sidecar.
- **One chip fills one hole.** A chip may be multi-word (`lo + (hi - lo) / 2`). This matches the "tap → nearest empty hole" interaction.
- **`accepted` compares by chip `text`, not by id.** Two identical chips (e.g. two `1`) are interchangeable, and alternative answers can reuse bank chips. Only distractors carry `why`.
- **Every `accepted` alternative must be satisfiable from the bank**, and the bank must contain at least one chip that is in no accepted answer, unless the exercise is deliberately distractor-free. The generator enforces both.
- **`solution` / complexity / pitfalls are lesson-level** and shown in the reveal after each exercise is solved. Every exercise in a lesson shares the reference code.
- `schema` int field: the app refuses or ignores unknown future versions. `ignoreUnknownKeys = true` lets new optional fields ship without breaking older builds.
- `index.json`: `{ "schema":1, "levels":[{ "id":0, "title":"Разминка", "lessons":["l0.1","l0.2","l0.3","l0.4"] }] }`.

### Authoring sidecar (`content/authoring/l0.4.md`)

````markdown
---
note: 05 Тренажёр решений
heading: 0.4 Бинарный поиск
---
## e1
```kotlin
    while (⟦lo <= hi⟧) {
        val mid = ⟦lo + (hi - lo) / 2 | (lo + hi) ushr 1⟧
```
- `lo < hi` — При lo == hi цикл не зайдёт: пропустишь последний кандидат.
- `(lo + hi) / 2` — lo + hi может переполнить Int на больших индексах.

## e2
(same solution, more ⟦⟧ holes)
````

The generator reads statement, 🎯, ⏱, hints, solution, complexity and ⚠️ from the note under `heading`. It locates the marked fragment inside the reference solution, splits it into `segments`, and builds `accepted` (`|` = alternatives per hole, combined into at most 3 sequences) and `bank` (correct + alternative + distractor chips). **Drift check:** stripping `⟦⟧` and keeping the first alternative must reproduce the note's solution text (whitespace-normalized). If the note changes, generation fails loudly. Theory cards are written in the sidecar (`## theory`) because the notes have no card structure. They can be seeded from 🎯 and the first hint.

## Architectural Patterns

### Pattern 1: Pure engine with chip-index state

**What:** The exercise state is just "which bank chip sits in which hole". Everything else is derived.
**When:** Always. This is the core and must be 100% unit-tested.
**Trade-offs:** Placement is always "first empty hole". A "selected hole" cursor can be added later as one optional field.

```kotlin
data class ExerciseState(val placed: List<Int?>)        // hole -> bank index
fun ExerciseState.isUsed(bank: Int) = bank in placed

fun place(s: ExerciseState, bank: Int): ExerciseState {
    if (s.isUsed(bank)) return s
    val hole = s.placed.indexOfFirst { it == null }.takeIf { it >= 0 } ?: return s
    return s.copy(placed = s.placed.toMutableList().also { it[hole] = bank })
}
fun remove(s: ExerciseState, hole: Int) = s.copy(placed = s.placed.toMutableList().also { it[hole] = null })

data class Mistake(val hole: Int, val text: String, val why: String?)
data class CheckResult(val correct: Boolean, val holeOk: List<Boolean>, val mistakes: List<Mistake>)

fun check(ex: Exercise, s: ExerciseState): CheckResult {
    val answer = s.placed.map { it?.let { i -> ex.bank[i].text } }
    // compare with the closest accepted variant so per-hole red/green is meaningful
    val best = ex.accepted.maxBy { acc -> acc.indices.count { acc[it] == answer[it] } }
    val ok = best.indices.map { best[it] == answer[it] }
    val mistakes = ok.withIndex().filter { !it.value }.map { (h, _) ->
        Mistake(h, answer[h].orEmpty(), s.placed[h]?.let { ex.bank[it].why })
    }
    return CheckResult(ok.all { it }, ok, mistakes)
}
```

"Проверить" is enabled only when every hole is filled, so the empty-hole case is a UI guard and not a check result. Bank display order is shuffled with `Random(seed)`, where the seed comes from the exercise id. The order is deterministic in tests and stable across recompositions and process death.

### Pattern 2: Lesson session as a reducer

**What:** `LessonSession.reduce(state, action): state`. Actions: `Tap(bank)`, `TapHole(hole)`, `Check`, `Hint`, `Continue`, `NextTheory`. State: `phase` (Theory(i) | Exercise | Feedback(result) | Reveal | Done), `queue: List<String>` of exercise ids, `current: ExerciseState`, `hintsShown`, `scores: Map<id,Int>`, `firstTryOk: Map<id,Boolean>`.
**Retry rule:** wrong check → Feedback(red + `why`). Continue → append the id to the end of `queue` and move on. A retried exercise scores 0 XP (or a reduced amount) and sets `firstTryOk=false`. No lives.
**Hints:** `Hint` reveals `hints[hintsShown++]`. Score = `max(0, BASE - hintsShown * HINT_COST)` (e.g. 10 and 3).
**Trade-offs:** The whole Duolingo flow is testable without coroutines or UI. The ViewModel is ~30 lines.

```kotlin
class LessonViewModel(lesson: Lesson, private val store: ProgressStore, private val today: () -> LocalDate) : ViewModel() {
    private val _state = MutableStateFlow(LessonSession.start(lesson))
    val state: StateFlow<LessonState> = _state
    fun on(a: LessonAction) {
        _state.update { LessonSession.reduce(it, a) }
        if (_state.value.phase is Phase.Done) viewModelScope.launch { store.update { applyLessonResult(it, _state.value.result, today()) } }
    }
}
```

### Pattern 3: Progress as one document, pure update functions

**What:** A single `@Serializable data class Progress(xp, streak: Streak(current, lastDay), lessons: Map<String, LessonProgress(completedOn, bestScore)>, reviews: Map<String, Review(stage: Int, due: LocalDate)>)`, persisted with `DataStore<Progress>` (`OkioStorage` + a JSON `OkioSerializer`; the path comes from expect/actual). DataStore gives atomic writes and serialized updates, so a crash can't corrupt progress. All logic lives in `applyLessonResult(progress, result, today)`.
**SRS (1 / 3 / 7, from the notes' "умею" rule):** the review unit is the **lesson/task**. On the first completion, `Review(stage=0, due=today+1)`. A review passed with every exercise right on the first try and no hints does `stage++`, `due = today + [1,3,7][stage]`. After stage 3 the task is **mastered**. A failed review resets to `stage=0, due=today+1`. Home shows "Повторить (N)" where `due <= today`. Review mode replays the lesson's hardest exercise(s) without theory.
**Streak:** `lastDay == today` → no change. `lastDay == today-1` → +1. Anything else → 1.
**Trade-offs:** A whole-document rewrite on every lesson end is fine for thousands of entries. `today` is injected, so tests never touch the clock. Use `kotlinx-datetime` `LocalDate` with stdlib `kotlin.time.Clock` (kotlinx-datetime 0.7 moved Clock into the stdlib; verify in STACK).

## Data Flow

### Exercise interaction

```
tap chip ─► LessonViewModel.on(Tap(i)) ─► LessonSession.reduce ─► ExerciseEngine.place
                                                    │
UI recomposes ◄── StateFlow<LessonState> ◄──────────┘
tap "Проверить" ─► reduce(Check) ─► ExerciseEngine.check ─► phase=Feedback(result)
   correct ─► Reveal(solution, complexity, pitfalls) ─► Continue ─► next in queue
   wrong   ─► red holes + why ─► Continue ─► id appended to queue end
queue empty ─► Done ─► ProgressStore.update(applyLessonResult) ─► Summary screen
```

### Content load (read-only, one direction)

```
Res.readBytes("files/content/index.json") ─► Json.decode<Index> ─► HomeViewModel
Res.readBytes("files/content/lessons/<id>.json") ─► Json.decode<Lesson> ─► LessonViewModel (cached in repo)
```

### Progress (the only write path)

```
HomeViewModel ◄── store.data: Flow<Progress> (xp, streak, completed, due reviews)
LessonViewModel ──(once, on Done)──► store.updateData { applyLessonResult(it, result, today) }
```

### Navigation

Three destinations with `org.jetbrains.androidx.navigation:navigation-compose` 2.9.x (type-safe `@Serializable` routes; CMP 1.10.x ships 2.9.2): `Home`, `Lesson(id: String, review: Boolean)`, `Summary(...)`. Summary can also be a `Done` phase inside the Lesson screen, which drops it to two routes. Theory, exercise and reveal are **phases inside one Lesson screen**, not routes, so the back-stack never lands mid-lesson. Navigation 3 is still alpha on CMP. Don't use it.

## Scaling Considerations

| Scale | Adjustments |
|-------|-------------|
| Level 0 (4 lessons) | Everything above. Load all from resources. |
| Levels 0–5 (~30 lessons, v1.x) | Unchanged. Per-lesson files already lazy-loaded. Add level gating in `index.json`. |
| Store / many users | Still no backend. New exercise types (Parsons, predict-output) = new `type` discriminator in `exercises` (kotlinx sealed polymorphism) + new engine file + new view. Schema `schema` bump if breaking. |

### Scaling priorities
1. **First pain: authoring cost**, not runtime. The sidecar + drift check + generator validation is the lever.
2. **Second pain: progress migration** when ids or schema change. Never rename lesson/exercise ids. Make `Progress` fields defaulted so old documents decode.

## Anti-Patterns

### Anti-Pattern 1: Logic in composables / ViewModels
**What people do:** Check answers, manage the retry queue or compute streaks inside the ViewModel or `@Composable`.
**Why it's wrong:** It can't be tested in commonTest, and iOS bugs only show up on the simulator.
**Do this instead:** Pure `engine/` + `progress/` functions. The ViewModel only calls `reduce` and persists.

### Anti-Pattern 2: Gradle task that reads the Obsidian vault
**What people do:** Wire generation into `preBuild` with the Yandex.Disk path.
**Why it's wrong:** Builds break on any other machine or CI, or when Disk is offline. Content changes become invisible in diffs.
**Do this instead:** A manual script with the vault path as an argument. Commit the JSON. `androidHostTest` decodes all committed files with the real model and solves each exercise with `accepted[0]` → `check().correct == true`.

### Anti-Pattern 3: Comparing by chip id or by concatenated code string
**What people do:** `accepted` holds bank ids, or the checker compares joined text.
**Why it's wrong:** Ids make duplicate-text chips non-interchangeable and alternatives clumsy. String joining loses per-hole feedback and the distractor `why`.
**Do this instead:** Per-hole text comparison against the closest accepted sequence.

### Anti-Pattern 4: Premature infrastructure
**What people do:** Koin/Hilt, Room/SQLDelight, multi-module clean architecture, a repository interface with one implementation.
**Why it's wrong:** It adds no value for one static read-only content source and one small document.
**Do this instead:** `AppGraph` class, DataStore doc, concrete classes. Add a DB only if progress queries become relational (they won't in v1).

### Anti-Pattern 5: Clock / Random inside the engine
**Do this instead:** Pass `today` and `seed` in. Tests stay deterministic.

## Integration Points

### External
| Service | Pattern | Notes |
|---------|---------|-------|
| Obsidian vault | Read by `gen_content.py` only | Path is an argument. The vault stays the source of truth for prose |
| Platform FS | `expect fun progressFilePath(): String` | Android `context.filesDir`, iOS `NSDocumentDirectory`. One DataStore instance per file (singleton in `AppGraph`) |

### Internal boundaries
| Boundary | Communication | Notes |
|----------|---------------|-------|
| UI ↔ ViewModel | `StateFlow<UiState>` down, `on(Action)` up | Unidirectional |
| ViewModel ↔ engine | Direct pure calls | No coroutines in engine |
| ViewModel ↔ ContentRepository | `suspend` load | Read-only, cached |
| ViewModel ↔ ProgressStore | `Flow<Progress>` read, `update {}` write once per lesson | Only write path |
| Generator ↔ app | JSON files and the Kotlin `@Serializable` model | Contract enforced by `androidHostTest` ContentFilesTest |

## Suggested Build Order

1. **Project skeleton**: `composeApp` (android-kmp-library + iOS) + `androidApp` + `iosApp`. Hello screen on Android and the iOS simulator. One `files/` resource read on both. *Risk retirement first: AGP 9 layout and resources packaging.*
2. **Content model + engine + session** (commonTest-driven), with a hand-written `l0.4.json`. No UI dependency, so it can run in parallel with 1 once Gradle exists.
3. **Exercise screen**: code with inline holes, chip bank, check, red/green, `why`, hints, reveal. Driven by 2.
4. **Lesson flow + Home + navigation**: theory pager → queue → retry at end → summary.
5. **Progress**: DataStore doc, XP, streak, completion, SRS due list and review mode. Needs the session result shape from 2 and 4.
6. **Content pipeline**: sidecar format, `gen_content.py` with validation and drift check, `ContentFilesTest`, author 0.1–0.4, customer review. The schema is frozen by step 2, so this can run in parallel with 3–5.
7. **End-to-end pass** on Android + iOS simulator. Polish (long code lines: horizontal scroll in the code block, chip wrapping).

Dependencies: 2 → 3 → 4 → 5. 6 depends only on 2. 1 gates the platform runs, not the engine tests.

## Sources

- [Updating multiplatform projects with Android apps to use AGP 9 (kotlinlang.org)](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html): HIGH. Separate `androidApp`, `com.android.kotlin.multiplatform.library`, `androidResources { enable = true }`
- [Kotlin agent skill: AGP 9 migration](https://github.com/Kotlin/kotlin-agent-skills/blob/main/skills/kotlin-tooling-agp9-migration/SKILL.md): MEDIUM. New default per-platform app modules
- [Kefe PR: Move Android app into its own module](https://github.com/BcanGRG/Kefe/pull/67): LOW-MEDIUM. Compose resources packing issue, `androidUnitTest` → `androidHostTest`
- [Compose Multiplatform resources usage](https://github.com/JetBrains/kotlin-multiplatform-dev-docs/blob/master/topics/compose/compose-multiplatform-resources-usage.md): HIGH. `Res.readBytes("files/...")`, suspend
- [Setup for multiplatform resources](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html): HIGH
- [What's new in Compose Multiplatform 1.10.3](https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html): MEDIUM. Navigation 2.9.2 bundled, Navigation 3 alpha
- [Set up DataStore for KMP (developer.android.com)](https://developer.android.com/kotlin/multiplatform/datastore): HIGH. `OkioStorage`, one instance per file
- Source notes: `05 Тренажёр решений.md` (level 0, card format, 1/3/7 rule), `06 Движок «маркеры и дырки».md` (incremental ladder over one skeleton, pitfalls as distractors)

---
*Architecture research for: offline token-assembly algorithm trainer (KMP/CMP)*
*Researched: 2026-10-21*
