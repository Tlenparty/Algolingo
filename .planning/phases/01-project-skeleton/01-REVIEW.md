---
phase: 01-project-skeleton
reviewed: 2026-10-21T00:00:00Z
depth: standard
files_reviewed: 23
files_reviewed_list:
  - .gitignore
  - README.md
  - androidApp/build.gradle.kts
  - androidApp/src/main/AndroidManifest.xml
  - androidApp/src/main/kotlin/com/tlenparty/algolingo/MainActivity.kt
  - build.gradle.kts
  - settings.gradle.kts
  - gradle.properties
  - gradle/gradle-daemon-jvm.properties
  - gradle/libs.versions.toml
  - gradle/wrapper/gradle-wrapper.properties
  - iosApp/Configuration/Config.xcconfig
  - iosApp/iosApp/ContentView.swift
  - iosApp/iosApp/iOSApp.swift
  - iosApp/iosApp/Info.plist
  - shared/build.gradle.kts
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt
  - shared/src/commonMain/kotlin/com/tlenparty/algolingo/Platform.kt
  - shared/src/commonMain/composeResources/files/hello.json
  - shared/src/commonTest/kotlin/com/tlenparty/algolingo/HelloTest.kt
  - shared/src/androidMain/kotlin/com/tlenparty/algolingo/Platform.android.kt
  - shared/src/iosMain/kotlin/com/tlenparty/algolingo/MainViewController.kt
  - shared/src/iosMain/kotlin/com/tlenparty/algolingo/Platform.ios.kt
findings:
  critical: 0
  warning: 4
  info: 3
  total: 7
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-10-21
**Depth:** standard
**Files Reviewed:** 23
**Status:** issues_found

## Summary

The walking skeleton is sound. Public-repo hygiene is clean. I grepped all tracked files for `/Users/`, `/home/`, `sdk.dir`, passwords and `emulator-N`:
- No machine paths, secrets or keystores are committed.
- The only hit is a placeholder in the planning docs (`emulator-5554` is a generic default, and the phone serial is written as `<phone-serial>`).
- `local.properties`, `.idea`, `build`, `.kotlin` and `xcuserdata` are not tracked.
- `TEAM_ID` is empty.
- The pbxproj has no hardcoded team or bundle id; both come from the xcconfig.
- The Gradle wrapper pins a sha256.

The remaining findings are robustness and config issues.

## Warnings

### WR-01: Resource load/parse failure crashes the app (no error handling)

**File:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/App.kt:31`
**Issue:** `produceState { value = loadHello() }` has no try/catch. `Res.readBytes` throws `MissingResourceException` if the resource is not packaged. `decodeFromString` throws `SerializationException` on malformed JSON. An exception in the `produceState` producer coroutine propagates and crashes the host (Activity or iOS app) instead of showing a fallback. That is a poor failure mode for the "can read a bundled JSON" proof.
**Fix:**
```kotlin
val hello by produceState<Hello?>(null) {
    value = runCatching { loadHello() }.getOrNull()
}
```
Better, keep a distinct error state and rethrow `CancellationException`, because `runCatching` swallows it.

### WR-02: Alpha dependency pinned for material3

**File:** `gradle/libs.versions.toml:15`
**Issue:** `material3 = "1.12.0-alpha03"` is a pre-release artifact, pinned separately from `composeMultiplatform = 1.12.2`. Alpha APIs and ABI can break between releases. Mixing it with a stable CMP core risks a runtime `NoSuchMethodError` or resolution drift. It may be what the wizard emitted, but the repo should not silently depend on an alpha.
**Fix:** Use the material3 version that matches CMP 1.12.2 (the stable one, or the `compose.material3` alias from the plugin). If the alpha is deliberate, add a comment saying why.

### WR-03: iOS bundle id concatenates TEAM_ID, so a real signed build gets a different id than the README launch command

**File:** `iosApp/Configuration/Config.xcconfig:4`
**Issue:** `PRODUCT_BUNDLE_IDENTIFIER=com.tlenparty.algolingo$(TEAM_ID)` yields a malformed id such as `com.tlenparty.algolingoABCDE12345` once a team is set. It is correct only while `TEAM_ID` is empty. The README `simctl launch com.tlenparty.algolingo` then breaks for anyone who fills in `TEAM_ID`, and the id would differ from the Android `applicationId`. It is wizard boilerplate, but it contradicts the README's documented flow.
**Fix:** Remove the suffix.
```
PRODUCT_BUNDLE_IDENTIFIER=com.tlenparty.algolingo
```
Alternatively, document that `TEAM_ID` must stay empty for simulator builds. Prefer an uncommitted `Config.local.xcconfig` that is `#include?`d for the team.

### WR-04: Debug-tooling dependencies leak into non-debug configurations

**File:** `shared/build.gradle.kts:45-46, 66` and `androidApp/build.gradle.kts:18`
**Issue:** `androidMain.dependencies` adds `compose.uiTooling` as `implementation`. That puts it on the release classpath of the shared module, and `androidApp` then consumes it transitively. The app module already correctly uses `debugImplementation(uiTooling)`, so the shared-module entry defeats that and ships tooling in release APKs. `uiToolingPreview` is also declared in `commonMain` and `androidApp`, which is redundant.
**Fix:** Drop `implementation(libs.compose.uiTooling)` from `androidMain` and keep only the `androidRuntimeClasspath(...)` line at 66. Keep a single `uiToolingPreview` declaration.

## Info

### IN-01: Unused version-catalog entries

**File:** `gradle/libs.versions.toml:6-11, 19-25`
**Issue:** `androidx-appcompat`, `androidx-core-ktx`, `junit`, `kotlin-testJunit`, `androidx-testExt-junit`, `androidx-espresso-core` and `androidx-lifecycle-*` (the last in part) are declared but, from the build files read, unused. Dead catalog entries drift out of date.
**Fix:** Delete unused entries, or add them when first needed.

### IN-02: allowBackup and release minify off

**File:** `androidApp/src/main/AndroidManifest.xml:5`, `androidApp/build.gradle.kts:40`
**Issue:** `allowBackup="true"` and `isMinifyEnabled=false` are fine for a skeleton. Revisit before any release once the app stores user progress, because ADB and cloud backup will export it.
**Fix:** Set `allowBackup` deliberately, with `dataExtractionRules`, when persistence arrives.

### IN-03: Test does not cover the bundled resource itself

**File:** `shared/src/commonTest/kotlin/com/tlenparty/algolingo/HelloTest.kt:8-19`
**Issue:** The tests duplicate the string literals and exercise only `parseHello`. If `hello.json` drifts from the `Hello` schema (missing key), the tests still pass and the failure surfaces only at runtime (see WR-01). `readBytes` is not covered.
**Fix:** Add a host test that calls `loadHello()` via `runTest`. It needs `isIncludeAndroidResources`, which is already enabled. It is optional for a skeleton.

---

_Reviewed: 2026-10-21_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
