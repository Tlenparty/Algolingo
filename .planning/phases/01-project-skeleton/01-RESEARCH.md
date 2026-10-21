# Phase 1: Project Skeleton - Research

**Researched:** 2026-10-21
**Domain:** Kotlin Multiplatform + Compose Multiplatform scaffold (AGP 9 layout), Android + iOS simulator CLI builds
**Confidence:** HIGH. The whole recommended path was executed end to end on this machine in `/tmp/algotest`, with the pinned versions below. Results: Android APK built, host tests passed, `xcodebuild` succeeded for the simulator, and the app was launched on both the iOS 26.2 simulator and the Android 36 emulator. Screenshots of both show text decoded from `files/*.json`.

## Summary

The project skeleton can be generated **non-interactively** with one `curl` GET against the official KMP wizard backend (`https://kmp.jetbrains.com/generateKmtProject`). The response is a zip with the current AGP 9 layout:
- `androidApp/` uses `com.android.application`.
- `shared/` is the KMP module with `com.android.kotlin.multiplatform.library`, `android { androidResources { enable = true }; withHostTest {} }`, and only `iosArm64()` + `iosSimulatorArm64()`.
- `iosApp/` is an Xcode project with `ENABLE_USER_SCRIPT_SANDBOXING = NO` already set and a "Compile Kotlin Framework" phase that runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`.

The wizard currently emits Kotlin 2.4.20 / AGP 9.1.1 / CMP 1.12.1 / Gradle 9.5.1. Bumping to the STACK.md pins (Kotlin 2.4.21, AGP 9.3.3, CMP 1.12.2, Gradle 9.7.1) builds cleanly. [VERIFIED: local build 2026-10-21]

**Xcode 26.2 is fine. Do not upgrade Xcode for this phase.** Kotlin 2.4.x lists 26.4 as its *tested* Xcode version. The official docs say older Xcode versions "typically work well" and that the risk sits mostly in third-party cinterop, which this project doesn't use. [CITED: kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html] The full `xcodebuild` run with Kotlin 2.4.21 + Xcode 26.2 (17C52) succeeded, and the app ran on the iOS 26.2 simulator. [VERIFIED: local build] The simulator to use is **iPhone 17**. iPhone 16 does not exist on this machine, so the destination in PITFALLS.md is wrong for this machine.

The wizard names the shared module **`shared`**, not `composeApp`. The new DSL block inside `kotlin {}` is **`android { ... }`**, not `androidLibrary { ... }` as written in STACK.md. That makes the generated resources class `algolingo.shared.generated.resources.Res`, and the commands are `:shared:...` / `:androidApp:...`.

**Primary recommendation:** Generate with the curl command below, unzip into the repo root, and make these changes:
- Bump the version pins.
- Add kotlinx-serialization.
- Delete the sample code (`Greeting*`).
- Add `composeResources/files/hello.json`.
- Have `App()` read it through `Res.readBytes` + `Json.decodeFromString`.
- Fix the Gradle daemon JVM vendor pin.

Verify with the CLI commands in "Code Examples", including screenshots via `simctl io screenshot` / `adb exec-out screencap`.

## Project Constraints (from CLAUDE.md)

- Stack: Kotlin Multiplatform + Compose Multiplatform, shared UI for Android and iOS, one codebase.
- Offline: no network or backend at runtime (this phase reads only bundled resources).
- UI/theory in Russian; code in Kotlin.
- iOS: simulator only for MVP, no Apple Developer signing. Use `CODE_SIGNING_ALLOWED=NO` for CLI builds.
- Repo: github.com/Tlenparty/Algolingo (public), branch `main`.
- STACK.md guiding principle: minimal dependencies. No DI, no network library, no DB.
- PITFALLS.md: keep the project at `$HOME/work/Algolingo`, never under Yandex.Disk. Use only `iosArm64` + `iosSimulatorArm64`. Turn sandboxing off. Put resources in the module that generates `Res`. Record versions in the README.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PLAT-01 | User can install and run the app on Android (emulator/device) | Wizard `androidApp` + pins verified: `./gradlew :androidApp:assembleDebug` → `adb -s emulator-5554 install` → `am start` showed the JSON-backed screen on Android 36 |
| PLAT-02 | User can run the same app on the iOS simulator (shared Compose UI) | `xcodebuild ... -destination 'platform=iOS Simulator,name=iPhone 17' CODE_SIGNING_ALLOWED=NO build` → `simctl install/launch` showed the same `App()` composable on iOS 26.2 |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Start screen UI | `shared/commonMain` (Compose) | — | One composable `App()` serves both platforms (PLAT-02) |
| Reading bundled JSON | `shared/commonMain/composeResources/files` + `Res.readBytes` | — | Compose Resources packages files into the APK and the iOS .app (`Algolingo.app/compose-resources/...` verified) |
| JSON decoding | `shared/commonMain` (kotlinx.serialization) | — | Pure common code |
| Android entry | `androidApp` (`MainActivity` → `setContent { App() }`) | — | AGP 9 forbids an application in the KMP module |
| iOS entry | `iosApp` (SwiftUI `ComposeView` → `MainViewControllerKt.MainViewController()`) + `shared/iosMain` | — | Xcode project hosts the static `Shared.framework` |
| Platform name (optional proof of expect/actual) | `shared/androidMain`, `shared/iosMain` | — | The wizard's `getPlatform()`. Keep it: it costs nothing and proves expect/actual wiring |

## Standard Stack

### Core (verified against Maven Central / Google Maven metadata, 2026-10-21)
| Library / Tool | Version | Purpose | Notes |
|---------|---------|---------|-------|
| Kotlin (`kotlin.multiplatform`, `plugin.compose`, `plugin.serialization`) | 2.4.21 | Language, KMP, Compose compiler, serialization plugin | Latest stable; the wizard gives 2.4.20 → bump [VERIFIED: maven-metadata + local build] |
| AGP (`com.android.application`, `com.android.kotlin.multiplatform.library`) | 9.3.3 | Android build | Wizard gives 9.1.1. Compat guide lists 9.3.1 as max tested for Kotlin 2.4.21; 9.3.3 is a patch on that line and built fine [VERIFIED: local build]. Don't use 9.4.x |
| Compose Multiplatform (`org.jetbrains.compose`) | 1.12.2 | UI + Compose Resources | Wizard gives 1.12.1 → bump [VERIFIED] |
| material3 (`org.jetbrains.compose.material3:material3`) | 1.12.0-alpha03 | Theme/Text | Same as wizard [VERIFIED] |
| kotlinx-serialization-json | 1.12.0 | Decode JSON | Must be added. The wizard doesn't include it [VERIFIED] |
| lifecycle-viewmodel-compose / runtime-compose | 2.11.0 | Wizard includes them | Keep; later phases use them |
| androidx.activity:activity-compose | 1.13.0 | `MainActivity.setContent` | Wizard default (latest stable) |
| Gradle wrapper | 9.7.1 | Build | Wizard gives 9.5.1. The compat guide lists up to 9.7.0; 9.7.1 is a patch and built fine. Already cached in `~/.gradle/wrapper/dists` |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 | — | Wizard defaults. android-37.0 platform is installed |
| iOS deployment target | 18.2 (wizard pbxproj) | — | Fine for the 26.2 simulator. Kotlin 2.4 defaults to min iOS 15 |
| JDK | 21 | Gradle daemon | See the daemon-JVM pitfall |

**Not added this phase (YAGNI):** navigation, kotlinx-datetime, ViewModel usage. Add them in the phases that need them.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| curl to the wizard backend | Clone `github.com/Kotlin/KMP-App-Template` | Brings in a sample app with networking/DI to delete. More cleanup |
| curl to the wizard backend | Hand-write all Gradle + pbxproj files | The pbxproj is the error-prone part (script phase, sandboxing, framework). Don't do this |
| Upgrade to Xcode 26.4 | Keep 26.2 | Upgrading is a multi-GB download for no gain here. Revisit only if K/N emits errors |

## Package Legitimacy Audit

All artifacts are first-party JetBrains/Google/Gradle artifacts resolved from Maven Central / Google Maven. Their versions were confirmed via `maven-metadata.xml` on 2026-10-21, and all of them were downloaded and compiled in a real build. slopcheck covers npm/PyPI and doesn't apply to Maven coordinates.

| Package | Registry | Source | Disposition |
|---------|----------|--------|-------------|
| org.jetbrains.kotlin:* 2.4.21 | Maven Central | JetBrains | Approved |
| org.jetbrains.compose:* 1.12.2, material3 1.12.0-alpha03 | Maven Central | JetBrains | Approved |
| org.jetbrains.kotlinx:kotlinx-serialization-json 1.12.0 | Maven Central | JetBrains | Approved |
| com.android.tools.build:gradle 9.3.3 | Google Maven | Google | Approved |
| androidx.activity:activity-compose 1.13.0, org.jetbrains.androidx.lifecycle 2.11.0 | Google Maven / Maven Central | Google / JetBrains | Approved |

**Removed:** none. **Suspicious:** none.

## Architecture Patterns

### Data flow
```
App launch
  Android: MainActivity.onCreate -> setContent { App() }
  iOS:     iOSApp (SwiftUI) -> ContentView -> ComposeView -> MainViewControllerKt.MainViewController() -> ComposeUIViewController { App() }
        |
        v
