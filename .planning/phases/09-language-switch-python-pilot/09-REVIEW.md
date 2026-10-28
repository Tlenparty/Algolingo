---
phase: 09-language-switch-python-pilot
reviewed: 2026-10-28T14:55:22Z
depth: standard
files_reviewed: 27
files_reviewed_list:
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/CodeLang.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/Highlight.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/LangSegment.kt
  - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonRepository.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/LessonViewModel.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/LessonHost.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/CodeTemplate.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/ResultPanel.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/design/HighlightTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonRepositoryTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/LessonViewModelTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/CatalogTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressRepositoryTest.kt
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/progress/ProgressTest.kt
  - shared/src/androidDeviceTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
  - shared/src/androidHostTest/kotlin/com/tlenparty/algolingo/lesson/LessonContentTest.kt
  - shared/src/iosTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
  - tools/gen_content.py
  - tools/test_gen_content.py
findings:
  critical: 1
  warning: 5
  info: 6
  total: 12
status: issues_found
fix_status: partial
fixed: [CR-01, WR-02, WR-03, WR-04, WR-05]
deferred: [WR-01]
---

# Phase 9: Code Review Report

**Reviewed:** 2026-10-28T14:55:22Z
**Depth:** standard
**Files Reviewed:** 27
**Status:** issues_found

## Summary

I reviewed the phase 9 diff (`4cf70ea..HEAD`), which covers CodeLang, highlighting, LangSegment, language-aware catalog/home, lessonPath, the Progress.lang serializer, setLang, AppGraph keying, the Python generator gates, and the tests. The single branch point for language and the shared reducer are clean. The status chain in `lessonStatuses` behaves as specified, and `tools/test_gen_content.py` passes (74 tests, OK; it writes only to temp roots, and the repo was untouched).

There is one ship-blocker. A lesson that is Done but not shipped in the selected language stays tappable, and tapping it opens a Python file that does not exist, so the user lands on the error screen. This is the normal path for anyone who already finished Kotlin lessons and then switches to Python. The remaining findings cover gaps in the generator's verification, the robustness of the lang decoder, and test fragility.

## Critical Issues

### CR-01: Done lesson without the selected language opens a missing file (error screen)

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt:528` (status), `shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt:142-145` (tap), `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:154` (route)

**Issue:** `lessonStatuses` returns `Done` before it checks `available` (`id in p.lessons -> Done`). `CatalogTest.doneLessonWithoutPythonStaysDone` locks this in. The `Done` node is clickable (`Node(..., true, start, ...)`). `HomeTab.onStart` always navigates with `lang = p.lang.id`. Here is what happens for a user who passed l0.1–l1.3 in Kotlin and then taps "Python":
1. All of those nodes show ✅ "Пройден, повторить".
2. Tapping one creates `LessonRoute("l1.1", lang="python")`.
3. `lessonPath` returns `files/content/lessons/py/l1.1.json`, which does not exist.
4. `Res.readBytes` throws, and `LessonErrorScreen` appears.

This violates the locked decision that a lesson without the selected language does not open (T-09-10 guarded only `Soon`). The e2e `scenarioPython` starts from fresh progress, so it never reaches this path.

**Fix:** Have the model decide which language each node opens in, so the UI never sends a language the lesson lacks. One option is a flag on `HomeNode` computed in `homeModel`, which is already the allowed branch point:
```kotlin
data class HomeNode(val id: String, val caption: String, val status: LessonStatus, val inLang: Boolean = true)
// homeModel:
val nodes = catalog.lessons.zip(lessonStatuses(ids, p, available)) { e, s ->
    HomeNode(e.id, e.caption ?: (e.id.removePrefix("l") + " " + e.title), s, inLang = available(e.id))
}
// HomeTab.onStart: replay a Done-off-language lesson in Kotlin (it always ships)
onStart = { id -> val n = model?.nodes?.find { it.id == id }
    entry.ifResumed { nav.navigate(LessonRoute(id, lang = if (n?.inLang != false) lang.id else CodeLang.Kotlin.id)) } }
