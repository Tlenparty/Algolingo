# Project Research Summary

**Project:** Algolingo (Алголинго)
**Domain:** Offline single-user Duolingo-style mobile trainer for algorithm interviews (KMP + Compose Multiplatform, Android + iOS); Kotlin solutions assembled from tappable chips into holes of a template.
**Researched:** 2026-10-21
**Confidence:** MEDIUM-HIGH

## Executive Summary

Algolingo is a **content player**: static lesson JSON shipped in the app, a pure-Kotlin engine that judges chip placement, and one small progress document. Comparable apps (Duolingo, Mimo, Grasshopper) share one loop: short explainer → linear session of tap-to-fill exercises → immediate feedback → mistakes re-queued at end → XP + streak. The strongest asset is the customer's Obsidian notes: each task already has statement, hints, reference solution, complexity and «⚠️ Грабли» — the grabli become distractors with explanations, so every wrong chip is a real interview bug with a stated reason.

Approach: one KMP shared module with all code + thin `androidApp` and `iosApp` shells (AGP 9 forces the split); Compose Multiplatform 1.12.2, Kotlin 2.4.21 and a handful of libraries; no DI, no database, no network. All logic is pure, clock-free and tested in `commonTest`: exercise engine (place/remove/check), lesson session reducer (queue, retry-at-end, hints, grade), progress functions (XP, streak, Leitner 1/3/7). Lesson JSON is produced by a hand-run generator from the notes + a hand-written hole/distractor sidecar, committed to git; Gradle never reads Yandex.Disk.

Top risks: (1) **a correct answer marked red** — several "obvious" distractors are valid (`l <= r`, `Int.MIN_VALUE`, `(lo + hi) ushr 1`) and whole-sequence comparison can't cover equivalent forms → per-hole accepted sets + distractor verdicts with counter-examples; (2) **iOS build rot** while Android works → Phase 1 done only when both CLI builds pass, every phase verified on the iOS simulator; (3) **lost/corrupted progress** → stable ASCII ids, versioned document, `LocalDate` in local timezone with injected `today`.

## Resolved Conflicts & Customer Decisions (2026-10-21)