App() [commonMain] -- produceState --> suspend loadHello()
        |                                   |
        |                     Res.readBytes("files/hello.json")   (bundled: APK assets / Algolingo.app/compose-resources/...)
        |                                   |
        |                     Json.decodeFromString<Hello>(bytes.decodeToString())
        v
Text(title), Text(subtitle), Text(getPlatform().name)
```

### Target project structure (after generation + edits)
```
Algolingo/
├── settings.gradle.kts         # include(":androidApp", ":shared")
├── build.gradle.kts            # plugins ... apply false (+ kotlinxSerialization)
├── gradle.properties           # wizard: config-cache, caching, -Xmx4096M
├── gradle/libs.versions.toml   # pins above
├── gradle/wrapper/*            # 9.7.1
├── gradle/gradle-daemon-jvm.properties  # see Pitfall 1
├── androidApp/                 # MainActivity only
├── shared/
│   ├── build.gradle.kts        # KMP + android{} + iosArm64/iosSimulatorArm64 + serialization
│   └── src/
│       ├── commonMain/kotlin/com/<pkg>/App.kt
│       ├── commonMain/composeResources/files/hello.json
│       ├── androidMain/…Platform.android.kt
│       ├── iosMain/…MainViewController.kt, Platform.ios.kt
│       ├── commonTest/…   androidHostTest/…   iosTest/…   (wizard tests)
└── iosApp/                     # iosApp.xcodeproj, Configuration/Config.xcconfig, Swift entry
```

### Anti-Patterns to Avoid
- **`androidTarget()` / `com.android.library` in shared:** Incompatible with the AGP 9 layout. The wizard output is already correct, so keep it.
- **`iosX64()`:** Removed in CMP 1.11+.
- **Putting the JSON in `src/commonMain/resources`:** Not bundled on iOS. Use `composeResources/files/`.
- **Creating `Json {}` inside the function:** Kotlin 2.4.21 emits the warning `JSON_FORMAT_REDUNDANT` (seen in the test build). Use a top-level `private val json = Json { ignoreUnknownKeys = true }`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Xcode project + Kotlin framework embedding | Hand-written pbxproj | Wizard zip (`iosApp/`) | It has the script phase, sandboxing set to NO, the xcconfig, and objectVersion 77 |
| Gradle module layout for AGP 9 | Copying from old tutorials | Wizard zip | Current DSL (`kotlin { android { … } }`, `withHostTest`, `androidRuntimeClasspath`) |
| Resource bundling on iOS | Copying JSON into the Xcode bundle | Compose Resources `files/` | Verified bundled at `Algolingo.app/compose-resources/composeResources/algolingo.shared.generated.resources/files/` |
| JSON parsing | Manual parsing | kotlinx.serialization | — |

## Common Pitfalls

### Pitfall 1: The Gradle daemon JVM pin silently downloads a JDK
**What goes wrong:** The wizard ships `gradle/gradle-daemon-jvm.properties` with `toolchainVersion=21` + `toolchainVendor=AZUL` + foojay URLs. This machine only has Homebrew OpenJDK 21, so on the test build Gradle **auto-downloaded Azul Zulu 21.0.11 into `~/.gradle/jdks`**. That breaks offline clean-clone builds and adds a hidden dependency.
**How to avoid:** Delete the `toolchainVendor=AZUL` line and keep `toolchainVersion=21`. The Homebrew JDK 21 (registered with `/usr/libexec/java_home`) then matches. Alternatively delete the file and rely on `JAVA_HOME`. After the change, run `./gradlew --stop` and check the daemon JVM in the build output or with `./gradlew -q javaToolchains`. [VERIFIED: observed download in this session; the vendor-line fix is [ASSUMED]. The executor should confirm no new entry appears in `~/.gradle/jdks`.]

### Pitfall 2: Several adb devices are attached, so commands hit the user's phone
**What goes wrong:** A physical device (`<phone-serial>`) is connected to this Mac. A bare `adb install` before the emulator came up installed the test APK on the **physical phone**. It was uninstalled right away. Once both are online, a bare `adb` fails with "more than one device".
**How to avoid:** Always pass `adb -s emulator-5554 …`. Get the serial from `adb devices | grep emulator`. Wait for `sys.boot_completed` on that serial.

### Pitfall 3: Catalog sed edits hit the plugin alias
**What goes wrong:** `composeMultiplatform` is both a version key and a plugin alias in `libs.versions.toml`. A line-anchored `sed s/^composeMultiplatform = .*/…/` also overwrote the `[plugins]` entry, and the build failed with "Invalid plugin notation". This happened in the test run.
**How to avoid:** Edit only within the `[versions]` section, or rewrite the whole TOML with the Write tool.

### Pitfall 4: Wrong simulator name
**What goes wrong:** `name=iPhone 16` (from PITFALLS.md) doesn't exist; only the iOS 26.2 runtime is installed.
**How to avoid:** Use `name=iPhone 17` (UDID <sim-udid> on this machine). Use `xcrun simctl list devices available` to re-check.

### Pitfall 5: The Xcode build phase can't find Java / sandboxing
**What goes wrong:** In GUI Xcode, `JAVA_HOME` isn't inherited, and sandboxing breaks the script.
**How to avoid:** The wizard pbxproj already sets `ENABLE_USER_SCRIPT_SANDBOXING = NO` (Debug and Release, verified). CLI `xcodebuild` from a shell inherits `JAVA_HOME`, which is verified to work. The daemon-JVM criteria file also helps here, because Gradle picks JDK 21 itself. If sandboxing is ever flipped on: set it back and run `./gradlew --stop`.

### Pitfall 6: Emulator cold boot shows "System UI isn't responding"
**What goes wrong:** The first screenshot after a cold boot showed an ANR dialog from System UI on top of the (correctly rendered) app.
**How to avoid:** Before the screenshot, wait about 20s after `boot_completed`, or dismiss with `adb -s $E shell input keyevent KEYCODE_BACK` and relaunch. It's cosmetic; don't treat it as an app failure.

### Pitfall 7: Wizard artifacts that must not be committed
`local.properties` (empty `sdk.dir=`) is in the zip, so delete it. `ANDROID_HOME=$HOME/Library/Android/sdk` is set in the environment, and the build worked without the file. `.idea/receipt.json` is covered by `.idea` ignore. Merge the wizard `.gitignore` into the existing one. It adds `**/build/`, `captures`, `*.xcodeproj/*` with `!project.pbxproj` etc. The existing `build/` entry already covers `build/` at any depth.

### Pitfall 8: Package/bundle id
The wizard takes `id=` as the package. Choose the final id now (e.g. `com.tlenparty.algolingo` [ASSUMED: user to confirm]). Changing it later touches the package dirs, `applicationId`, the shared namespace, and `PRODUCT_BUNDLE_IDENTIFIER` in `Config.xcconfig` (wizard appends `.Algolingo$(TEAM_ID)`, i.e. `<id>.Algolingo`). The `Res` package depends on the **project name + module** (`algolingo.shared.generated.resources`), not on the id.

### Pitfall 9: Wrapper jar/sha after the version bump
Editing `distributionUrl` without updating `distributionSha256Sum` fails validation, so in the test the sha line was deleted. Better: after unzipping, run `./gradlew wrapper --gradle-version 9.7.1` once (this also refreshes the jar and scripts), then commit.

## Code Examples

### 1. Generate the project (non-interactive, verified)
```bash
mkdir -p /tmp/algolingo-gen && cd /tmp/algolingo-gen
curl -sSf -G -o kmp.zip https://kmp.jetbrains.com/generateKmtProject \
  --data-urlencode 'name=Algolingo' \
  --data-urlencode 'id=com.tlenparty.algolingo' \
  --data-urlencode 'spec={"template_id":"kmt","targets":{"android":{"ui":["compose"]},"ios":{"ui":["compose"]}},"include_tests":true}'
unzip -q kmp.zip            # -> ./Algolingo/...
# copy contents into the repo root, preserving the existing .git, README.md, CLAUDE.md, .planning/
rsync -a --exclude local.properties --exclude .gitignore --exclude README.md Algolingo/ $HOME/work/Algolingo/
```
Notes:
- Adding `buildSystem=gradle` / `locale=en` params returned HTTP 400 in testing, so omit them.
- The endpoint and spec format were reverse-engineered from the wizard's web form (`spec` hidden field, `ProjectSpec{template_id, targets, include_tests}`). This is not a documented API [VERIFIED: worked 2026-10-21; stability LOW].
- Fallback if it breaks: download in a browser at kmp.jetbrains.com (Android + iOS "Share UI", tests on). The output is the same.

### 2. `shared/build.gradle.kts` deltas over the wizard
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)      // added
}
// commonMain.dependencies { … implementation(libs.kotlinx.serialization.json) }   // added
```
Root `build.gradle.kts`: add `alias(libs.plugins.kotlinxSerialization) apply false`.
TOML additions:
```toml
[versions]  kotlinx-serialization = "1.12.0"
[libraries] kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
[plugins]   kotlinxSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 3. Start screen reading JSON (verified on both platforms)
```kotlin
// shared/src/commonMain/kotlin/<pkg>/App.kt
import algolingo.shared.generated.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Hello(val title: String, val subtitle: String)

private val json = Json { ignoreUnknownKeys = true }

suspend fun loadHello(): Hello =
    json.decodeFromString(Res.readBytes("files/hello.json").decodeToString())

@Composable
fun App() {
    MaterialTheme {
        val hello by produceState<Hello?>(null) { value = loadHello() }
        Column(Modifier.safeContentPadding().fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hello?.title ?: "…", style = MaterialTheme.typography.headlineMedium)
            Text(hello?.subtitle ?: "")
            Text(getPlatform().name)
        }
    }
}
```
`shared/src/commonMain/composeResources/files/hello.json`:
```json
{ "title": "Алголинго", "subtitle": "Скелет: JSON прочитан из ресурсов" }
```
After deleting `Greeting.kt` / `GreetingUtil.kt`, remove the `compose_multiplatform` drawable import from App.kt. Deleting `composeResources/drawable/compose-multiplatform.xml` is optional.

### 4. Verification commands (all verified on this machine)
```bash
cd $HOME/work/Algolingo
# Android build + tests (~2.5 min cold)
./gradlew :androidApp:assembleDebug :shared:testAndroidHostTest --console=plain
# iOS common tests (optional, slower)
./gradlew :shared:iosSimulatorArm64Test

# iOS simulator build (~3 min cold; Gradle is invoked by the Xcode phase)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17' \
  -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO build
# -> ** BUILD SUCCEEDED **

# iOS run + screenshot
UDID=$(xcrun simctl list devices available | grep 'iPhone 17 (' | head -1 | grep -oE '[0-9A-F-]{36}')
xcrun simctl boot "$UDID" || true; xcrun simctl bootstatus "$UDID" -b
xcrun simctl install "$UDID" build/ios/Build/Products/Debug-iphonesimulator/Algolingo.app
xcrun simctl launch "$UDID" <PRODUCT_BUNDLE_IDENTIFIER>    # wizard: <id>.Algolingo
sleep 6; xcrun simctl io "$UDID" screenshot /tmp/ios.png

# Android run + screenshot (ALWAYS -s; a physical phone is attached)
ADB=~/Library/Android/sdk/platform-tools/adb
nohup ~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1 -no-snapshot-save -no-audio -no-boot-anim >/tmp/emu.log 2>&1 &
E=emulator-5554; $ADB -s $E wait-for-device
until [ "$($ADB -s $E shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 3; done
$ADB -s $E install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
$ADB -s $E shell am start -W -n <applicationId>/.MainActivity
sleep 5; $ADB -s $E exec-out screencap -p > /tmp/android.png
```
The executor should Read both PNGs and confirm the JSON title and subtitle are visible. That covers success criterion 3. For a text-level check, `adb shell uiautomator dump` + grep for "Алголинго" can be added if a non-visual assertion is wanted.

### 5. "Clean clone" check (success criterion 4)
```bash
rm -rf /tmp/algolingo-clone && git clone $HOME/work/Algolingo /tmp/algolingo-clone && cd /tmp/algolingo-clone
./gradlew --stop && ./gradlew :androidApp:assembleDebug && \
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO build
```
Cloning from the local repo (after commit) avoids needing GitHub auth.

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| `composeApp` module with `androidTarget()` + `com.android.application` | `androidApp` + `shared` with `com.android.kotlin.multiplatform.library`, `kotlin { android { } }` | Use module names `shared` / `androidApp` in all tasks |
| `androidLibrary { }` block name (STACK.md) | `android { }` (wizard output, AGP 9.1+) | STACK.md wording is outdated; follow the wizard |
| `debugImplementation(uiTooling)` in the KMP module | `dependencies { androidRuntimeClasspath(libs.compose.uiTooling) }` | Already in the wizard |
| Default iOS min 14 | Kotlin 2.4 default min 15; wizard pbxproj sets 18.2 | No action |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Removing `toolchainVendor=AZUL` makes Gradle use local Homebrew JDK 21 without downloading | Pitfall 1 | Low: Gradle downloads Zulu again (already cached now) |
| A2 | Package id `com.tlenparty.algolingo` | Pitfall 8 / Example 1 | Renaming later costs about 30 min; ask the user or accept the default |
| A3 | The `generateKmtProject` endpoint stays stable | Example 1 | Low: browser download fallback gives identical output |

## Open Questions (RESOLVED)

1. **Final applicationId / bundle id.** RESOLVED: `com.tlenparty.algolingo` (D-01, customer decision). Recommendation: `com.tlenparty.algolingo` (matches the GitHub owner). The planner can lock it as the default unless the user objects.
2. **Keep or drop the wizard's device-test config (`withDeviceTestBuilder`) and the sample tests?** RESOLVED: keep the sample tests (D-05). Recommendation: keep the sample tests. They are a ready `commonTest` and `androidHostTest` scaffold that Phase 2 uses. Device tests are harmless.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 21 | Gradle | ✓ | Homebrew OpenJDK 21.0.10 (+ Zulu 21.0.11 auto-downloaded into ~/.gradle/jdks during research) | — |
| Gradle 9.7.1 dist | wrapper | ✓ cached | 9.7.1 | — |
| Android SDK | build/emulator | ✓ `ANDROID_HOME=~/Library/Android/sdk` | platform 37.0, build-tools 36.1 | — |
| Android emulator AVD | PLAT-01 check | ✓ | `Medium_Phone_API_36.1`, `Pixel_4_XL` | Physical phone `<phone-serial>` (only with explicit `-s`) |
| Xcode | PLAT-02 | ✓ | 26.2 (17C52); verified working | Upgrade to 26.4 only if K/N errors |
| iOS simulator | PLAT-02 | ✓ | iOS 26.2, iPhone 17 | — |
| Kotlin/Native toolchain | iOS link | ✓ (2.4.21 was downloaded into ~/.konan during research) | 2.4.21 | — |
| Android Studio | optional IDE | ✓ at `~/Applications/Android Studio Rabbit 1 2026.2.1 Nightly.app` | Nightly | CLI path works without it |
| Network | first build (deps) | ✓ | — | Caches are now warm for these exact pins |

The global `~/.gradle/gradle.properties` (preferIPv4Stack, a routeApiUrl) and `init.gradle.kts` (scoped to `$HOME/work/aura`) are harmless for this project. Verified: the build succeeded with them present.

## Security Domain

Minimal surface: no network, no auth, no user input in this phase.
| ASVS Category | Applies | Control |
|---------------|---------|---------|
| V2/V3/V4 | no | — |
| V5 Input Validation | marginal | Bundled JSON is trusted build input; `ignoreUnknownKeys` + a typed model |
| V6 Crypto | no | — |
| Secrets in repo | yes | Don't commit `local.properties`; `Config.xcconfig` `TEAM_ID` stays empty |

## Sources

### Primary (HIGH)
- Local end-to-end build in `/tmp/algotest`, 2026-10-21: Gradle assemble + host tests, xcodebuild, simctl and adb runs, screenshots of both platforms.
- https://kmp.jetbrains.com (wizard). The generated zip (`.idea/receipt.json` spec) was inspected.
- https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html: Kotlin 2.4.20–2.4.21 ↔ Gradle 7.6.3–9.7.0, AGP 8.5.2–9.3.1, Xcode 26.4. Older Xcode "typically works".
- Maven Central / Google Maven `maven-metadata.xml`: kotlin-gradle-plugin 2.4.21, compose-gradle-plugin 1.12.2, AGP 9.3.3, kotlinx-serialization-json 1.12.0, material3 1.12.0-alpha03. services.gradle.org: 9.7.1.

### Secondary
- https://kotlinlang.org/docs/whatsnew24.html: Kotlin 2.4 iOS min 15, Xcode 26.4 support.
- .planning/research/STACK.md, PITFALLS.md (project research; corrected here on module/DSL names and simulator name).

## Metadata
- Standard stack: HIGH (built and run)
- Architecture: HIGH (wizard output, executed)
- Pitfalls: HIGH (1–4 and 6 observed in this session)
- **Valid until:** about 2026-11-20 (the wizard pins move monthly; re-check the curl output's versions)
