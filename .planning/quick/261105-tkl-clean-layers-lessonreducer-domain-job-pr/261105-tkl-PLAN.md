---
quick_id: 261105-tkl
status: complete
---
# Clean layers: LessonReducer в domain, markdown в core/domain

По итогам ревью слоёв: правило зависимостей нигде не нарушено, три косметических замечания.
1. `LessonReducer` (с `Phase`, `LessonAction`, `LessonState`, `ScreenKey`, `screenKey`) из `lesson/presentation` в `lesson/domain`: чистая машина состояний, зависит только от domain.
2. `core/markdown` → `core/domain/markdown`, чтобы у всех пакетов один вид.
3. `Job` из `ProgressRepository` — не делаем: тесты репозитория (в том числе на гонки с реальными диспетчерами) ждут запись через `.join()`.
Проверка: `:shared:testAndroidHostTest`, компиляция iOS main и test.
