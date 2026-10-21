# Phase 3: Lesson & Exercise UI - Research

**Researched:** 2026-10-21
**Domain:** Compose Multiplatform UI on top of the finished Phase 2 engine (no new libraries)
**Confidence:** HIGH (code/engine/fonts verified locally), MEDIUM (BackHandler on iOS, haptics on iOS)

## User Constraints (from CONTEXT.md)

No CONTEXT.md exists for this phase. The locked contract is `03-UI-SPEC.md` (status: approved 2026-10-21). Treat it as locked: palette, sizes, copy, motion, interaction rules, the exit dialog, "show wrong AND acceptable holes at once" in the error panel, dark ink on colored buttons, Nunito + JetBrains Mono.

Out of scope (from UI-SPEC and ROADMAP): Home data (streak, XP totals, review count, real done/locked statuses), persistence, SRS (Phase 4); `index.json`, lessons 0.1-0.3 (Phase 5); drag-and-drop, sound, confetti, in-app theme toggle, per-hole candidate filtering, trace cards.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| LESS-02 | 2-4 theory cards, "Дальше" | `Phase.Theory(index)` + `NextTheory`; card renders `TheoryCard` (signals/skeleton/pitfalls/body); l0.4 has 3 cards |
| LESS-04 | Progress bar | `LessonState.progress` already = done/(done+queue.size) incl. requeues; `animateFloatAsState` 300ms |
| LESS-06 | Summary ✅/💡/❌ + XP | `LessonReducer.result(state)` gives `Grade`; add pure `xpFor` (below) |
| EXER-01 | Statement + highlighted monospace template with holes | `CodeTemplate`: per-line Rows of `highlight()` pieces + hole composables |
| EXER-02 | Shared bank, ≤10 chips, multi-token chips | `ExerciseState.order` (shuffled bank indices), `FlowRow` |
| EXER-03 | Tap chip fills selected/first empty hole; gray ghost stays | `LessonAction.TapChip(id)`; ghost = `state.isPlaced(id)` |
| EXER-04 | Tap placed returns it; tap hole selects it | One action: `TapHole(h)` (clears the hole and selects it) |
| EXER-06 | "Проверить" only when all holes filled | `current.placed.all { it != null }` |
| EXER-07 | Hole colors green/yellow/red | `Phase.Feedback.check.holes[h]`: Ok / Acceptable / Wrong |
| EXER-08 | Slide-up panel with grabli and correct variant | `ResultPanel` built from `HoleResult.Wrong(text, why, correct)` |
| EXER-09 | Hints one by one, hidden if none | `Exercise.hints`, `LessonState.hintsShown[id]`, `LessonAction.Hint` |
| EXER-10 | Reveal: reference, complexity, pitfalls, variations | `lesson.solution` (code/time/space/pitfalls/variations) |
| UI-01 | Playful style, light/dark by system | `AppTheme` already switches on `isSystemInDarkTheme()`; add fonts + chunky components |
</phase_requirements>

## Summary

Phase 3 is a pure UI phase on a finished engine. Everything needed is already in the repo: `LessonReducer` (start/reduce/result), `ExerciseState` (placed/selected/order are all Int bank indices), `CheckResult`/`HoleResult`, `AppTheme` + `AppColors` (UI-SPEC palette already coded), `highlight(code, colors)`, the KB inline-markdown parser (`parseInline` + `inlineText`), and `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `navigation-compose`, `components-resources` are already declared in `shared/build.gradle.kts`. **No new Gradle dependencies are required**, so there is no package-legitimacy risk. New assets only: 4 TTF files.

The work splits into: (1) fonts + theme typography; (2) 6 shared components (`ChunkyButton`, `TokenChip`, `CodeTemplate`, `CodeBlock`, `LessonProgressBar`, `ResultPanel`); (3) a `LessonViewModel` + `LessonRepository` loader + a `LessonScreen` that switches on `Phase`; (4) nav wiring (a `LessonRoute(id)`, bottom bar hidden, minimal Home entry); (5) three small pure functions with unit tests (`xpFor`, hole/panel mapping, reveal spans). The reducer is the single source of truth; the screen is a stateless function of `LessonState`.

l0.4.json satisfies the UI: 3 theory cards, 3 exercises (1/2/4 holes), hints (1/2/2), `solution.time/space/pitfalls`. Only gap: `solution.variations` is `[]`, so the "🔁 Вариации" section is hidden for now (UI-SPEC already says hide when empty). No schema change is needed.

**Primary recommendation:** Add no libraries; build a stateless `LessonScreen(state, onAction)` driven by `LessonViewModel` (`MutableStateFlow<LessonState>` + `LessonReducer.reduce`), put all visual-state decisions in a small pure `UiMapping.kt` (unit-tested in `commonTest`), and ship static Nunito/JetBrains Mono TTFs with lowercase-underscore file names.

## Architectural Responsibility Map

Single-tier (on-device Compose app, no backend).

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Lesson state machine, queue, grading | Domain (`lesson/`, pure) | — | Done in Phase 2; UI must not duplicate logic |
| XP for a result | Domain (`lesson/Xp.kt`, pure) | — | Needs unit tests; Phase 4 will store it |
| Hole/chip visual state, panel model, reveal spans | Domain-adjacent pure mapping (`lesson/UiMapping.kt`) | — | Testable without Compose |
| Holding state across recomposition/rotation | `LessonViewModel` | — | `viewModel { }`, `StateFlow` |
| Loading + validating lesson JSON | `LessonRepository` (suspend, `Res.readBytes`) | — | Offline asset; `validate()` already exists |
| Rendering, animation, haptics | Compose UI (`lesson/ui/`, `ui/components/`) | — | Shared CMP |
| Back/exit confirmation | Compose UI (BackHandler + AlertDialog) | NavHost | See Pitfall 5 |

## Standard Stack

### Core (all already in `gradle/libs.versions.toml` and `shared/build.gradle.kts`) [VERIFIED: repo files]
| Library | Version | Purpose |
|---------|---------|---------|
| Compose Multiplatform runtime/foundation/ui | 1.12.2 | `FlowRow`, `AnimatedVisibility`, `AnimatedContent`, gestures, `horizontalScroll` |
| `org.jetbrains.compose.material3:material3` | 1.12.0-alpha03 | `MaterialTheme`, `AlertDialog`, `Text` |
| `org.jetbrains.compose.components:components-resources` | 1.12.2 | `Res.readBytes`, `Res.font.*`, `Font()` |
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` / `lifecycle-runtime-compose` | 2.11.0 | `viewModel { }`, `collectAsStateWithLifecycle` |
| `org.jetbrains.androidx.navigation:navigation-compose` | 2.9.2 | New `@Serializable data class LessonRoute(val id: String)` |
| `kotlinx-serialization-json` | 1.12.0 | Decode `Lesson` |

