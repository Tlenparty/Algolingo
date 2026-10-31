# Algolingo

Duolingo для алгоритмов: KMP + Compose Multiplatform (Android + iOS), решения на Kotlin собираются из облачков-токенов.

## Модули

- `shared` — логика приложения и экраны: уроки, база знаний, главная, навигация, загрузка контента.
- `design` — дизайн-система `AlgoTheme`: токены (цвета, типографика, отступы, размеры, формы), шрифты, подсветка Kotlin и переиспользуемые компоненты без доменных типов. `shared` зависит от `design`, не наоборот.
- `androidApp` — Android-приложение (точка входа).
- `iosApp` — Xcode-проект iOS-приложения (точка входа, линкует фреймворк `Shared`).

## Сборка и запуск

| Компонент | Версия |
|-----------|--------|
| Kotlin | 2.4.21 |
| Android Gradle Plugin | 9.3.3 |
| Compose Multiplatform | 1.12.2 |
| Gradle | 9.7.1 |
| kotlinx-serialization-json | 1.12.0 |
| JDK | 21 |
| Xcode | 26.2 |
| iOS Simulator | iPhone 17 (iOS 26.2) |

Требования: JDK 21 в PATH/JAVA_HOME, Android SDK через `ANDROID_HOME` (например `~/Library/Android/sdk`) или `sdk.dir` в локальном, не коммитимом `local.properties`, Xcode 26.2+ для iOS.

### Android

```bash
./gradlew :androidApp:assembleDebug
adb -s <emulator-serial> install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Серийный номер (`adb -s`) обязателен всегда, когда к компьютеру подключён ещё и физический телефон.

### iOS (симулятор)

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17' \
  -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO build
xcrun simctl install <sim-udid> build/ios/Build/Products/Debug-iphonesimulator/Algolingo.app
xcrun simctl launch <sim-udid> com.tlenparty.algolingo
```

### Тесты

```bash
./gradlew :shared:testAndroidHostTest
```

Полный прогон (Android host + iOS-симулятор): `./gradlew :shared:allTests`.

### Урок и шрифты

- Урок 0.4 открывается с главной: узел «0.4» → «Начать» (теория → упражнения → итог).
- UI набран Nunito, код — JetBrains Mono (обе SIL OFL 1.1). Шрифты лежат в `design/src/commonMain/composeResources/font/`, лицензии — в `third_party/fonts/`.
- Команды пересборки статических начертаний Nunito — в `third_party/fonts/README.md`.

### Обновление заметок базы знаний

Заметки лежат в репозитории: `shared/src/commonMain/composeResources/files/kb/`. Правьте `.md` прямо там. Новую заметку добавьте туда же и запишите в `kb/index.json`. После этого пересоберите приложение.

### Контент уроков

Уроки собираются из двух источников: разметки `content/authoring/l0.N.md` (теория, ⟦дырки⟧, ложные облачка с объяснениями, подсказки, примеры для тестов; грамматика описана в заголовке `tools/gen_content.py`) и вшитой заметки 05 (`files/kb/n07ed966c.md`: условие, эталон, сложность, грабли).

```bash
python3 tools/gen_content.py                  # перегенерировать index.json, lessons/*.json и ReferenceSolutionsTest.kt
python3 tools/gen_content.py --check          # проверить, что закоммиченный вывод совпадает с исходниками
python3 -m unittest tools/test_gen_content.py # тесты генератора
```

Сгенерированные файлы коммитятся; Gradle Python не запускает. Если менялась заметка 05, перегенерируйте контент.
