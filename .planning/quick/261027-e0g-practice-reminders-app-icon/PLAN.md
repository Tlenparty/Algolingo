---
quick_id: 261027-e0g
slug: practice-reminders-app-icon
date: 2026-10-27
---
# Напоминания «пора потренироваться» + иконка Algolingo

Локальные уведомления (без FCM/APNs — офлайн, без бэкенда, без Apple-подписи) и собственная иконка.

## Tasks
1. Android: `PracticeReminder` (BroadcastReceiver) — пост уведомления + перевзвод; `AlarmManager.setWindow` на +24ч с окном 1ч; `POST_NOTIFICATIONS` запрос в `MainActivity`; ресивер в манифесте.
2. iOS: `UNTimeIntervalNotificationTrigger(24h, repeats)` с фиксированным id `practice` в `iOSApp.init` после `requestAuthorization`.
3. Иконка: маскот «{ ◉◉ }» — фигурные скобки, глаза, жёлтый клюв на `#58CC02`. Android — adaptive vector (+ monochrome для themed icon и small icon уведомления), iOS — PNG 1024. Удалить legacy mipmap PNG (minSdk 26 → всегда adaptive).