### Package Legitimacy Audit
No external packages are added in this phase. Re-used: `androidx.compose.ui.backhandler.BackHandler` lives in the JetBrains-official `org.jetbrains.compose.ui:ui-backhandler` artifact, which is already resolved in the local Gradle cache [VERIFIED: ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose.ui/ui-backhandler*]; if it is not on the compile classpath transitively, declare `org.jetbrains.compose.ui:ui-backhandler:1.12.2` (same CMP version) in `commonMain`. slopcheck not run: nothing new from a public registry beyond a first-party JetBrains artifact. Fonts are static assets (see below).

### Assets (new)
Put in `shared/src/commonMain/composeResources/font/`. **File names must be lowercase letters/digits/underscore** (they become `Res.font.<name>` accessors) [CITED: kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html shows `Res.font.Inter_24pt_Regular` pattern; lowercase rule per the resource-name convention, ASSUMED to be enforced — use lowercase anyway].

| File | Source | Verified |
|------|--------|----------|
| `jetbrains_mono_regular.ttf`, `jetbrains_mono_bold.ttf` | `https://raw.githubusercontent.com/JetBrains/JetBrainsMono/master/fonts/ttf/JetBrainsMono-Regular.ttf` and `-Bold.ttf` | HTTP 200, 270 KB; Cyrillic (Ж я ё) and `→` present [VERIFIED: downloaded + fontTools cmap] |
| `nunito_regular.ttf`, `nunito_bold.ttf` | **No static TTFs exist upstream.** `googlefonts/nunito` and `google/fonts/ofl/nunito` ship only the variable `Nunito[wght].ttf` (axis wght 200-1000; Cyrillic present). The Google Fonts zip endpoint returns HTML (not fetchable by curl). | Generated static instances from the variable font: `fonttools varLib.instancer Nunito[wght].ttf wght=400 -o nunito_regular.ttf` and `wght=700 -o nunito_bold.ttf` (132 KB each; worked in this session with `pip install fonttools` in a venv) [VERIFIED] |

Why static, not the variable file: `Font(resource, weight)` just labels a file with a weight; a variable font's `wght` axis is not guaranteed to be applied per-weight on every target, and Android `FontVariation` needs API 26 while minSdk = 24 [CITED: kotlinlang docs signature `Font(resource, weight, style)`; web search found no confirmation that the axis is applied] — static instances remove the risk. OFL requires shipping the license: copy `OFL.txt` for both fonts into e.g. `third_party/fonts/` (not into composeResources).

Nunito has no emoji or `→` glyphs; emoji (✅ 💡 ❌ 🔥 ▶ 🔒) fall back to the system emoji font on both platforms, which is the intended icon strategy in UI-SPEC. JetBrains Mono also lacks emoji; keep emoji outside code text.

Font loading API (composable, so the typography must be built inside `@Composable`): [CITED: kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html]
```kotlin
import algolingo.shared.generated.resources.*   // Res, nunito_regular, ...
@Composable fun rememberAppFonts(): AppFonts {
    val ui = FontFamily(Font(Res.font.nunito_regular, FontWeight.Normal), Font(Res.font.nunito_bold, FontWeight.Bold))
    val mono = FontFamily(Font(Res.font.jetbrains_mono_regular, FontWeight.Normal), Font(Res.font.jetbrains_mono_bold, FontWeight.Bold))
    return remember(ui, mono) { AppFonts(ui, mono) }
}
```

## Architecture Patterns