| Topic | Positions | **Decision** | Upgrade path |
|---|---|---|---|
| Progress persistence | ARCHITECTURE: DataStore; STACK: JSON string via expect/actual SharedPreferences/NSUserDefaults; PITFALLS: versioned JSON file | **Customer chose DataStore KMP (1.2.x) from the start**, storing one versioned `@Serializable Progress` document (`schemaVersion`, defaulted fields, `ignoreUnknownKeys`) via OkioStorage + JSON serializer. DataStore gives atomic writes | SQLDelight/Room only if relational queries appear (attempt history, analytics) |
| Answer checking | PROJECT.md: whole sequence vs 1–3 references; FEATURES + PITFALLS: per-hole | **Per-hole accepted sets.** One chip per hole; a chip may be a multi-token expression (`lo + (hi - lo) / 2`). Chips have **unique ids** (state + Compose keys); checker compares chip **text** to the hole's accepted set. PROJECT.md Key Decision updated accordingly | Exercise-level `alternatives` only for coupled holes (none in level 0). Atomic-token whole-body assembly = v2 exercise type |
| "Works but not ideal" (`l <= r` in 0.2/0.3, `Int.MIN_VALUE` in 0.1, `ushr` in 0.4) | Notes contradict (`04` §3 says wrong, `05` 0.2 says harmless) | **Customer: «Принято, но…»** — amber result: counts as solved, shows a note on why the reference is better. Chip carries verdict `acceptable` + `note`. A chip may be `wrong` only with a concrete `breaksOn` counter-example | — |
| Chip bank | FEATURES: shared vs per-hole candidates | **Customer: one shared bank** for all holes of an exercise (≤ ~10 chips, ≤ ~1 distractor per decision hole) | Per-hole candidate filtering as an easy mode, v1.x |
| Theory & missing distractors | Level 0 in `05` has no theory; 0.2/0.3 have no Грабли | **Customer: Claude authors theory cards (from `04` §3/§7, `00`, `03`) and distractors; customer reviews** | — |
| Navigation | Nav2 vs Nav3 | **Nav2 `navigation-compose` 2.9.2**, type-safe routes, 2–3 routes; theory/exercise/reveal are phases inside the Lesson screen | 2.10.0 when stable |
| Hole markup | md sidecar `⟦a \| b⟧` vs JSON overlay | **md sidecar with `⟦⟧`** in `content/authoring/` + drift check (stripping markup must reproduce the note's solution) | — |
| Generator input | vault path vs ASCII copies | Vault path as argument, NFC-normalize names; commit one ASCII-named fixture copy of `05` for the golden parser test | — |
| Time API | — | `kotlin.time.Instant/Clock` (stable since Kotlin 2.3); kotlinx-datetime 0.8.0 only for `LocalDate`/`TimeZone` | — |

## Key Findings

### Recommended Stack
Details: [STACK.md](STACK.md).
- Kotlin **2.4.21**; Compose Multiplatform **1.12.2** + material3 **1.12.0-alpha03**; Compose Resources for bundled JSON (`Res.readBytes("files/...")`, no directory listing → ship `index.json`).
- AGP **9.3.x**, Gradle **9.7.1**, Xcode **26.4**, compileSdk 37 / minSdk 24 (highest within the KGP 2.4.21 tested matrix; avoid Kotlin 2.5 Beta, AGP 9.4, Gradle 9.8).
- lifecycle-viewmodel-compose **2.11.0**, navigation-compose **2.9.2**, kotlinx-serialization-json **1.12.0**, kotlinx-datetime **0.8.0**, DataStore **1.2.x** (customer decision).
- Built in-house: per-token keyword highlighter, `FlowRow` chips, one `AppGraph` object instead of DI, Russian string literals.
- Layout from the KMP wizard: `com.android.kotlin.multiplatform.library` in shared module with `androidResources { enable = true }` and `withHostTest {}`; targets `iosArm64` + `iosSimulatorArm64` only (iosX64 removed in CMP 1.11).

### Expected Features
Details: [FEATURES.md](FEATURES.md).

**Must have:** lesson as linear session with progress bar; 2–4 theory cards first (to be authored); hole exercise (monospace template, inline holes, shared chip bank with grey ghosts, tap → first empty hole, tap placed chip → back, tap hole → target it); «Проверить» disabled until all holes filled; per-hole green/red/amber + feedback sheet with the trap explanation; wrong exercises re-queued at end, capped ~2, bank reshuffled; hints one by one with penalty, hidden when 0; post-solve reveal (reference, complexity, grabli, variations); XP, daily streak (local day), lesson list with done/locked, local persistence.

**Differentiators (MVP):** distractors = real interview traps with explanations; holes on decisions, not boilerplate; three-way grade ✅ clean / 💡 hints or retried / ❌ revealed or failed — single input for XP and SRS; Leitner 1/3/7 with "mastered" + «Повтор сегодня (N)» reusing the lesson player.

**Defer:** trace card, ladder diff highlighting, per-hole candidates, streak freeze, reference screen (v1.x); Parsons, atomic-token assembly, signal→method, predict output, FSRS, notifications, VPS compile (v2+). **Don't build:** hearts, ⏱ timer enforcement, leagues, coins, drag-and-drop, mid-lesson resume.

### Architecture Approach
Details: [ARCHITECTURE.md](ARCHITECTURE.md). Three layers in one module, enforced by packages: **UI** (Compose, renders `StateFlow<UiState>`), **Domain** (pure Kotlin, no Compose/IO/clock/Random — `today` and `seed` injected), **Data** (read-only content repo + one progress document). Single write path: lesson Done → `applyLessonResult(progress, result, today)` → persist.

1. `tools/gen_content.py` (Python stdlib, hand-run): note + `⟦⟧` sidecar → validate → JSON (committed).
2. Content model (`@Serializable`, `schema` int): `index.json` + one file per lesson; exercise = `segments` (holes = segments − 1), `bank` of chips `{id, text, verdict?, why?, breaksOn?}`, per-hole `accepted` sets; `origin` field for future store licensing.
3. `ExerciseEngine`: `hole → chipId?`; `place`, `remove`, `selectHole`, `check` → per-hole ok/acceptable/wrong + `why`.
4. `LessonSession`: pure reducer Theory(i) → Exercise → Feedback → Reveal → Done; capped retry queue, hints, grade.
5. Progress: `Progress` doc, `Srs`, `Streak`, `Xp` — pure functions of `(state, result, today: LocalDate)`, keyed by stable ASCII ids.
6. `ContentRepository`, `ProgressStore` (DataStore), `AppGraph`, ViewModels — thin wiring.

### Critical Pitfalls
Full list: [PITFALLS.md](PITFALLS.md).
1. **Valid answers as distractors / equivalent answers rejected** → per-hole accepted sets, verdict + `why` + `breaksOn`; hand-written tests running level-0 references and accepted variants on the statement examples. `05` 2.4 reference is confirmed wrong — references must be verified, not trusted.
2. **Duplicate chip text** (`mid`, `1`, `-1`) → unique ids for state and Compose keys; never `key = chip.text`.
3. **Obsidian parsing traps**: `>` empty lines, `->`/`>=` in code, VS16 emoji (`⚠️` = U+26A0 U+FE0F), 0..N hints, two fences in one solution, `[!warning]` callouts, NFD vs NFC Cyrillic names → line state machine, match callout type not title, NFC, golden count test (32 `[!question]`, 29 `[!success]`, 0 hints in 0.3).
4. **iOS build drift** → official wizard, Arm64 only, User Script Sandboxing off + `./gradlew --stop`, both CLI builds from a clean clone; project outside Yandex.Disk.
5. **Streak/SRS date bugs, lost progress** → `LocalDate` from `todayIn(currentSystemDefault())`, injected `today`, never `86400`; stable ids (generator fails if an id disappears), versioned document with defaults; unknown ids ignored.

## Implications for Roadmap

1. **Project Skeleton (Android + iOS)** — retire AGP 9 / resources / Xcode sandbox risk: wizard project, pinned versions, hello screen reading one `files/` JSON on both platforms, `./gradlew` + `xcodebuild` from clean clone.
2. **Content Model + Engine + Lesson Session (pure, TDD)** — freeze schema and check semantics before any markup/UI: model, `ExerciseEngine`, `LessonSession`, hand-written `l0.4.json`, tests for duplicate chips, acceptable verdicts, 0 hints, retry cap.
3. **Exercise & Lesson UI + Navigation** — core-value demo: code-with-holes rendering (no soft wrap, horizontal scroll, JetBrains Mono), `FlowRow` bank with ghosts, feedback sheet (green/red/amber), hints, reveal, theory pager, summary, Home, Nav2. UI-SPEC (`/gsd-ui-phase`) and style sketches before this phase.
4. **Progress — XP, Streak, SRS, Review** — DataStore `Progress` doc, `applyLessonResult`, LocalDate streak, Leitner 1/3/7 + mastered, «Повтор сегодня (N)», done/locked; fake-date tests (23:59→00:01, two lessons a day, skipped day, clock moved back).
5. **Content Pipeline + Level 0 Authoring** — depends only on Phase 2, can overlap 3–4: `gen_content.py`, golden parser test, sidecars + theory cards for 0.1–0.4, reference tests, `ContentFilesTest`, customer review.
6. **End-to-End Pass on Both Platforms** — all 4 lessons on Android + iOS simulator, "Looks Done But Isn't" checklist, polish.

**Research flags:** Phase 1 (fresh version pins, wizard output) and Phase 5 (sidecar syntax on all 4 tasks; how reference tests run) need `--research-phase`. Phases 2, 3, 4, 6 are standard patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Versions from Maven metadata, CMP changelog, compatibility matrix (2026-10-21) |
| Features | MEDIUM | Competitor UX from reviews; distractor/Parsons claims from peer-reviewed papers (HIGH); notes mapping read directly (HIGH) |
| Architecture | MEDIUM-HIGH | Layout/resources from official docs; engine/schema/SRS are grounded design decisions |
| Pitfalls | MEDIUM-HIGH | Content/Unicode/parser verified on real files (HIGH); platform from docs + community |

### Gaps to Address
- **Exercise structure per lesson:** "one task = one lesson, exercises = same solution with progressively more holes" — confirm during Phase 2 discussion.
- **SRS unit:** recommend lesson/task (matches «умею = 3 раза с интервалами 1/3/7»), review replays its hardest exercise — confirm in requirements.
- **Theory cards:** authored by Claude in Phase 5, reviewed by customer.
- **`05` 2.4 bug:** fix in the note before level 2.
- **Store release** (LeetCode wording, «🔥Avito» tags, "-lingo" trademark): out of MVP; `origin` field added now.
- **Visual style:** not yet defined — sketch 2–3 options and lock a UI-SPEC before Phase 3.

## Sources
- **HIGH:** Maven Central / Google Maven metadata; CMP CHANGELOG; kotlinlang KMP compatibility guide & AGP 9 migration; Compose resources docs; customer notes `00`–`06` (read directly); ICER 2024 distractors paper; Harms et al.; Ericson et al. 2022; Smith & Zilles SIGCSE 2023.
- **MEDIUM:** DataStore KMP docs; kotlinx-datetime 0.7 notes; Gradle #27068; CMP #4111; Room KMP reports; Duolingo UX case studies; Mimo/Grasshopper reviews; LeetCode ToS.
- **LOW:** Leitner vs FSRS blog; Duolingo trademark risk; NeetCode/AlgoExpert hint patterns.

---
*Research completed: 2026-10-21 — customer decisions applied*
*Ready for roadmap: yes*
