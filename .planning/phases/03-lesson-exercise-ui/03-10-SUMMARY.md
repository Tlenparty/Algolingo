---
phase: 03-lesson-exercise-ui
plan: 10
subsystem: design-system
tags: [kmp, compose, design-tokens, refactor]
requires: [03-09]
provides: [AlgoCard, "design.components (ChunkyBox, ChunkyButton, ChunkyStyle, TokenChip, ChipState, LessonProgressBar, CodeBlock)"]
affects: [03-08]
requirements: [UI-01]
key-files:
  created:
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/AlgoCard.kt
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/ChunkyButton.kt (git mv from shared)
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/TokenChip.kt (git mv from shared)
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/LessonProgressBar.kt (git mv from shared)
    - design/src/commonMain/kotlin/com/tlenparty/algolingo/design/components/CodeBlock.kt (git mv from shared)
  modified:
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/ui/components/{CodeTemplate,LessonTopBar,ResultPanel}.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/{ExerciseScreen,TheoryScreen,RevealScreen,SummaryScreen,ExitGuard,LessonHost,LessonScreen}.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/home/HomeScreen.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/kb/ui/{BlockView,CalloutView,KbListScreen,NoteScreen}.kt
decisions:
  - Components with no lesson/kb types moved to :design (ChunkyBox/ChunkyButton, TokenChip, LessonProgressBar, CodeBlock, plus new AlgoCard); CodeTemplate, LessonTopBar and ResultPanel stay in shared because they depend on lesson logic types / lesson copy / kb richText
  - ChunkyBox is public and takes shape: Shape instead of corner: Dp
  - The only computed dp left in shared is the BlockView table column width heuristic (allowed exception)
metrics:
  tasks: 3
  completed: 2026-10-21
---

# Phase 3 Plan 10: Design Components and Token Migration Summary

The reusable kit now lives in `:design` (`com.tlenparty.algolingo.design.components`): ChunkyBox/ChunkyButton/ChunkyStyle, TokenChip/ChipState, LessonProgressBar, CodeBlock and the new `AlgoCard`, which is a Surface with a `sizes.border` outline around a padded, spaced Column. The theory card, the hint card and the kb callout all render through AlgoCard. Every raw dp/sp/shape literal in `shared/src/commonMain/kotlin` was replaced by the full-form `AlgoTheme.spacing.*`, `AlgoTheme.sizes.*` and `AlgoTheme.shapes.*` tokens from the plan's map. The tokens hold the same values as the literals they replaced, so the UI should look unchanged.

## Commits
- 3f447d5 refactor(03-10): move reusable components into :design and add AlgoCard
- 3218ef7 refactor(03-10): tokenize lesson screens and lesson components
- 0c1d33f refactor(03-10): tokenize Home and knowledge-base screens

## Component placement
- Moved to :design: ChunkyButton.kt (ChunkyBox, ChunkyButton, ChunkyStyle), TokenChip.kt, LessonProgressBar.kt, CodeBlock.kt, and the new AlgoCard.kt.
- Stayed in shared/ui/components: CodeTemplate needs HoleVisual/Piece/templateLines from the lesson logic. LessonTopBar has lesson copy and a11y strings. ResultPanel needs PanelModel/PanelKind and kb richText. All three use only AlgoTheme tokens.

## Allowed exception
- `kb/ui/BlockView.kt`: the table column width `(chars * 8 + 16).coerceIn(80, 240).dp` is computed from text length, is not a design token, and is already marked with a ponytail comment. The literal-gate regex does not match it.

## Verification
- Literal gate: `grep -rnE '[0-9]\.(dp|sp)\b|Color\(0x|RoundedCornerShape\(' shared/src/commonMain/kotlin` finds 0 lines.
- Token usage gate: all 7 spacing, 18 sizes, 5 shapes (including `shapes.node`, which 03-09 already defined) and 3 emoji typography tokens have at least one call site. No UNUSED tokens, so nothing was deleted. The token names in AlgoTheme.kt match the plan exactly.
- In :design components, the only remaining `0.dp` literals are the ChunkyBox rest drop and the AlgoCard `spacing` default, as the plan allows.
- `./gradlew :shared:allTests :design:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug` passed.
- Emulator smoke test: `adb -s emulator-5554 install -r` (no installDebug). Screenshots of Home (pill «Начать» + node + caption) and the first theory card (AlgoCard, top bar, progress bar, «Дальше») matched the previous look. They are in /tmp and not committed.

## Deviations from Plan
- Smoke screenshot: captured the first theory card instead of the first exercise, because the lesson opens on theory. The exercise screen is covered by the 03-08 walkthrough.
- Otherwise the plan was executed as written. Dropping the `CodeBlock` `.copy(lineHeight = 20.sp)` changes nothing, because codeRegular already has lineHeight 20.sp.

## Known Stubs
None.

## Self-Check: PASSED
- The 5 design component files exist, and shared ui/components holds only CodeTemplate, LessonTopBar and ResultPanel.
- Commits 3f447d5, 3218ef7 and 0c1d33f exist, authored by tlenparty, with no AI trailers.