### System Architecture Diagram
```
Home node tap ──navigate(LessonRoute("l0.4"))──▶ LessonRoute destination
                                                   │
                         produceState: LessonRepository.load(id)
                         Res.readBytes("files/content/lessons/l0.4.json")
                         ─▶ Json{ignoreUnknownKeys}.decode<Lesson> ─▶ validate(lesson)
                              │ error/non-empty ─▶ ErrorState («Не удалось открыть урок», «На главную»)
                              ▼ ok
              viewModel(key = id) { LessonViewModel(lesson, seed) }
                              │  state: StateFlow<LessonState>
        ┌─────────────────────┴───────────────────────┐
  UI actions (LessonAction) ──▶ LessonReducer.reduce(state, a) ──▶ new LessonState
        ▲                                                           │
        │                                  AnimatedContent(screenKey(state))
        │            ┌────────────┬─────────────────┬─────────────┬───────────┐
        │       Phase.Theory   Phase.Solving /   Phase.Reveal    Phase.Done
        │       TheoryScreen   Phase.Feedback    RevealScreen    SummaryScreen
        │                      ExerciseScreen                    (result(state) + xpFor)
        │                      ├ TopBar(progress, 💡)               │
        │                      ├ scroll: title, statement, hint cards, CodeTemplate
        │                      └ bottom: Bank+Проверить  ⇄  ResultPanel (on Feedback)
        └────────────── onClick lambdas (chips, holes, buttons)     └─▶ popBackStack to Home
```

### Recommended Project Structure (reuse existing packages)
```
shared/src/commonMain/kotlin/com/tlenparty/algolingo/
├── lesson/
│   ├── Xp.kt                 # xpFor(ExerciseResult) / xpFor(LessonResult)  (pure)
│   ├── UiMapping.kt          # holeVisual(), panelModel(), solutionSpans(), hintsRemaining()  (pure)
│   ├── LessonRepository.kt   # suspend load(id): Result<Lesson>
│   └── LessonViewModel.kt
├── lesson/ui/                # LessonScreen.kt (switch on Phase), TheoryScreen, ExerciseScreen, RevealScreen, SummaryScreen
├── ui/components/            # ChunkyButton, TokenChip, CodeTemplate, CodeBlock, LessonProgressBar, ResultPanel (one file each)
├── ui/theme/Theme.kt         # + fonts, LocalCodeFont, AppText styles (extend, do not fork)
├── ui/theme/Highlight.kt     # reuse as is
└── home/HomeScreen.kt        # minimal entry (replaces HomeStub in App.kt)
shared/src/commonTest/.../lesson/  XpTest.kt, UiMappingTest.kt
```
The UI-SPEC says `highlight(token): AnnotatedString`; the repo already has `highlight(code, colors)` — **reuse it, do not add a second one**. The spec's keyword set is already present (includes `when`, `in`, etc.).

### Pattern 1: ViewModel + reducer
```kotlin
class LessonViewModel(lesson: Lesson, seed: Int) : ViewModel() {
    private val _state = MutableStateFlow(LessonReducer.start(lesson, seed))
    val state: StateFlow<LessonState> = _state
    fun on(a: LessonAction) { _state.update { LessonReducer.reduce(it, a) } }
}
// in the destination:
val vm = viewModel(key = id) { LessonViewModel(lesson, seed = Clock.System.now().toEpochMilliseconds().toInt()) }
val state by vm.state.collectAsStateWithLifecycle()
```
`Clock`/`Instant` are `kotlin.time` (stable, per CLAUDE.md) - no kotlinx-datetime needed. `reduce` is pure and returns the same state for stray actions, so double taps are harmless. `StateFlow` conflates equal states; `LessonState` is a data class so identical results do not recompose. Tests may pass a fixed seed.

### Pattern 2: Stateless screen, `screenKey` for transitions
Drive `AnimatedContent(targetState = screenKey(state))` where `screenKey` = `Theory(i)` / `Exercise(queue.first(), attempts)` / `Reveal` / `Done`. **`Solving` and `Feedback` of the same exercise attempt must map to the same key** (the panel slides inside the exercise screen, UI-SPEC section 5). Requeue changes `attempts[id]` so the next attempt gets a fresh screen (resets scroll). Rebuild per-screen state (`rememberScrollState`) inside the content.

### Pattern 3: Code template with inline holes
Segments contain `\n` and holes are between segments: `segments.size == holes.size + 1` (validated). Build lines by walking segments: split each segment by `'\n'`; the first piece continues the current line, later pieces start new lines. Each line is a `Row(Modifier.height(40.dp), verticalAlignment = CenterVertically)` of `Text(highlight(piece, colors), fontFamily = mono, softWrap = false, maxLines = 1)` pieces and `Hole` composables. One `horizontalScroll(rememberScrollState())` wraps the `Column` of all lines (UI-SPEC: lines scroll together). Skip empty pieces. Do not give anything `fillMaxWidth` inside the horizontal scroll.
```kotlin
sealed interface Piece { data class Code(val s: String): Piece; data class HoleAt(val h: Int): Piece }
fun templateLines(segments: List<String>): List<List<Piece>> { /* split by \n; HoleAt(i) after segment i except last */ }
```
Put `templateLines` in `UiMapping.kt` and unit-test it (segments from l0.4.e3 give 12 lines, 4 holes).

