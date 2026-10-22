# KMP. Сборка и доставка iOS-приложения

Частый вопрос на собесе по KMP: **«За счёт чего собирается iOS-приложение и можно ли это сделать из Android Studio?»** Короткий ответ: Android Studio (с KMP-плагином) лишь **оркеструет** процесс, но фактическая компиляция, подпись и установка iOS-приложения выполняются **toolchain'ом Apple (Xcode + iOS SDK + codesign), который работает только на macOS**.

> [!important] Главный тезис
> **Без Mac собрать iOS-приложение нельзя.** Kotlin/Native умеет скомпилировать общий код в `.framework` на любой ОС в теории, но линковка `.app`, подпись и деплой требуют Apple-инструментов, доступных только на macOS. Android Studio на Mac + KMP-плагин может запустить iOS-приложение на симуляторе/устройстве — но **под капотом он зовёт Xcode**.

## Полный конвейер: от Kotlin до иконки на iPhone

### 1. Kotlin/Native компилирует общий модуль
`commonMain` + `iosMain` проходят через компилятор **Kotlin/Native**:

```text
Kotlin → Kotlin IR → LLVM IR → LLVM → нативный машинный код (Mach-O) → .framework
```

В отличие от Android (где Kotlin/JVM даёт байткод для виртуальной машины), здесь Kotlin компилируется в **настоящий нативный код под ARM64** через **LLVM** — без виртуальной машины. Результат — `*.framework` (или `XCFramework` для нескольких архитектур). Промежуточный формат библиотек Kotlin/Native — **`.klib`**.

Gradle-задачи, которые это делают:
- `linkDebugFrameworkIosArm64` / `linkDebugFrameworkIosSimulatorArm64` — собрать framework под нужную архитектуру;
- `embedAndSignAppleFrameworkForXcode` — собрать **и** встроить framework в Xcode-сборку (вызывается из Xcode, см. ниже);
- `assembleXCFramework` — упаковать все архитектуры в один XCFramework.

### 2. Xcode собирает само приложение
Дальше работает **Xcode** (`xcodebuild` под капотом):
1. **Build Phase «Run Script»** в Xcode-проекте вызывает Gradle:
   ```sh
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
   Эта задача компилирует Kotlin-framework и кладёт его туда, где его подхватит линкер Xcode.
2. **Компиляция Swift/ObjC** — UI-код приложения (SwiftUI/UIKit) компилируется компилятором Swift.
3. **Линковка** — Swift-код линкуется с Kotlin-framework (через сгенерированные Objective-C заголовки — см. [[KMP. iOS interop и корутины]]).
4. Результат — **`.app`** (bundle с бинарём, ресурсами, `Info.plist`).

### 3. Code signing (подпись) — обязательный шаг для устройства
Чтобы iOS запустил приложение на **реальном устройстве**, оно должно быть подписано:
- **Сертификат разработчика** (Development/Distribution) из Apple Developer аккаунта;
- **Provisioning profile** — связывает App ID + сертификат + список **UDID устройств** (для development);
- утилита **`codesign`** подписывает `.app`.
На **симуляторе** подпись не нужна — он исполняет код нативно на Mac без проверки сертификата.

### 4. Упаковка и доставка
- Для распространения `.app` упаковывается в **`.ipa`** (по сути zip с подписанным bundle).
- **На устройство** доставка идёт через: Xcode (USB/Wi-Fi → `devicectl`/`ios-deploy`), **TestFlight** (бета), **App Store** (релиз), или ad-hoc/enterprise-распространение.

## Что реально умеет Android Studio

> [!tip] Роль KMP-плагина в Android Studio
> Плагин **Kotlin Multiplatform** (преемник KMM-плагина) добавляет в Android Studio **run-конфигурацию для iOS**. При запуске на Mac он:
> 1. дергает Gradle для сборки Kotlin-framework;
> 2. вызывает **`xcodebuild`** для сборки `iosApp`;
> 3. поднимает **iOS-симулятор** (или ставит на подключённое устройство) и запускает приложение — всё не выходя из Android Studio.
> Но каждый из этих шагов опирается на установленный **Xcode + Command Line Tools на macOS**. Без них кнопка запуска iOS работать не будет.

> [!warning] Типичные заблуждения
> • **«KMP компилирует Kotlin в Swift»** — нет. Kotlin/Native компилирует в нативный бинарь и экспортирует **Objective-C API**; Swift вызывает его через ObjC-мост.
> • **«Можно собрать iOS на Windows/Linux из Android Studio»** — нет, нужен Mac (требование Apple, не KMP).
> • **«Android Studio полностью заменяет Xcode»** — нет, он его оркеструет. Тонкие настройки (signing, capabilities, Info.plist, build phases) всё равно правят в Xcode.
> • **Bitcode** — раньше Apple требовала промежуточный Bitcode; с Xcode 14 он **deprecated**, в современных KMP-сборках не нужен.

## Сравнение с Android-сборкой

| Этап | Android (из Android Studio) | iOS (KMP) |
| --- | --- | --- |
| Компилятор | Kotlin/JVM → байткод → **D8/R8** → DEX | **Kotlin/Native (LLVM)** → нативный ARM64 |
| Линковка UI | Gradle (AGP) собирает `.aar`/код в `.apk`/`.aab` | **Xcode** линкует Swift + Kotlin-framework в `.app` |
| Подпись | `apksigner` + keystore (`.jks`) | `codesign` + сертификат + provisioning profile |
| Артефакт | `.apk` / `.aab` | `.app` / `.ipa` |
| Где можно собрать | любая ОС | **только macOS** |
| Доставка | adb / Play Console / Firebase | Xcode / TestFlight / App Store |

> [!note] Для собеса в одну фразу
> «Общий Kotlin-код компилируется Kotlin/Native через LLVM в `.framework`; Xcode линкует его со Swift-кодом, `codesign` подписывает приложение сертификатом и provisioning-профилем, и оно ставится на устройство через Xcode/TestFlight. Android Studio с KMP-плагином может всё это запустить одной кнопкой, но только на Mac, потому что внутри зовёт `xcodebuild` и Apple-toolchain.»

Связано: [[KMP]], [[KMP. Source sets и Gradle]], [[KMP. iOS interop и корутины]], [[KMP. Вопросы с собеса]], [[BuildType, BuildFlavour, BuildVariant]], [[Разница в SDK]]
