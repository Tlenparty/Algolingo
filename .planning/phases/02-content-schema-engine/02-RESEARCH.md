# Phase 2: Content Schema & Engine - Research

**Researched:** 2026-10-21
**Domain:** pure-Kotlin domain model + engine in `commonMain` (kotlinx-serialization schema, hole-filling state machine, lesson-queue reducer)
**Confidence:** HIGH (everything is plain Kotlin on already-installed deps; the design is built from the project's own ARCHITECTURE.md and UI-SPEC and checked against the real code layout)

## User Constraints

No CONTEXT.md exists for this phase (`phases/02-content-schema-engine/` was empty). Constraints come from CLAUDE.md and the roadmap:
- Phase is `mode: mvp`. Requirements: LESS-03, LESS-05, EXER-05.
- No new dependencies. Kotlin 2.4.21, kotlinx-serialization-json 1.12.0 and `kotlin-test` are already in `shared/build.gradle.kts`.
- No DI, no network libs. Russian UI text, Kotlin code.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| LESS-03 | A lesson is one task with 2-3 exercises on the same code and a growing hole count | `Lesson.exercises` ladder (1 -> 2 -> 4 holes) and a `LadderTest` that checks hole counts are strictly increasing |
| LESS-05 | Wrong exercises go to the end of the queue (max 2 repeats), the bank is shuffled, no lives | `LessonReducer` with `repeats` map and `seededShuffle`; tests listed below |
| EXER-05 | Chips with identical text are separate objects | Chip instance id = bank index; `placed: List<Int?>` holds ids, never text; duplicate test |
</phase_requirements>

## Summary

The phase is three small pure-Kotlin layers: (1) `@Serializable` content model plus a `validate()` function, (2) `ExerciseEngine` (state + `tapChip`/`tapHole`/`check`), (3) `LessonReducer` (queue, repeats, hints, grade). No platform code and no UI. The one-line shape from project research holds up: *"which bank chip sits in which hole; everything else is derived."* The research adds four things the earlier sketch lacked:

1. The check must be **per hole with three outcomes** (ok / acceptable-with-note / wrong-with-why), so the schema stores a **per-hole answer set**, not whole-answer `accepted` rows. The old `accepted: List<List<String>>` cannot express «принято, но…» per hole (EXER-07) and mixes alternatives across holes.
2. The `why` of a wrong chip can depend on the hole (`mid + 1` is right in `lo = ...` and wrong in `hi = ...`). A chip-level `why` is the default and an optional per-hole `wrongWhy` map overrides it.
3. The reveal screen highlights the hole spans inside the reference solution (UI-SPEC section 7). So the reference per exercise is **derived** from `segments` + `hole.ok[0]`, and a test pins that it equals `solution.code` (this doubles as the CONT-02 drift check in Phase 5).
4. Content tests that read files can only live in `androidHostTest` (same trick as `KbContentTest`); everything else, including the cross-target seeded-shuffle golden test, lives in `commonTest` and so runs on JVM and iOS.

**Primary recommendation:** Put the model in `com.tlenparty.algolingo.lesson` (`Model.kt`, `ExerciseEngine.kt`, `LessonReducer.kt`, `SeededShuffle.kt`), the hand-written `l0.4.json` in `shared/src/commonMain/composeResources/files/content/lessons/l0.4.json`, engine tests in `commonTest`, and a `LessonContentTest` in `androidHostTest` that decodes the file strictly and runs `validate()`.

## Architectural Responsibility Map

Single-tier app (on-device Kotlin; no backend), so tiers here are code layers.

| Capability | Primary layer | Secondary | Rationale |
|------------|--------------|-----------|-----------|
| Content schema + validation | `lesson/Model.kt` (pure) | Phase 5 generator (Python) writes it | Single source of truth; generator output is checked by the same `validate()` in `androidHostTest` |
| Insert/remove chips, check | `ExerciseEngine` (pure functions) | - | Testable on JVM and iOS without UI |
| Queue, repeats, hints, grade | `LessonReducer` (pure) | `LessonViewModel` in Phase 3 (thin: `MutableStateFlow` + `reduce`) | No logic in composables or the ViewModel |
| Bank shuffle | `seededShuffle` (pure, own Fisher-Yates) | - | Determinism across targets must not depend on stdlib internals |
| XP / SRS / persistence | Phase 4 | Engine exposes `LessonResult` facts | Out of scope here |
| Loading JSON | `Res.readBytes` in a 5-line loader | - | Same as `KbRepository`; not unit-testable in commonTest |

## Standard Stack

No new libraries. All from `libs.versions.toml` already (`[VERIFIED: codebase grep shared/build.gradle.kts, libs.versions.toml]`):

| Library | Version | Purpose |
|---------|---------|---------|
| kotlin stdlib (`kotlin.random.Random`) | 2.4.21 | Seeded PRNG |
| kotlinx-serialization-json | 1.12.0 | Model decode with `Json { ignoreUnknownKeys = true }` |
| kotlin-test | 2.4.21 | `commonTest`, `androidHostTest` |

**Package Legitimacy Audit:** no external packages are installed in this phase, so no audit is needed.

## Architecture Patterns

### Data flow

```
l0.4.json (composeResources/files/content/lessons)
   | Res.readBytes + Json{ignoreUnknownKeys}      (Phase 3 loader; tests: File() in androidHostTest)
   v
Lesson ----validate()----> List<String> errors (empty = OK)   [also used by Phase 5 tests]
   |
   v
LessonReducer.start(lesson, seed) -> LessonState
   |  reduce(state, action)   actions: NextTheory, TapChip(id), TapHole(h), Check, Hint, Continue
   |      +-- ExerciseEngine.tapChip / tapHole / check   (per-exercise state)
   |      +-- seededShuffle(bank ids, seed(exerciseId, attempt))
   v
LessonState(phase = Theory | Exercise | Feedback | Reveal | Done, queue, repeats, ...)
   | when phase == Done
   v
LessonState.result(): LessonResult(grade, perExercise: attempts/hints/solved)   -> Phase 3 summary, Phase 4 XP/SRS
```

### Recommended structure

```
shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/
  Model.kt            # Lesson, TheoryCard, Exercise, Hole, Accepted, Chip, Solution + validate()
  SeededShuffle.kt    # seed(exerciseId, attempt), seededShuffle
  ExerciseEngine.kt   # ExerciseState, initial/tapChip/tapHole/check, CheckResult
  LessonReducer.kt    # LessonState, LessonAction, Phase, reduce, grade
  LessonLoader.kt     # suspend fun loadLesson(id): Lesson  (Res.readBytes)
shared/src/commonMain/composeResources/files/content/lessons/l0.4.json
shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/   # EngineTest, DuplicateChipTest, ReducerTest, ShuffleTest, ModelTest
shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
```

File location follows ARCHITECTURE.md (`files/content/lessons/`), parallel to the existing `files/kb/`. Do not create `content/index.json` in this phase (Phase 5 owns the index; YAGNI). `[VERIFIED: ls composeResources/files/kb; ARCHITECTURE.md]`

### Schema (concrete)

```kotlin
@Serializable data class Lesson(
    val schema: Int = 1,
    val id: String,                       // "l0.4" -- stable forever (progress keys on it)
    val level: Int = 0,
    val title: String,
    val statement: String,                // condition, markdown-lite text
    val trains: String? = null,           // the "Trenируем" line
    val targetTimeSec: Int? = null,
    val theory: List<TheoryCard> = emptyList(),
    val exercises: List<Exercise>,
    val solution: Solution,
)
@Serializable data class TheoryCard(      // shape locked by UI-SPEC section 3
    val title: String,
    val body: String = "",
    val signals: List<String> = emptyList(),
    val skeleton: String? = null,
    val pitfalls: List<String> = emptyList(),
)
@Serializable data class Exercise(
    val id: String,                       // "l0.4.e1" stable
    val segments: List<String>,           // fixed code; holes = segments.size - 1
    val holes: List<Hole>,
    val bank: List<Chip>,                 // duplicates allowed (two "1" chips are two entries)
    val hints: List<String> = emptyList(),// empty is valid: no hint button
)
@Serializable data class Hole(
    val ok: List<String>,                 // >= 1; ok[0] is THE reference text
    val acceptable: List<Accepted> = emptyList(),
    val wrongWhy: Map<String, String> = emptyMap(),   // chip text -> why, in THIS hole; overrides Chip.why
)
@Serializable data class Accepted(val text: String, val note: String)
@Serializable data class Chip(val text: String, val why: String? = null)   // why only on distractors
@Serializable data class Solution(
    val code: String, val time: String, val space: String,
    val pitfalls: List<String> = emptyList(), val variations: List<String> = emptyList(),
)
```

Design decisions and why:
- **Per-hole sets, compared by chip `text`** (not id). Two chips with the same text are interchangeable for checking, but distinct as objects for placement. `[ASSUMED]` that the Phase 5 generator can emit `holes` (it emits `ok`/`acceptable` per hole from the `⟦a | b⟧` syntax; this is a straightforward mapping).
- `segments` instead of a marker string (no escaping clash with `$`, `{`). `[CITED: .planning/research/ARCHITECTURE.md]`
- **Defaults everywhere optional** + `ignoreUnknownKeys = true` so Phase 5 can add fields without breaking old builds. The reverse (a typo in hand-written JSON silently ignored) is covered by a strict-decode test (see Pitfalls).
- `Chip.why` should include the counterexample (CONT-02: "объяснение и контрпример"), e.g. `` `max = 0` ломается на `[-5, -2]` ``. The UI shows it as-is.
- No polymorphic `type` discriminator yet (one exercise kind). Add only when a second kind exists.
- Wrong chip with no `why` anywhere is legal (UI shows only "Верно: ..."), but `validate()` should report it for **distractors** (chips that appear in no hole's `ok`/`acceptable`) because CONT-02 requires it.

`validate(lesson): List<String>` checks (return messages, do not throw, so tests print all problems):
1. `segments.size == holes.size + 1`; every hole has `ok.isNotEmpty()`.
2. Every hole's `ok` and `acceptable` texts are present in `bank`; and the whole set of holes is **assignable** from the bank (multiset: with `ok[0]` per hole, each text used at most as many times as it appears in the bank). This is what makes duplicates matter.
3. Every distractor (bank text not in any hole's `ok`/`acceptable`) has `why` (or a `wrongWhy` entry); bank size <= 10 (EXER-02).
4. `segments` joined with `ok[0]` equals `solution.code` for every exercise (drift/reference check; this is the "reveal" invariant).
5. Ladder: 2..3 exercises, hole counts strictly increasing (LESS-03); ids unique and prefixed with the lesson id; `schema == 1`.
6. `acceptable` text not also in `ok` of the same hole.

### The l0.4.json ladder (source: note n07ed966c, section «0.4 Бинарный поиск», lines 122-148 `[VERIFIED: file read]`)

Reference solution `solution.code` (verbatim from the note):

```kotlin
fun binarySearch(nums: IntArray, target: Int): Int {
    var lo = 0
    var hi = nums.size - 1
    while (lo <= hi) {
        val mid = lo + (hi - lo) / 2
        when {
            nums[mid] == target -> return mid
            nums[mid] < target  -> lo = mid + 1
            else                -> hi = mid - 1
        }
    }
    return -1
}
```

Note content to reuse: statement «Найди индекс `target` в **отсортированном** массиве за O(log n), иначе `-1`.»; trains «каркас бинпоиска и инвариант «ответ всегда внутри `[lo, hi]`»»; target 180 s; hint «`while (lo <= hi)`. Середина `mid = lo + (hi - lo) / 2` (защита от переполнения). Три ветки: нашли / ушли вправо / ушли влево.» (split into 2 hints); complexity `O(log n)` / `O(1)`; pitfalls «`lo < hi` вместо `<=` пропустит последний элемент; `(lo + hi) / 2` может переполниться на больших индексах»; no variations in the note (use `[]`).

Recommended ladder (1 -> 2 -> 4 holes):

| Exercise | Holes | Bank (<= 10) |
|----------|-------|--------------|
| e1 | `while (⟦lo <= hi⟧)` | `lo <= hi`, `lo < hi` (why: при lo == hi цикл не зайдёт, пропустишь последний кандидат, напр. `[5]`, target 5), `lo != hi`... keep 3-4 chips |
| e2 | e1 hole + `val mid = ⟦lo + (hi - lo) / 2⟧` with `acceptable: (lo + hi) ushr 1` | + `(lo + hi) / 2` (why: lo + hi переполняет Int при больших индексах), `(lo + hi) ushr 1` (acceptable note: работает, но эталон читаемее и не опирается на трюк с беззнаковым сдвигом) |
| e3 | while cond, `-> return ⟦mid⟧`, `lo = mid + ⟦1⟧`, `hi = mid - ⟦1⟧` | `lo <= hi`, `lo < hi`, `mid`, `1`, `1`, `0` (why: `lo = mid + 0` зацикливается на `[1, 3]`, target 3), `-1`... |

e3 gives the real-content duplicate (`1` x2, which also equals the EXER-05 example). Adding `lo <= hi` -> `lo + (hi - lo) / 2` into e3 would exceed 10 chips; keep the mid line fixed text in e3. Exact distractor wording is for the plan to write; `why` strings must carry counterexamples. `(lo + hi) ushr 1` as `acceptable` is correct: `lo + hi` may wrap negative, but `ushr` reads it as unsigned, so the halving is right. `[ASSUMED: reasoning from two's complement, not run]` - add a test `solve(array, target)` is NOT needed here (CONT-05 is Phase 5), but a human review of the note text is.

Use a `when` branch swap check: `lo = mid + 1` and `hi = mid - 1` are separate holes. `wrongWhy` handles chip `1` placed where... (both holes take `1`, so no override needed in e3). Use `wrongWhy` only where one chip is right in one hole and wrong in another (e.g. if a later exercise puts `mid + 1` / `mid - 1` as whole chips).

### Engine

```kotlin
data class ExerciseState(
    val placed: List<Int?>,     // hole index -> bank index (the chip INSTANCE id); null = empty
    val selected: Int?,         // selected hole, null when none
    val order: List<Int>,       // shuffled display order of bank indices
)
fun initial(ex: Exercise, seed: Int) = ExerciseState(List(ex.holes.size) { null }, 0, seededShuffle(ex.bank.indices.toList(), seed))
```

- **Chip identity = bank index.** `Chip` needs no `id` field in JSON (identity is positional and stable, because bank order in the file is fixed and shuffling only changes `order`). UI uses `key = id` from `order`. Equal text never causes a merge, because nothing in state stores text. Satisfies EXER-05 and the UI-SPEC rule "Compose keys use `id`, never `text`". `[CITED: 03-UI-SPEC.md section 4]`
- `isPlaced(id)` = `id in placed`; ghost = placed chips (bank shows them grey).
- `tapChip(s, id)`: ignore if already placed. Target hole = `s.selected ?: first empty`; if none, no-op. If the target hole was filled, that chip returns implicitly (it is just overwritten, and it is no longer in `placed`). Then `selected` = first empty hole after the target scanning cyclically, else null. Matches UI-SPEC "selection moves to the next empty hole (first empty after the current one, wrapping)".
- `tapHole(s, h)`: if hole is filled: clear it (chip returns to bank) and select `h`; if empty: select `h`. See Open Question 1 for the conflict with the "Filled + selected" visual.
- `check(ex, s): CheckResult?` returns null unless every hole is filled (UI guard EXER-06 enforced in the engine too).

```kotlin
sealed interface HoleResult {
    data object Ok : HoleResult
    data class Acceptable(val text: String, val note: String) : HoleResult
    data class Wrong(val text: String, val why: String?, val correct: String) : HoleResult
}
data class CheckResult(val holes: List<HoleResult>) {
    val verdict: Verdict   // Wrong if any Wrong; else Acceptable if any Acceptable; else Ok
}
fun check(ex: Exercise, s: ExerciseState): CheckResult? {
    if (s.placed.any { it == null }) return null
    return CheckResult(ex.holes.mapIndexed { h, hole ->
        val text = ex.bank[s.placed[h]!!].text
        when {
            text in hole.ok -> HoleResult.Ok
            hole.acceptable.any { it.text == text } ->
                HoleResult.Acceptable(text, hole.acceptable.first { it.text == text }.note)
            else -> HoleResult.Wrong(text, hole.wrongWhy[text] ?: ex.bank[s.placed[h]!!].why, hole.ok[0])
        }
    })
}
```

Per-hole checking is independent: a hole is judged only against its own set. This is what the ladder needs (a swapped `lo = mid - 1` / `hi = mid + 1` makes both holes red). Cross-hole constraints do not exist in l0.4; if a later task needs "these two holes must differ" it becomes a new optional field then.

### Reducer

```kotlin
data class LessonState(
    val lesson: Lesson,
    val phase: Phase,                       // Theory(i) | Exercise | Feedback(check) | Reveal(solved) | Done
    val queue: List<String>,                // exercise ids still to play; head = current
    val attempts: Map<String, Int>,         // exercise id -> number of Checks done
    val hintsShown: Map<String, Int>,       // cumulative per exercise, capped by hints.size
    val solved: Map<String, Boolean>,       // true once a Check is Ok/Acceptable
    val current: ExerciseState?,
    val done: Int,                          // items finished, for the progress bar
    val seed: Int,                          // lesson-level seed base
)
```

Rules (all from LESS-05 + UI-SPEC sections 5, 7, 8):
- `start(lesson, seed)`: `queue = exercises.map { it.id }`, phase = `Theory(0)` if `theory` non-empty else `Exercise`.
- `Check` -> `ExerciseEngine.check`; `attempts[id]++`; verdict Ok/Acceptable -> `solved[id]=true`, phase `Feedback` (panel) ; Wrong -> phase `Feedback`.
- `Continue` from `Feedback`:
  - solved -> phase `Reveal`.
  - wrong and `attempts[id] <= MAX_REPEATS` (i.e. repeats used = `attempts-1 < 2`) -> pop head, **append id to queue end**, start the next item with a fresh `ExerciseState` (new shuffle, see below).
  - wrong and repeats exhausted -> `Reveal(solved=false)` (UI: «❌ Разберём решение»).
- `Continue` from `Reveal` -> pop head, `done++`, next item or `Done`.
- **MAX_REPEATS = 2** means at most 3 attempts per exercise (1 original + 2 repeats). Test: an exercise answered wrong 3 times gets exactly 3 attempts and then `Reveal(unsolved)`; never a 4th.
- If the only remaining item is the failed one, the "end of queue" is the same position: still works (queue = [x]).
- `Hint`: `hintsShown[id] = min(+1, hints.size)`; a no-op when the exercise has no hints, so a lesson with `hints = []` is valid and never breaks (success criterion 4). The UI hides the button when `hints.isEmpty()`.
- Stray actions in the wrong phase (e.g. `TapChip` during Feedback, `Check` twice) return the state unchanged. UI-SPEC says the bank is disabled after Check, and the reducer enforces it so double taps cannot corrupt state.
- Progress bar: `done / (done + queue.size)` (UI-SPEC section 2: counts re-queued items); expose as `val progress: Float`. `[CITED: 03-UI-SPEC.md section 2]`

**Seeded shuffle** (deterministic, KMP-portable):

```kotlin
fun seedOf(exerciseId: String, attempt: Int): Int =
    exerciseId.fold(17) { h, c -> h * 31 + c.code } * 31 + attempt   // explicit; no String.hashCode reliance

fun <T> seededShuffle(items: List<T>, seed: Int): List<T> {
    val r = Random(seed)               // kotlin.random.Random(Int)
    val a = items.toMutableList()
    for (i in a.lastIndex downTo 1) { val j = r.nextInt(i + 1); val t = a[i]; a[i] = a[j]; a[j] = t }
    return a
}
```

`Random(seed: Int)` returns the stdlib XorWow generator, implemented in common Kotlin code, so the sequence is the same on JVM and Native. `[ASSUMED: from kotlin stdlib source knowledge; not re-verified this session]` - that is exactly why the plan must include a **golden test in `commonTest`** (`seededShuffle((0..9).toList(), 42) == <literal>`); the literal is captured once on the JVM and then asserted by `iosSimulatorArm64Test` too, which proves cross-target determinism. Writing Fisher-Yates by hand (4 lines) avoids depending on how `shuffled(Random)` is implemented in future stdlib versions. `seedOf` uses the attempt number so each re-queued exercise gets a different order ("банк перемешивается", LESS-05) while remaining reproducible. `Int` overflow wraps identically on both targets.

**Grade (LESS-06 facts, computed in Phase 2; display in Phase 3):**

Per exercise: `solved`, `attempts`, `hintsShown`. Lesson grade, from the worst exercise:
- ❌ `Grade.Failed`: any exercise unsolved (repeats exhausted).
- 💡 `Grade.Hinted`: all solved, but some exercise had `attempts > 1` or `hintsShown > 0`.
- ✅ `Grade.Clean`: all solved on the first attempt with zero hints.

`acceptable` verdict counts as **solved and clean** (the code works; the note is informational). `[ASSUMED]` - the customer may want «принято, но…» to also drop to 💡; it is a one-line change in `grade()`. Keep `LessonResult(grade, perExercise)` free of XP; Phase 4 owns XP (UI-SPEC says "the XP value comes from the domain"). If Phase 3 needs a number for the reveal headline before Phase 4, put a tiny `xpFor(attempts, hints)` (10 base, -3 per hint, 0 for retried; `[CITED: ARCHITECTURE.md Pattern 2]`) next to `LessonResult`, and let Phase 4 retune.

### Anti-patterns to avoid
- Storing chip **text** (or a text -> position map) in `ExerciseState` - merges duplicates (EXER-05).
- Giving `Chip` an `id` field in JSON "for safety" - positional identity is enough and removes a validation class.
- Letting the ViewModel or composables decide queue/grade.
- Throwing from `reduce`/`check` on bad actions (return the state as is).
- Using `Random.Default` or `List.shuffled()` with no seed anywhere in the engine.
- Putting a hand-written `check` that compares joined strings: it breaks per-hole feedback.

## Don't Hand-Roll

| Problem | Don't build | Use instead | Why |
|---------|-------------|-------------|-----|
| JSON <-> model | custom parser | `Json { ignoreUnknownKeys = true }` + `@Serializable` | Already in the project |
| Resource access | platform file code | `Res.readBytes("files/content/lessons/l0.4.json")` | Same as `KbRepository` |
| Assertions | custom matchers | `kotlin.test` | Present in commonTest |
| Tests with PRNG | mocking Random | golden literal list | Proves cross-target equality directly |

Hand-rolled on purpose (tiny, no suitable lib): Fisher-Yates (4 lines), seed hash (1 line).

## Common Pitfalls

### Pitfall 1: Typos in hand-written JSON silently ignored
**What goes wrong:** `ignoreUnknownKeys = true` hides `"acepted"` and the default `emptyList()` fills in, so a hole has no acceptable answers and the bug shows up in the UI.
**How to avoid:** `LessonContentTest` decodes with a **strict** `Json` (`ignoreUnknownKeys = false`) the file in the repo; the runtime loader stays lenient. Validate with `validate()` in the same test.

### Pitfall 2: Duplicate chips collapsing
**What goes wrong:** `bank.indexOf(text)`, `Map<String, ...>`, `distinct()`, `toSet()` on texts, or Compose `key = text`.
**How to avoid:** all state is Int ids; the test `DuplicateChipTest` places the first `1` chip into hole 1 and the second into hole 2 and checks (a) both holes are filled, (b) `isPlaced(id0)`/`isPlaced(id1)` are independent, (c) returning one leaves the other placed, (d) tapping an already-placed chip is a no-op, (e) check returns Ok for both. Use a synthetic exercise built in Kotlin (two `mid`, two `1`) so the test does not depend on the content file.

### Pitfall 3: Reading resource files from commonTest
**What goes wrong:** `commonTest` has no `File` and `Res.readBytes` is suspend and needs the compose-resources test setup.
**How to avoid:** engine/reducer tests build `Lesson`/`Exercise` objects in Kotlin or decode inline JSON strings (these run on JVM and iOS). File-based tests go in `shared/src/androidHostTest/...` with `File("src/commonMain/composeResources/files/content/lessons/l0.4.json")` - the working directory is the module dir, exactly as `KbContentTest` does with `File("src/commonMain/composeResources/files/kb")`. `[VERIFIED: KbContentTest.kt]`. The iOS side of the real file is covered when Phase 3 loads it in the app (and Phase 5 end-to-end); a decode-from-inline-JSON test in `commonTest` that mirrors one exercise covers the serialization plugin on iOS.

### Pitfall 4: Off-by-one in "max 2 repeats"
**What goes wrong:** 2 repeats read as 2 attempts, or as 3 repeats.
**How to avoid:** define `MAX_REPEATS = 2` -> 3 attempts. Unit test with a `fun failThrice()`.

### Pitfall 5: Selection state after remove/replace
**What goes wrong:** `selected` points at a stale hole, or `null` and the next tap goes nowhere.
**How to avoid:** `tapChip` falls back to "first empty" when `selected == null`; after the last empty hole is filled `selected = null`; tests: fill all holes, remove one (selected = that hole), fill again.

### Pitfall 6: The reference drift
**What goes wrong:** `solution.code` and the holes disagree (someone edits one).
**How to avoid:** validation rule 4 (segments + `ok[0]` == `solution.code` for every exercise). Keep whitespace identical, including the aligned `->` in the `when`. In JSON the code needs `\n` escapes; build the file with a script or a Kotlin raw string while authoring, never by hand-escaping Cyrillic pitfalls on one line.

### Pitfall 7: `acceptable` that is actually wrong, or `ok` that is not the reference
**How to avoid:** only `ok[0]` is the reference; extra `ok` entries are alternatives of equal quality. Verifying acceptable variants on examples is CONT-05 (Phase 5), but the hand-written l0.4 should be reviewed once against the note by the customer.

## Code Examples

### Strict file test (androidHostTest, mirrors KbContentTest)

```kotlin
class LessonContentTest {
    private val file = File("src/commonMain/composeResources/files/content/lessons/l0.4.json")
    @Test fun l04IsValid() {
        val lesson = Json { ignoreUnknownKeys = false }.decodeFromString<Lesson>(file.readText())
        assertEquals(emptyList(), validate(lesson))
        assertTrue(lesson.exercises.size in 2..3)
    }
}
```

### Reducer smoke test shape (commonTest)

```kotlin
@Test fun wrongGoesToEndMax2() {
    var s = LessonReducer.start(lessonOf("a", "b"), seed = 1)       // helper builds a 2-exercise lesson
    repeat(3) { s = failCurrent(s) }                                 // helper: fill wrong, Check, Continue
    // a failed 3 times -> unsolved, not queued again
    assertEquals(3, s.attempts["a"]); assertNotEquals(Grade.Clean, LessonReducer.result(s).grade)
}
```

### Test list for the plan (all `kotlin-test`, `./gradlew :shared:allTests` or `:shared:testAndroidHostTest` + `:shared:iosSimulatorArm64Test`)

| Success criterion | Test | Location |
|-------------------|------|----------|
| 1 | l0.4 decodes strictly, `validate()` empty, 2-3 exercises, hole counts increasing | androidHostTest |
| 2 | place/remove/replace/selection tests; check yields Ok, Acceptable(note), Wrong(why) per hole; check null while a hole is empty; per-hole independence (swapped branches -> two Wrong) | commonTest |
| 3 | duplicate chips test (Pitfall 2) | commonTest |
| 4 | wrong -> end of queue; max 3 attempts; progress fraction counts re-queued; no-hints lesson valid and `Hint` is a no-op; hint cap; grade Clean / Hinted / Failed; shuffle golden + different seed per attempt + same seed same order; stray actions ignored | commonTest |

## State of the Art

| Old / earlier sketch | Use now | Why |
|----------------------|---------|-----|
| `accepted: List<List<String>>` whole-answer rows (ARCHITECTURE.md) | per-hole `ok` + `acceptable(note)` + `wrongWhy` | EXER-07 needs a per-hole acceptable note; independent holes |
| `body`+`code` theory card | `signals/skeleton/pitfalls` card (UI-SPEC section 3) | UI-SPEC is later and locked; keep `body` as an optional free-text |
| `placed: List<Int?>` with only first-empty placement | add `selected` hole + cyclic next-empty | UI-SPEC section 4 selection rules |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `kotlin.random.Random(Int)` yields an identical sequence on JVM and Kotlin/Native | Seeded shuffle | Shuffle differs per platform; mitigated by the golden test running on both targets (fails loudly, then fall back to a 5-line LCG) |
| A2 | `acceptable` counts as a clean solve for the grade | Grade | Customer wants 💡; one-line change |
| A3 | Phase 5 generator can emit per-hole `ok`/`acceptable`/`wrongWhy` from `⟦a | b⟧` markup | Schema | Generator spec in Phase 5 adjusts; schema is additive |
| A4 | `(lo + hi) ushr 1` is correct for non-negative `lo`,`hi` < 2^31 | l0.4 content | Wrong acceptable note; customer review (CONT-06) |
| A5 | Ladder shape 1 -> 2 -> 4 holes with the chips in the table | l0.4 | Pure content; plan may tune |
| A6 | XP stays out of Phase 2 (only a tiny optional `xpFor`) | Grade | Phase 3 reveal needs a number; solved by the optional helper |

## Open Questions (RESOLVED — see "User Decisions" at the end: Q1 tapHole returns chip, Q2 no ShowAnswer, Acceptable = clean ✅)

1. **Tap on a filled hole: return the chip, or only select the hole?**
   - Known: EXER-04 says tapping an inserted chip returns it; UI-SPEC lists both "tap hole -> selected (empty or filled)" and "tap placed chip in a hole -> returns".
   - Unclear: the filled hole is the same hit target as its chip; the "Filled + selected" visual then occurs only via a different path.
   - Recommendation: `tapHole` on a filled hole returns the chip **and** selects the hole (what the engine research above implements). Planner: note in the Phase 3 hand-off that "Filled + selected" is effectively unreachable and can be dropped.
2. **Reveal after a wrong answer with repeats left:** UI-SPEC goes straight to the next item (no reveal); the reference is only shown when repeats are exhausted. Implemented this way; no action unless the customer disagrees.
3. **Is a «Показать ответ» action needed?** The ❌ definition in LESS-06 says «открыл ответ», but UI-SPEC has no such button. Recommendation: no action in Phase 2 (YAGNI); exhausting repeats is the only ❌ path. Adding `Reveal` action later is additive.

## Environment Availability

| Dependency | Required By | Available | Notes |
|------------|------------|-----------|-------|
| Gradle wrapper + JDK | all tests | yes (Phase 1/1.1 verified green) | `[CITED: 01.1-VERIFICATION.md]` |
| Xcode / iOS simulator | `iosSimulatorArm64Test` (cross-target golden test) | macOS host; Phase 1 verified iOS build | Run once at the phase gate; if the simulator is not bootable, state it in the summary rather than skipping silently |

## Security Domain

| ASVS | Applies | Control |
|------|---------|---------|
| V5 Input validation | Only for bundled, repo-controlled JSON | Strict decode in tests; `validate()`; engine never throws on bad actions |
| V2/V3/V4/V6 | No | No accounts, network or crypto in this phase |

Threat surface is nil (offline, trusted bundled content). The one real risk is a malformed content file crashing the lesson at runtime: `validate()` in `androidHostTest` prevents it from shipping.

## Sources

### Primary (HIGH)
- Local files read this session: `.planning/ROADMAP.md` (Phase 2), `REQUIREMENTS.md`, `research/ARCHITECTURE.md` (schema + engine + reducer sketches), `phases/03-lesson-exercise-ui/03-UI-SPEC.md` (sections 2-8), `shared/build.gradle.kts`, `shared/src/androidHostTest/.../KbContentTest.kt`, `kb/KbRepository.kt`, `kb/index.json`, and note `kb/n07ed966c.md` (0.4 binary search).
- `CLAUDE.md` STACK section (Res.readBytes, `Json { ignoreUnknownKeys }`, no DI, `kotlin-test`).

### Tertiary (LOW)
- Kotlin stdlib `Random(seed)` implementation detail (A1) - from training knowledge; verified by test, not by docs.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - nothing new.
- Architecture/schema/reducer: HIGH - derived from project docs and the UI contract; it is a design, not an ecosystem fact.
- Pitfalls: HIGH for logic, MEDIUM for the PRNG parity (A1, guarded by a test).

**Research date:** 2026-10-21
**Valid until:** 2026-11-20 (stable; revisit only if the UI-SPEC changes)

## Project Constraints (from CLAUDE.md)

- KMP + Compose Multiplatform, one shared module; engine goes into `shared/src/commonMain` (package `com.tlenparty.algolingo`).
- Offline; no backend, no network or DI libraries; no new dependencies.
- JSON content via Compose Resources `files/` + `Res.readBytes`; `Json { ignoreUnknownKeys = true }`.
- Tests: `kotlin-test` in `commonTest`; `androidHostTest` for file-reading tests; run via `./gradlew :shared:allTests`.
- UI text Russian, code Kotlin. Do not commit `local.properties`/secrets.
- Work through a GSD command (edits only inside the GSD workflow).

## User Decisions (confirmed 2026-10-21, during plan-phase)

- **A2 / Q3 confirmed:** «принято, но…» (Acceptable) counts as a clean solve — grade stays ✅ if first try and no hints; the note is shown but does not lower the grade.
- **Q1 (orchestrator default, recommended):** `tapHole` on a filled hole returns the chip to the bank and selects that hole. Phase 3 must be told ("Filled + selected" state is unreachable).
- **Q2 (orchestrator default, YAGNI):** no «Показать ответ» action this phase; ❌ only when repeats are exhausted.
- No CONTEXT.md / discuss-phase; no UI-SPEC needed (engine-only phase, no screens).