### Pattern 4: Chunky 3D button / chip (ledge via offset, press = translate)
```kotlin
@Composable fun ChunkyButton(text: String, onClick: () -> Unit, enabled: Boolean = true, style: ChunkyStyle = Primary, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val dy by animateDpAsState(if (pressed && enabled) 4.dp else 0.dp, tween(80))
    Box(modifier.fillMaxWidth().height(56.dp).clickable(src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)) {
        Box(Modifier.matchParentSize().padding(top = 4.dp).background(ledgeColor, RoundedCornerShape(16.dp)))   // ledge layer
        Box(Modifier.fillMaxWidth().height(52.dp).offset(y = dy).background(faceColor, RoundedCornerShape(16.dp)), Alignment.Center) { Text(...) }
    }
}
```
`clickable(interactionSource, indication = null, ...)` is the right overload (no ripple) [ASSUMED from standard Compose API; compile will confirm]. Disabled style: face `#E5E5E5/#37464F`, label `#AFAFAF/#52656D`, no ledge (UI-SPEC). Add Danger/Amber/Secondary variants as a `ChunkyStyle` data holder (face, ledge, label). `TokenChip` uses the same mechanism (face + 4dp outline ledge + 2dp border, corner 12dp).

Missing from `AppColors`: green/red/amber/focus **fill+ledge** and disabled colors. `AppColors` has tints/text only. Add the remaining fields (greenLedge `#58A700`, errorLedge `#EA2B2B`, amber `#FFC800`/ledge `#E5A500`, focus `#1CB0F6`, disabled face/label, secondaryText `#6B6B6B/#9AA9B1`) to `AppColors` (extend the data class; both Light/Dark instances) rather than hard-coding in components.

### Pattern 5: Chip bank with ghosts
`FlowRow(horizontalArrangement = spacedBy(8.dp), verticalArrangement = spacedBy(8.dp))` over `state.current.order` (bank indices), `key(id)` = the bank index (never text, EXER-05; l0.4.e3 has two chips with text "1"). Render `TokenChip(state = if (cur.isPlaced(id)) Ghost else Normal)`. **A ghost must keep its size**: render the same chip with a transparent text color and the outline fill, `clearAndSetSemantics {}`, not clickable. If the text were removed the chip would collapse and the FlowRow would reflow on every tap. Bank is non-interactive during `Phase.Feedback` (`enabled = phase is Solving`).

