---
phase: 09-language-switch-python-pilot
plan: 06
subsystem: e2e, device-verification
tags: [python, e2e, smoke, adb, idb]
requires: [09-04, 09-05]
provides:
  - "E2eScenarios.scenarioPython + E2eSmokeTest.python on Android and iOS"
  - "09-VERIFICATION-DEVICES.md: live-run protocol on both platforms"
affects: [AppGraph, E2eScenarios.launchApp]
tech-stack:
  added: []
  patterns: ["App recreation in a compose test: key(generation) { App(dataDir) }", "progress loaded before setContent in e2e"]
key-files:
  created:
    - .planning/phases/09-language-switch-python-pilot/09-VERIFICATION-DEVICES.md
  modified:
    - shared/src/commonTest/kotlin/com/tlenparty/algolingo/e2e/E2eScenarios.kt
    - shared/src/androidDeviceTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
    - shared/src/iosTest/kotlin/com/tlenparty/algolingo/e2e/E2eSmokeTest.kt
    - shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/ProgressRepository.kt
decisions:
  - "AppGraph caches the repository per dataDir. The app always passes one dir, so production behaviour is unchanged, and a second @Test with a fresh dir gets fresh progress"
  - "In-test 'recreate' drops and rebuilds the App composition via key(); the real process-restart persistence is checked on devices"
metrics:
  duration: ~45min
  completed: 2026-10-28
  tasks: 2
  files: 5
---

# Phase 9 Plan 06: E2E + live device verification Summary

`scenarioPython` now runs as a separate `@Test python` in both smoke classes. Fresh progress, switch to Python, check that other lessons show «Скоро на Python», play Python l1.4 to «Чисто!», recreate App and see Python still selected, switch to Kotlin and see l1.4 Done.

`tools/smoke.sh` is green on the Pixel 7 (adb) and the iPhone 17 Pro simulator. I ran the same path live on both platforms myself, including a real process kill and a cold start.

## Tasks

| # | Task | Commit |
|---|------|--------|
| 1 | scenarioPython in E2eScenarios and both E2eSmokeTest | 90c1e0d |
| 2 | Device run (adb + idb), 09-VERIFICATION-DEVICES.md | 80223d9 |

## Verification

- `./gradlew :shared:testAndroidHostTest :shared:compileTestKotlinIosSimulatorArm64` passed.
- `tools/smoke.sh` (ANDROID_SERIAL=29211FDH200046) exited 0 in 71 s. Android ran smoke and python, iOS ran smoke and python, all passed.
- Live run, every step green on both platforms (details in 09-VERIFICATION-DEVICES.md):
  - switch to Python
  - Soon caption shown, and tapping a Soon node does nothing
  - the SD tab under Python has no segment and no Soon
  - l1.4 is available on fresh progress
  - Python highlighting in the theory skeleton and the template
  - 2/3/5 holes solved from chips
  - «Чисто!» and +30 XP
  - force-stop/terminate and cold start: Python kept, l1.4 Done
  - on Kotlin, l1.4 is Done and opens the Kotlin version

## Deviations from Plan

**1. [Rule 3 - Blocking] AppGraph is keyed by dataDir.** It used to keep the first dataDir per process. The plan wants a separate test, and that test needs fresh progress, but it would have shared the repository (and progress) of `smoke()`. The cache is now a `(dir, repo)` pair. Production always passes the same `filesDir`, so nothing changes there.

**2. [Rule 1 - Bug, test harness] The Android `python` test failed on its first run.** The error was `Detected multithreaded access to SnapshotStateObserver`. The cause: DataStore's first emit, on a Default worker, resumed the test recomposer inline on that worker. That can only happen when the composition subscribes before the first load, which is the warm second test in the process. The test passed when run alone.
- **Fix:** `launchApp` now waits for `AppGraph.progress(dir).progress.first { it != null }` before `setContent`.
- **Production is not affected:** the app's effect dispatcher is the UI dispatcher.
- **After the fix:** the full class and smoke.sh were green.

**3. Recreation in the e2e is in-process.** `setContent` can be called only once per test. "Recreate" therefore bumps a `key()` around `App(dataDir)`: nav and all remembered state are rebuilt, but AppGraph keeps the repository, as it does on Activity recreation. Persistence across a real process restart was verified on the devices.

**4. A physical Android device was used, not an emulator.** The orchestrator named Pixel 7 `29211FDH200046`. Only the debug app's data was cleared (`pm clear com.tlenparty.algolingo.debug`).

## Observations (not fixed, out of scope)

- No `#` comment on device: the pilot content has none. `HighlightTest` covers it.
- Long template lines run off the right edge. This is intended: CodeTemplate scrolls horizontally.
- After `pm clear`, Android asks for notification permission again on a cold start once it has been denied a single time. This predates phase 9.

## Known Stubs

None.

## Self-Check: PASSED

- 09-VERIFICATION-DEVICES.md and 09-06-SUMMARY.md exist; commits 90c1e0d and 80223d9 exist.
