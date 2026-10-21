---
status: resolved
trigger: "iOS left-edge swipe-back gesture does nothing anywhere in the app (lesson exit dialog, KB note pop). Android BACK works."
created: 2026-10-21
updated: 2026-10-21
---

## Current Focus

hypothesis: CONFIRMED - not an app bug; idb-synthesized touches never trigger UIScreenEdgePanGestureRecognizer, so the repro tool cannot exercise the iOS back gesture
test: real Simulator mouse drag starting on the device bezel (CGEvent, /tmp/edgetool/drag 2385 570 2700 25)
expecting: KB note pops, lesson shows exit dialog
next_action: user confirms by hand-dragging from the left bezel in Simulator (or on device)

## Symptoms

expected: iOS left-edge swipe in lesson shows «Выйти из урока?»; on KB note pops to list
actual: nothing happens in either place; Android system BACK works
errors: none
reproduction: idb ui swipe 0 450 -> 330 450 duration 0.5 on simulator 4461DE0A; same swipe works in iOS Settings
started: unknown / since iOS build

## Eliminated

- hypothesis: CMP recognizers not attached/enabled with the SwiftUI UIViewControllerRepresentable wrapper
  evidence: runtime view dump - UIKitBackGestureRecognizer attached to UITransitionView (child of UIWindow), en=true on note/lesson, en=false on Home (stack size 1)
  timestamp: 2026-10-21
- hypothesis: deprecated ui-backhandler BackHandler / nav-compose 2.9.2 not wired to UIKit back
  evidence: both register on LocalNavigationEventDispatcherOwner; real edge drag pops NavHost and triggers ExitBackHandler
  timestamp: 2026-10-21

## Evidence

- timestamp: 2026-10-21
  checked: CMP ui-backhandler 1.12 BackHandler.jb.kt, nav-compose 2.9.2 nonAndroid PredictiveBackHandler
  found: both register handlers on LocalNavigationEventDispatcherOwner dispatcher (navigationevent). UIKitNavigationEventInput enables its UIScreenEdgePan recognizers when dispatcher has enabled handlers; recognizers are attached to the top-most view under UIWindow.
  implication: handler API choice (deprecated BackHandler vs NavigationBackHandler) alone should not explain a dead gesture

- timestamp: 2026-10-21
  checked: own UIScreenEdgePanGestureRecognizer probe on UIWindow (Home screen, CMP recognizers disabled), logged via NSLog
  found: idb `ui swipe 0..330` never fires it (x=0,1,3); CGEvent mouse drag in Simulator starting on the bezel fires it (state 1,2,...)
  implication: idb touches lack screen-edge origin; tool cannot test edge pans
- timestamp: 2026-10-21
  checked: control in iOS Settings with idb swipe starting at x=150 (mid-content)
  found: Settings still pops - iOS 26 UINavigationController content-area pop gesture, not edge pan
  implication: the Settings control did not prove idb can drive edge-pan gestures
- timestamp: 2026-10-21
  checked: unmodified app (HEAD 435e8f6), bezel mouse drag
  found: KB note -> list; lesson -> «Выйти из урока?» dialog over the lesson (route kept); «Остаться» returns to lesson. idb swipe on same screens: no effect.
  implication: feature works; reported bug is a test-harness artifact

## Resolution

root_cause: No app defect. idb synthesized touches do not trigger UIScreenEdgePanGestureRecognizer (CMP UIKitBackGestureRecognizer subclass); Settings control passed only because iOS 26 nav controllers pop on content-area swipes.
fix: none (no code change)
verification: bezel-origin mouse drag on simulator, unmodified build: both scenarios behave as expected
files_changed: []

## Resolution (2026-10-21)

Not an app bug — idb-synthesized touches never trigger UIScreenEdgePanGestureRecognizer. Verified with a real bezel mouse drag on HEAD 435e8f6: KB note pops to list; lesson shows «Выйти из урока?» and keeps the route. No code change. For future edge-swipe checks use the bezel mouse-drag helper, not idb.
