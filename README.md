# Algolingo

Duolingo для алгоритмов: KMP + Compose Multiplatform (Android + iOS), решения на Kotlin собираются из облачков-токенов.

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