```
Alternatively, render Done + `!inLang` as non-clickable ✅. Either way, add a CatalogTest or e2e case: Kotlin progress → switch to Python → tap a Done Kotlin-only node, and expect no error screen.

## Warnings

### WR-01: G13 does not distinguish a semantic failure from SyntaxError, timeout, or crash; Python kind tags and counterexamples are unverified

**File:** `tools/gen_content.py:708-718`

**Issue:** `python_gates` accepts a wrong program if it fails in any way: a SyntaxError at module compile, a NameError, a timeout, or a failed assert. The `[compile]/[crash]/[wrong]/[hang]` tags are only G9-shape-validated. Unlike the Kotlin path (Probe kinds + WRONG_JSON), nothing checks that the program really fails the way its why claims. For example, `content/authoring/py/l1.4.md:115` `@3 enumerate(nums) [wrong]` claims "→ None", and a crash or SyntaxError would pass G13 just as well. `test_hanging_wrong_chip_is_killed_and_counts_as_failing` passes a chip tagged `[hang]` in a hole whose real outcome is not checked. As a result, wrong explanations shown to the learner can ship. That is the "грабли" content the product's core value rests on.

**Fix:** Have the harness classify the outcome and compare it with the claim tags:
- `compile`: `compile(src, ...)` raises SyntaxError.
- `hang`: TimeoutExpired.
- `crash`: an exception other than AssertionError inside `case`.
- `wrong`: AssertionError only.

Record the kind in `_fails` (for example, prefix `t.__name__`), print the set of kinds, and raise G13 when it does not intersect `e["claims"]` tags.

### WR-02: `CodeLangSerializer` still resets all progress on a non-string `lang`

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt:590-597`

**Issue:** `decoder.decodeString()` on `"lang": null` or `"lang": 1` throws `JsonDecodingException` in the tree decoder (`decodeProgress` uses `decodeFromJsonElement`, non-lenient). `decodeProgress` then returns null, `ProgressSerializer` raises CorruptionException, and `ReplaceFileCorruptionHandler { Progress() }` wipes XP, streak, and lessons. The locked decision says "`decodeProgress` не должен возвращать null из-за `lang`"; only string values are covered.

**Fix:**
```kotlin
override fun deserialize(decoder: Decoder): CodeLang {
    val id = (decoder as? JsonDecoder)?.decodeJsonElement()?.let { (it as? JsonPrimitive)?.contentOrNull } ?: decoder.decodeString()
    return CodeLang.entries.firstOrNull { it.id == id } ?: CodeLang.Kotlin
}
```
Then add `ProgressTest` cases for `"lang":null` and `"lang":1`.

### WR-03: AppGraph abandons the previous DataStore without closing it; switching back to an earlier dir crashes

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt:641-649`

**Issue:** When `dataDir` changes, the old `ProgressRepository` is dropped. Its `progressStore` scope (`CoroutineScope(Dispatchers.Default + SupervisorJob())`) is never cancelled, so its DataStore stays registered as active on its file. A sequence A → B → A creates a second DataStore on `A/progress.json`, which is the exact "two on one file crash" the KDoc warns about. Production passes a constant dir, but the e2e suite now depends on this path. Adding a third `@Test` that reuses a dir, or any future test that alternates dirs, will hit it.

**Fix:** Key by a map (`mutableMapOf<String, ProgressRepository>()` with `getOrPut(dataDir)`), so a returning dir reuses its store. Another option is to cancel the old store's scope before replacing it.

### WR-04: e2e `scenarioPython` will fail by design once phase 10 ships Python for every lesson

**File:** `shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt:285`

**Issue:** `py.firstOrNull { it.status == LessonStatus.Soon } ?: fail("every lesson has Python: nothing to show as Soon")`. Phase 10's goal is to translate the remaining 8 lessons, and after that no Soon node exists. The scenario then fails on both Android and iOS. The final `assertEquals(0, ...Скоро на Python...)` check is also vacuous at that point.

**Fix:** Make the Soon part conditional (`soon?.let { onHome(...) }`), or build the Soon assertion from a synthetic catalog in `CatalogTest` and keep the e2e focused on switch → play → persist → shared progress.

### WR-05: SD tab routes lessons with the Home language

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:289,294,297`

