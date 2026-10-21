---
phase: 03-lesson-exercise-ui
plan: 08
subsystem: verification
tags: [kmp, compose, verification, human-verify]
requires: [03-10]
provides: [phase-3 gate approved on Android emulator and iOS simulator]
affects: [04]
requirements: [LESS-02, LESS-04, LESS-06, EXER-01, EXER-02, EXER-03, EXER-04, EXER-06, EXER-07, EXER-08, EXER-09, EXER-10, UI-01]
key-files:
  created: []
  modified:
    - README.md
decisions:
  - Phase 3 lesson flow approved by the user on the Android emulator (light and dark) and the iOS simulator
  - The listed minor visual issues are follow-ups. They do not block the phase.
metrics:
  tasks: 2
  completed: 2026-10-21
---

# Phase 3 Plan 08: Device Walkthrough Gate Summary

This was the gate for Phase 3. Automated tests and both platform builds passed from the current tree. README.md gained an «Урок и шрифты» section. The user then walked lesson 0.4 end to end on the Android emulator (light and dark) and on the iOS simulator, and approved it ("закрывай всё ок").

## Commits
- c84990e docs(03-08): add lesson and fonts note to README

## Verification
- Tests: testAndroidHostTest passed 92/92 and iosSimulatorArm64Test passed 87/87. `:design:compileKotlinIosSimulatorArm64`, `:androidApp:assembleDebug` and the xcodebuild simulator build were green.
- Android emulator (driven through adb after a cold boot, installed with `adb -s emulator-5554 install -r`; light and dark):
  - Home showed node 0.4 with «Начать», and the bottom bar was hidden inside the lesson. All 3 theory cards were shown.
  - e1, wrong `lo < hi`: red hole, «❌ Не совсем», a why text with a counterexample, and «Верно: lo <= hi». The exercise was requeued.
  - e2, `(lo + hi) ushr 1`: amber «👌 Принято, но…» with its note. The reveal said «✅ Решено! +10 XP», so an acceptable answer counts as clean.
  - e3: auto-scroll brought the selected hole into view, ghost chips kept their size, and the two `1` chips behaved as separate chips. Tapping a filled hole returned the chip and selected the hole. All holes went green with «✅ Отлично!».
  - The requeued e1 was solved, and its reveal gave +5 XP. The last reveal showed Сложность and Грабли.
  - Summary: «💡 Решено с подсказкой» +25 XP.
  - Hints: the card read «Подсказка 1 из 1», and 💡 was disabled when the counter reached 0.
  - System BACK opened «Выйти из урока?» with a Secondary ChunkyButton «Остаться».
  - Dark theme looked correct. Fonts were Nunito and JetBrains Mono, with no ligatures.
- iOS simulator: the app was installed and launched, and the dark theme Home rendered with Nunito from :design. The user checked the rest, including the lesson and edge-swipe back, and approved.

## Follow-ups (non-blocking)
- The `▶` on the Home node shows as a color emoji ▶️ on iOS. It needs U+FE0E text presentation or an icon.
- Hint text shows code such as `lo <= hi` without the code font. This is a content markup issue.
- When the template auto-scrolls to holes on the right, its closing braces scroll out of view.
- Not tested on a physical phone, because none was connected.

## Deviations from Plan
None. The plan was executed as written.

## Known Stubs
None.

## Self-Check: PASSED
- Commit c84990e exists, and README.md contains the «Урок и шрифты» section.
