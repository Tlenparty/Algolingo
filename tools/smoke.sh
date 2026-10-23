#!/usr/bin/env bash
# UI e2e smoke (E2eSmokeTest): every lesson + wrong chip + KB on the Android emulator and the booted iOS simulator.
# Does not boot devices. ANDROID_SERIAL picks the Android device when several are attached.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -n "${ANDROID_SERIAL:-}" ]; then
  [ "$(adb get-state 2>/dev/null || true)" = device ] || { echo "нет Android-эмулятора ($ANDROID_SERIAL не в сети)" >&2; exit 2; }
else
  adb devices 2>/dev/null | tail -n +2 | grep -qw device || { echo "нет Android-эмулятора" >&2; exit 2; }
fi
xcrun simctl list devices booted 2>/dev/null | grep -q Booted || { echo "нет запущенного iOS-симулятора" >&2; exit 2; }

status=0
./gradlew --continue \
  :shared:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=com.tlenparty.algolingo.e2e.E2eSmokeTest \
  :shared:iosSimulatorArm64Test --tests 'com.tlenparty.algolingo.e2e.E2eSmokeTest' --rerun || status=$?

echo "Android: shared/build/reports/androidTests/connected/androidMain/index.html"
echo "iOS:     shared/build/reports/tests/iosSimulatorArm64Test/index.html"
echo "Время: ${SECONDS} с, код: $status"
exit $status
