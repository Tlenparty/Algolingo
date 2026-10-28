---
phase: 08-system-design-section
fixed_at: 2026-10-28T13:45:25Z
review_path: .planning/phases/08-system-design-section/08-REVIEW.md
iteration: 1
findings_in_scope: 5
fixed: 5
skipped: 0
status: all_fixed
---

# Phase 08: отчёт об исправлениях после code review

**Исправлено:** 2026-10-28T13:45:25Z
**Источник:** .planning/phases/08-system-design-section/08-REVIEW.md
**Итерация:** 1

**Итог:**
- В скоупе: 5 (WR-01, WR-02, IN-01, IN-02, IN-04). WR-03, IN-03 и IN-05 по решению оркестратора не исправлялись.
- Исправлено: 5
- Пропущено: 0

Проверка: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` и `python3 tools/gen_content.py --check` проходят.

## Исправленные

### WR-01: перестановка правильных облачков в design-шагах объясняется только текстом «Здесь нужно другое.»

**Файлы:** `shared/src/commonMain/composeResources/files/content/sd/sd0.1.json`, `sd1.1.json`, `sd1.2.json`
**Коммит:** bea90fd
**Что сделано:** в каждую дырку каждого SD-шага добавлен `wrongWhy` для каждого правильного облачка из других дырок (`ok` и `acceptable`). Всего около 100 коротких объяснений. Старые `wrongWhy` в sd1.2.e2 сохранены. sd0.1.e3 не менялся: там все три ответа принимаются в любой дырке. Новое правило `validate` не добавлялось. Тексты нужно показать заказчику на ревью.

### WR-02: «Грабли» и «Что спросят дальше» показываются на последнем шаге очереди

**Файлы:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/ui/RevealScreen.kt`
**Коммит:** 02922af
**Что сделано:** `showOutro = if (design) ex.id == state.lesson.exercises.last().id else isLastExercise(state)`. В code-уроках поведение прежнее. Статус: fixed, нужна ручная проверка логики: если ошибиться на финальном шаге, блок покажется под каждым его Reveal.

### IN-01: устаревший импорт и затенение имени `highlight`

**Файлы:** `CodeBlock.kt`, `TokenChip.kt` (design), `CodeTemplate.kt`, `ResultPanel.kt`, `ExerciseScreen.kt`, `TheoryScreen.kt`, `LessonScreen.kt`, `RevealScreen.kt` (shared)
**Коммит:** b05fdd1
**Что сделано:** параметр `highlight: Boolean` переименован в `syntax` везде, включая именованные аргументы. В `CodeBlock` FQN заменён на импортированный `highlight(...)`.

### IN-02: KDoc `loadCatalog` оторвался от функции

**Файлы:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/progress/Catalog.kt`
**Коммит:** 0be8448
**Что сделано:** константы стоят выше KDoc, и KDoc снова над `loadCatalog`.

### IN-04: `ex === lesson.exercises.last()` сравнивает по ссылке

**Файлы:** `shared/src/commonMain/kotlin/com/tlenparty/algolingo/lesson/Model.kt`
**Коммит:** c5900a9
**Что сделано:** цикл `for ((i, ex) in lesson.exercises.withIndex())` и проверка `i == lesson.exercises.lastIndex`.

---

_Fixed: 2026-10-28T13:45:25Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