**Issue:** `lang = p?.lang ?: Kotlin` is computed even when `byLang = false`. SD lessons therefore get `LessonRoute(..., lang = "python")`, `LocalCodeLang provides Python`, and a different ViewModel key in Python mode. This currently has no visible effect, because SD renders with `syntax = false`. It contradicts "SD без фильтра по языку" and silently changes SD rendering the moment any SD screen enables syntax.

**Fix:** `val lang = if (byLang) p?.lang ?: CodeLang.Kotlin else CodeLang.Kotlin`.

## Info

### IN-01: "Скоро на Python" is hardcoded to Python

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt:114`

**Issue:** `SOON` is a constant that names Python. In Kotlin mode, a catalog entry lacking `"kotlin"` (for example `langs: ["python"]`, or a typo) shows as "Скоро на Python". A third language would be mislabeled too, which breaks the rule "third language = enum + highlight rules + folder".

**Fix:** Pass `lang` into `PathNode` and use `"Скоро на ${lang.name}"`.

### IN-02: Duplicate id → CodeLang lookup

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:270`, `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Progress.kt:595`

**Issue:** `CodeLang.entries.firstOrNull { it.id == x } ?: CodeLang.Kotlin` appears twice.

**Fix:** Add `companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Kotlin }` to `CodeLang`.

### IN-03: The Python harness prelude can shadow lesson names

**File:** `tools/gen_content.py:683-685`

**Issue:** The prelude (`case`, `pw`, `_fails`, `_sys`) is inserted after the variant code. A lesson that defines `case` or `pw` (plausible names in later lessons) has its function silently replaced, and the checks then test the wrong function.

**Fix:** Put the prelude before the code, or use dunder-prefixed names.

### IN-04: The Python sidecar `title:` never reaches index.json

**File:** `tools/gen_content.py:747`

**Issue:** Index entries always come from the Kotlin item, so a Python-specific title is shown inside the lesson but not on Home.

**Fix:** Either document that the title must match the Kotlin one, or raise G7 when the Python and Kotlin titles differ.

### IN-05: The Python keyword set misses common control-flow words

**File:** `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/Highlight.kt:31-34`

**Issue:** `try`, `except`, `finally`, `raise`, `assert`, `del`, `global`, and `nonlocal` are not highlighted. The set matches the spec list exactly, but phase 10 lessons are likely to need some of these.

**Fix:** Extend the set when phase 10 content uses them.

### IN-06: LangSegment rewrites progress when the already-selected language is tapped

**File:** `design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/LangSegment.kt:132`

**Issue:** `selectable { onSelect(lang) }` fires even when `on == true`, which causes a redundant DataStore write. Any write failure then shows the save-failed banner for a no-op.

**Fix:** `{ if (!on) onSelect(lang) }`.

## Fix Status

| Finding | Status | Commit |
|---------|--------|--------|
| CR-01 | fixed: `Catalog.routeLang` — a lesson opens in the selected language only if its catalog entry has it, else Kotlin (Done lessons stay Done and replay in Kotlin); CatalogTest | b1b2505 |
| WR-05 | fixed with CR-01: SD tab (no filter) always routes Kotlin | b1b2505 |
| WR-02 | fixed: `CodeLangSerializer` reads the JSON element; null/number/object → Kotlin, progress kept; ProgressTest | d4b31bf |
| WR-03 | fixed: AppGraph caches one repository per dataDir (A→B→A reuses A); ProgressRepositoryTest | fedfa61 |
| WR-04 | fixed: scenarioPython derives the Soon set from the catalog; Soon UI checks only when such a lesson exists | b6a054d |
| WR-01 | deferred to phase 10 | — |
| IN-01..IN-06 | not in scope | — |

---

_Reviewed: 2026-10-28T14:55:22Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