### Pattern 6: Result panel
Replaces the pinned bottom section: `AnimatedVisibility(visible = phase is Feedback, enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)))`. Not a `ModalBottomSheet`. Content cap: `Modifier.heightIn(max = screenHeight / 2)` + `verticalScroll` (get screen height from `BoxWithConstraints` of the screen). `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on the title. Haptics in a `LaunchedEffect(feedbackKey)`: `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.Confirm / Reject)`; `Confirm`/`Reject` exist in common Compose UI since 1.8 [CITED: developer.android.com HapticFeedbackType via search]; how iOS maps them is unconfirmed and the simulator does not vibrate — treat as no-crash best effort; UI-SPEC fallback `LongPress` if it does not compile.

### Pattern 7: Rich text in cards (reuse KB parser)
Statement, theory `signals`/`body`/`pitfalls`, hints, `why`, `note` and `solution.pitfalls` contain `` `code` `` and `**bold**`. Reuse `parseInline(text)` + `inlineText(...)` from `kb/ui/BlockView.kt` (public). Small change needed: `inlineText` hard-codes `FontFamily.Monospace` for inline code; switch it to `LocalCodeFont.current` (JetBrains Mono) so lessons and KB match. Wrap a bullet as `"• " + text`.

### Pattern 8: Reveal highlighting of "deciding" holes
`validate()` guarantees `segments + ok[0] == solution.code`. Compute spans: walk segments, `offset += seg.length`, record `offset until offset + ok0.length`, advance. Build `buildAnnotatedString { append(highlight(code, colors)); spans.forEach { addStyle(SpanStyle(background = successTint), it) } }`. Pure function `solutionSpans(ex): List<IntRange>` + test (spans slice equals each `ok[0]`).

### Anti-Patterns to Avoid
- **Re-implementing selection/placement logic in UI**: all of it is `tapChip`/`tapHole`; UI only maps taps to `LessonAction` and renders `ExerciseState`.
- **Compose keys or equality on chip text** (EXER-05).
- **Adding "Filled + selected" styling**: unreachable (tapHole always clears a filled hole). UI-SPEC row can be skipped; do not add a separate "tap placed chip" handler, the placed chip is inside the hole and its tap is `TapHole(h)`.
- **Dismiss/`ShowAnswer` buttons**: there is no such action; Reveal is automatic.
- **Double safe-area padding**: `App()` wraps `NavHost` in `Scaffold` and passes `Modifier.padding(pad)` (insets already applied). Do not also add `WindowInsets.safeDrawing` in lesson screens (UI-SPEC says to add it for standalone screens; here it would double). Verify visually on both platforms; add only the bottom pinned-section inset if the Scaffold is bypassed.
- **A second `highlight` function** or a per-lesson DI container.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Wrapping chip bank | Custom layout | `FlowRow` (stable, no opt-in) | Per CLAUDE.md; no deprecated `overflow` params |
| Inline `code`/`**bold**` parsing | New regex parser | `parseInline` + `inlineText` (kb) | Already tested (MarkdownParserTest) |
| Kotlin highlighting | New lexer | `ui/theme/Highlight.kt` `highlight()` | Already per UI-SPEC colors |
| Lesson validation | Ad-hoc checks | `validate(lesson)` | Guards schema, bank sizes, solution rebuild |
| Queue/grade/requeue/shuffle | UI-side logic | `LessonReducer` | Done + tested in Phase 2 |
| Dark/light | Manual toggle | `AppTheme` / `isSystemInDarkTheme()` | Already implemented |
| Transitions | Manual alpha/offset code | `AnimatedContent`, `AnimatedVisibility`, `animate*AsState` | Built in |
| Dialog | Custom overlay | material3 `AlertDialog` | Per UI-SPEC |

## Key Answers to the Research Questions

### 1. Reaching the lesson from Home without Phase 4/5 features
- Add `@Serializable data class LessonRoute(val id: String)` next to `Home`/`KbList`/`NoteRoute` in `App.kt`. Hide the bottom `NavigationBar` for `LessonRoute` too (existing code hides it for `NoteRoute`: extend the condition).
- Replace `HomeStub` (keep `loadHello()`/`Hello` and their test untouched; they are only used by the stub) with a minimal `HomeScreen(onStart: (String) -> Unit)`: a hard-coded `private val lessons = listOf(HomeLesson("l0.4", "0.4 Бинарный поиск"))`, rendered as one path node (72dp circle + 4dp ledge, "current" style) with the «Начать» pill. No LazyColumn zig-zag, no header, no locked nodes (YAGNI; Phase 4 owns statuses and the real list with LESS-01, Phase 5 owns `index.json`). UI-SPEC section 1 says Phase 3 renders all nodes as tappable "current"; with a single known lesson a one-item list satisfies it, and 0.1-0.3 nodes would be dead ends.
- Use the existing `NavBackStackEntry.ifResumed { }` guard on the Start tap to avoid double navigation.
- On Summary «Продолжить» / system back: `nav.popBackStack()` (returns to Home).
- Lesson-load failure and `validate()` errors: error screen with UI-SPEC copy and «На главную». Never crash.

### 2. XP (no storage)
Add `lesson/Xp.kt` (pure, in `commonMain`, tested in `commonTest`):
```kotlin
const val XP_CLEAN = 10; const val XP_ASSISTED = 5   // [ASSUMED] numbers, not in any locked doc other than "+10 XP" in UI-SPEC
fun xpFor(r: ExerciseResult): Int = when {
    !r.solved -> 0
    r.attempts <= 1 && r.hintsShown == 0 -> XP_CLEAN
    else -> XP_ASSISTED
}
fun xpFor(r: LessonResult): Int = r.perExercise.sumOf(::xpFor)
```
Reveal headline "+N XP" for the exercise just finished: `xpFor(ExerciseResult(id, solved, attempts[id], hintsShown[id]))` from `LessonState` at `Phase.Reveal(solved)`. Summary XP = `xpFor(LessonReducer.result(state))` (e.g. 3 clean exercises = 30). This matches "+10 XP" in UI-SPEC, PROG-01 ("fewer XP with hints/repeats"), and the reducer's rule that an acceptable verdict counts as solved and clean. Phase 4 can store/redefine the numbers; keep the constants in one place. Flag to user in the plan as a tunable default.

### 3. Does l0.4.json satisfy UI needs? [VERIFIED: file read]
| Need | l0.4 | Gap |
|------|------|-----|
| Theory 2-4 cards | 3 (signals+body / skeleton+body / pitfalls) | none |
| Hints | e1: 1, e2: 2, e3: 2 | none (e1 button shows count 1) |
| Complexity | `time` O(log n), `space` O(1) | none |
| Pitfalls | 2 in `solution.pitfalls` | none |
| Variations | `[]` | Section hidden. Content gap for Phase 5 (CONT-04); not a Phase 3 blocker |
| Acceptable hole | e2 `(lo + hi) ushr 1` with note | exercises yellow path |
| Duplicate-text chips | e3 two chips "1" | exercises EXER-05 |
| Error `why` | `wrongWhy` and `chip.why` present | fallback copy covers missing |

Schema is additive; `Json { ignoreUnknownKeys = true }` per CLAUDE.md. Theory card `skeleton` is code (use `CodeBlock`); `signals`/`pitfalls` bullet lists; `body` plain paragraph.

**UX decision to confirm (flagged):** the reducer enters `Phase.Reveal` after **every** exercise, and `solution` is lesson-level, so a full reveal would show the same reference/complexity/pitfalls three times. Recommended default: every Reveal shows the headline + `CodeBlock` with this exercise's deciding holes highlighted; the sections «Сложность / Грабли / Вариации» are shown only when this is the last exercise (`state.queue.size == 1` at Reveal). This satisfies EXER-10 (after solving the task the user sees all of it) without repetition and without touching the reducer. If the user wants the full reveal every time, drop the condition.

### 4. Fonts and theme
- Files and sources verified above. App-wide change: in `AppTheme` build `Typography` with `fontFamily` replaced on all 15 M3 styles (a small `Typography.withFont(f)` helper), keep sizes as they are so KB screens do not reflow; this is a deliberate cosmetic change to KB screens and is acceptable per the brief.
- Do **not** retune M3 text sizes for Phase 3. Add `object AppText` (or `@Immutable` class provided via CompositionLocal) with the four UI-SPEC roles: Body 16/24 W400, Label 14/20 W700, Heading 20/24 W700, Display 28/34 W700, plus code styles (14/20 W400, 14/20 W700, 16/24 W700 in JetBrains Mono). Exactly two weights.
- `AppTheme` currently has `MaterialTheme(colorScheme = scheme, content)` and a comment saying fonts are deferred to Phase 3: update that comment.
- Provide `LocalCodeFont = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }` from `AppTheme` (the same pattern as `LocalAppColors`).
- Font loading is asynchronous-looking but synchronous on Android/iOS for bundled resources; first frame may use fallback for a moment on some targets. Not a concern for MVP.

### 5. ViewModel
`androidx-lifecycle-viewmodelCompose` and `runtimeCompose` 2.11.0 are already in `libs.versions.toml` and `commonMain.dependencies` [VERIFIED]. `viewModel(key) { ... }` factory-lambda form in commonMain is the CLAUDE.md-recommended form. Survives Android rotation; after process death the lesson restarts (acceptable: "continue mid-lesson" is out of scope). Keep the loaded `Lesson` out of the ViewModel constructor's responsibility: load in `produceState`, then create the VM.

### 6. Testing strategy (nyquist validation is disabled in config; brief notes)
- Run: `./gradlew :shared:testAndroidHostTest` (JVM, fast) and `:shared:iosSimulatorArm64Test` for the common tests. Existing: ReducerTest, EngineTest, DuplicateChipTest, ModelTest, ShuffleTest, MarkdownParserTest, LessonContentTest (androidHostTest, decodes bundled JSON).
- New pure tests (commonTest) worth writing:
  - `XpTest`: clean 10; one hint 5; retried 5; unsolved 0; lesson sum.
  - `UiMappingTest`: `holeVisual(h, state, check)` (Empty / EmptySelected / Filled / Ok / Acceptable / Wrong); `panelModel(check)` (Ok -> no body; Acceptable -> amber with notes; Wrong -> lists wrong in template order with `correct`, then acceptable section; `why == null` -> fallback copy); `templateLines` (line count, hole positions); `solutionSpans` (slice == ok[0]); `hintsRemaining`.
  - A content test that every bundled lesson passes `validate()` (check what `LessonContentTest` already covers before adding).
- UI interaction (tap chip into hole, ghosts, slide-up panel, themes, fonts, back dialog) is a manual `checkpoint:human-verify` on the Android emulator and the iOS simulator, light and dark. Compose UI tests are skipped for the MVP per CLAUDE.md.

## Common Pitfalls

### Pitfall 1: Ghost chip collapses
**What goes wrong:** Removing the text of a placed chip changes its width and the bank reflows. **Avoid:** transparent text, same padding/border, outline fill.

### Pitfall 2: Resource accessor names
**What goes wrong:** `JetBrainsMono-Regular.ttf` cannot be a Kotlin accessor name; build fails or accessor missing. **Avoid:** rename to `jetbrains_mono_regular.ttf` etc. before adding. The generated package is `algolingo.shared.generated.resources` (see `App.kt` import).

### Pitfall 3: Fonts are `@Composable`
**What goes wrong:** Building `Typography`/`TextStyle` with `Font(Res.font...)` outside composition does not compile. **Avoid:** build in `AppTheme`, expose through `MaterialTheme.typography` / CompositionLocals.

### Pitfall 4: Horizontal scroll plus fillMaxWidth
**What goes wrong:** `fillMaxWidth` inside `horizontalScroll` gets infinite constraints and crashes/zero-sizes. **Avoid:** wrap-content Rows inside the scroll; set `softWrap = false` on code Text.

### Pitfall 5: Back handling inside NavHost
**What goes wrong:** Exit confirmation (UI-SPEC) needs to intercept system back and the iOS edge swipe. A YouTrack issue (CMP-8791) reported `BackHandler` not working on iOS inside a NavHost; JetBrains' guidance for newer versions is `NavigationBackHandler` (navigation-event). **Avoid:** implement `ExitDialog` + `BackHandler(enabled = phase !is Done)` first; verify on the iOS simulator; if the swipe is not intercepted, switch to `NavigationBackHandler` (the `androidx.navigationevent` / `org.jetbrains.androidx.navigationevent` artifacts are already in the Gradle cache). MEDIUM confidence, make it an explicit verify step. The ✕ button always works (opens the dialog). On `Done`, back goes straight Home with no dialog.

### Pitfall 6: Stale screen state between attempts
**What goes wrong:** After a wrong answer the exercise is requeued (possibly the same exercise next when it is the only one left): scroll position and animations persist. **Avoid:** `screenKey` includes `attempts[id]`.

### Pitfall 7: Hint shown count vs. requeue
`hintsShown` persists across attempts of the same exercise (reducer keeps it), so revealed hint cards remain after a retry; derive cards from `state.hintsShown[id]`, not from local UI state. The 💡 button does nothing outside `Phase.Solving` (reducer ignores `Hint`); render it disabled in Feedback.

### Pitfall 8: Progress bar after requeue
`progress` = done / (done + queue.size); after a wrong answer the requeued exercise keeps `done` unchanged and the bar stays put (no regression). Do not compute your own progress.

### Pitfall 9: Hole touch targets
Hole is 32dp visually, but the touch area is the 40dp line x max(48dp, width): use `Modifier.height(40.dp).widthIn(min = 48.dp).clickable { }` on an outer Box and draw the 32dp face inside.

### Pitfall 10: Emoji in Nunito Bold labels
Emoji render via system fallback and ignore the font weight; sizes for icon emoji should be set explicitly (24/32/64dp-equivalent sp) as per UI-SPEC.

## Code Examples

### Hole visual state (pure, testable)
```kotlin
enum class HoleVisual { Empty, EmptySelected, Filled, Ok, Acceptable, Wrong }
fun holeVisual(h: Int, s: ExerciseState, check: CheckResult?): HoleVisual = when (val r = check?.holes?.getOrNull(h)) {
    is HoleResult.Ok -> HoleVisual.Ok
    is HoleResult.Acceptable -> HoleVisual.Acceptable
    is HoleResult.Wrong -> HoleVisual.Wrong
    null -> when {
        s.placed[h] != null -> HoleVisual.Filled
        s.selected == h -> HoleVisual.EmptySelected
        else -> HoleVisual.Empty
    }
}
```

### Panel model (pure)
```kotlin
enum class PanelKind { Success, Amber, Danger }
data class PanelItem(val chip: String, val text: String, val correct: String? = null)
data class PanelModel(val kind: PanelKind, val title: String, val wrong: List<PanelItem>, val acceptable: List<PanelItem>)
fun panelModel(c: CheckResult): PanelModel { /* wrong in template order; fallback text "Здесь нужно другое. Верно: `<correct>`" when why == null */ }
```
(Wrong holes are shown together with acceptable ones in the same panel, per the 2026-10-21 customer override.)

### Loader
```kotlin
private val json = Json { ignoreUnknownKeys = true }
suspend fun loadLesson(id: String): Result<Lesson> = runCatching {
    val l = json.decodeFromString<Lesson>(Res.readBytes("files/content/lessons/$id.json").decodeToString())
    val errs = validate(l); require(errs.isEmpty()) { errs.joinToString() }
    l
}
```
`Res.readBytes` is a suspend function [CITED: CLAUDE.md sources, kotlinlang compose-multiplatform-resources-usage]. Beware `runCatching` swallowing `CancellationException` inside `produceState`: rethrow it, or catch only `SerializationException`/`IllegalArgumentException`/resource-missing exceptions.

### Screen switch
```kotlin
@Composable fun LessonScreen(state: LessonState, on: (LessonAction) -> Unit, onExit: () -> Unit) {
    AnimatedContent(screenKey(state), transitionSpec = { (slideInHorizontally(tween(200)) { it } + fadeIn(tween(200))) togetherWith (slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(200))) }) { _ ->
        when (val p = state.phase) {
            is Phase.Theory -> TheoryScreen(state.lesson.theory[p.index], state.progress, on)
            Phase.Solving, is Phase.Feedback -> ExerciseScreen(state, on)
            is Phase.Reveal -> RevealScreen(state, p.solved, on)
            Phase.Done -> SummaryScreen(LessonReducer.result(state), onExit)
        }
    }
}
```
Caution: inside `AnimatedContent`, read the **live** `state` for the incoming content, not a stale lambda value, else the outgoing page renders the new phase while sliding out; pass the `screenKey` and fetch the content from `state` consistently, or keep it simple with `Crossfade` if the slide looks wrong.

## State of the Art

| Old | Current | Impact |
|-----|---------|--------|
| `PredictiveBackHandler` | `NavigationBackHandler` (navigation-event) in CMP 1.10+ | Use for the exit confirmation if `BackHandler` fails on iOS |
| Deprecated `FlowRow(overflow=…)` | plain `FlowRow` | Per CLAUDE.md |
| `kotlinx.datetime.Clock` | `kotlin.time.Clock` | Seed from `Clock.System.now()` |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | XP numbers: 10 clean, 5 assisted (hint or retry), 0 unsolved; lesson XP = sum of exercises | Q2 | User may want other values; isolated in `Xp.kt` and Phase 4 may redefine |
| A2 | Reveal shows complexity/pitfalls/variations only on the last exercise's Reveal (compact reveal before) | Q3 | Spec reading says full reveal after solving; a one-line condition change |
| A3 | Compose resource file names must be lowercase/underscore | Assets | If wrong, no harm: lowercase names are safe regardless |
| A4 | `clickable(interactionSource, indication = null, …)` overload and `Confirm`/`Reject` haptics compile in CMP 1.12.2 | Patterns 4, 6 | Minor compile fixes (`LongPress` fallback in UI-SPEC) |
| A5 | `ui-backhandler` is on the compile classpath transitively (it is in the Gradle cache) | Pitfall 5 | Add explicit dependency (same CMP version) |
| A6 | Home shows only the l0.4 node (no 0.1-0.3 placeholders) | Q1 | Cosmetic; UI-SPEC says "all nodes current", which would lead to dead taps |
| A7 | Scaffold's `pad` already supplies system insets, so lesson screens must not add `safeDrawing` again | Anti-patterns | Double padding or missing inset; verify visually on both platforms |

## Open Questions

1. **Back swipe on iOS inside NavHost** - unknown until tried on the simulator; fallback `NavigationBackHandler`. Plan an early spike task (Wave 1) rather than leaving it to the end.
2. **Reveal repetition (A2)** - confirm with the user, or adopt the default.
3. **Haptic mapping on iOS** - cannot be tested in the simulator; best-effort.

## Environment Availability

| Dependency | Required By | Available | Notes |
|------------|------------|-----------|-------|
| Gradle/Kotlin/CMP toolchain | build | ✓ (Phase 1-2 builds green) | per STATE |
| Android emulator / iOS simulator | human-verify | ✓ (used in 01.1-04) | — |
| `fonttools` (python) | only to regenerate Nunito statics | one-off, already generated in /tmp in this session (not persisted) | Planner: the Nunito static files must be created by a task (venv + `pip install fonttools`, instancer at wght=400/700); keep the instancer command in the task, files are committed |
| Network | download TTFs | ✓ | github raw URLs return 200 |

## Security Domain

Offline app, no network, no accounts, no secrets. Applicable ASVS: V5 input validation only, for bundled content: `Json.decodeFromString` + `validate(lesson)` guard against corrupted lesson files and produce the error screen instead of a crash; no `Regex` over user input (the highlighter only runs on bundled code and chip text). Fonts are OFL assets: include the license text. Do not commit `local.properties` or local paths (CLAUDE.md).

## Project Constraints (from CLAUDE.md)
- KMP + Compose Multiplatform shared UI; Android + iOS; offline; Russian UI strings as literals (no string resources); Kotlin code in content.
- Stack versions fixed: Kotlin 2.4.21, CMP 1.12.2, material3 1.12.0-alpha03, AGP 9.3.x, Gradle 9.7.1; no new dependencies without need (no DI container, no Ktor/Coil, no DataStore/Room in this phase).
- `FlowRow` plain (no `overflow`/`ContextualFlowRow`); content via `composeResources/files/` + `Res.readBytes`; `Json { ignoreUnknownKeys = true }`.
- Do not use `kotlinx.datetime.Instant/Clock`; use `kotlin.time`.
- Tests via `./gradlew :shared:allTests` / `testAndroidHostTest`; Compose UI tests skipped for MVP.
- GSD workflow: edits only through GSD commands; do not commit local paths/secrets.

## Sources

### Primary (HIGH)
- Repo files read: `03-UI-SPEC.md`, `REQUIREMENTS.md`, `02-03-SUMMARY.md`, `lesson/Model.kt`, `LessonReducer.kt`, `ExerciseEngine.kt`, `App.kt`, `ui/theme/Theme.kt`, `Highlight.kt`, `kb/MarkdownParser.kt`, `kb/ui/BlockView.kt`, `shared/build.gradle.kts`, `gradle/libs.versions.toml`, `l0.4.json`.
- https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html - fonts: `Font(resource, weight, style)` is `@Composable`, TTF/OTF/variable supported.
- Local downloads + fontTools cmap checks: Nunito variable (Cyrillic OK), JetBrains Mono TTFs (Cyrillic OK); GitHub API listings for `googlefonts/nunito` (variable only) and `JetBrains/JetBrainsMono`.

### Secondary (MEDIUM)
- https://youtrack.jetbrains.com/projects/CMP/issues/CMP-8791 and https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html - BackHandler vs NavigationBackHandler.
- https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType and compose-multiplatform-core PR #1255 - haptic types, iOS bridge.

### Tertiary (LOW)
- Variable-font axis behaviour via `Font(weight)` on CMP: no confirming source found; avoided by using static instances.

## Metadata
**Confidence:** Standard stack HIGH (nothing new); Architecture HIGH (engine API read directly); Pitfalls MEDIUM-HIGH (back handling and haptics on iOS unverified).
**Research date:** 2026-10-21. **Valid until:** 2026-11-20.

## User Decisions (confirmed 2026-10-21, during plan-phase)

- **A1 confirmed (XP):** `xpFor` per exercise: clean = 10, solved with hint(s) or retry = 5, unsolved = 0; lesson XP = sum (3 clean exercises = 30). Pure function, no storage (Phase 4 persists).
- **A2 confirmed (Reveal):** every Reveal shows headline + CodeBlock with this exercise's deciding holes highlighted; complexity, pitfalls and variations only on the last exercise of the lesson.
- **A6 (orchestrator default):** Home shows only the l0.4 node with «Начать»; no lesson list / locked nodes (Phase 4–5).
- **A7 (orchestrator default):** no extra safe-area padding inside lesson screens (App Scaffold already applies insets).
- No CONTEXT.md; 03-UI-SPEC.md is the locked contract. Nunito static TTFs must be generated in a task (fonttools varLib.instancer from the variable font) and committed with OFL license under third_party/fonts/.
