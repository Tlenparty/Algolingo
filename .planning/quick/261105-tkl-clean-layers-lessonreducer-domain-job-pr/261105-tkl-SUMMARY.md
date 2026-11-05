---
quick_id: 261105-tkl
status: complete
---
# Итог: Clean layers

- `lesson/domain/LessonReducer.kt`: редьюсер, `Phase`, `LessonAction`, `LessonState`, а также `ScreenKey`/`screenKey` (были в `UiMapping`, но это чистая функция состояния, без неё редьюсер не переезжал). Presentation импортирует их из domain.
- `core/domain/markdown/MarkdownParser.kt`: пакет переименован, 10 файлов обновили импорт.
- `Job` в `ProgressRepository` оставлен осознанно: его ждут тесты записи и гонок.
- Проверка: граф импортов — нарушений 0; `:shared:testAndroidHostTest` 241/0; iOS main и test компилируются. На устройствах не гонялось: поведение не менялось, только пакеты.
