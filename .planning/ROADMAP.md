# Roadmap: Algolingo (Алголинго)

## Overview

Путь от пустого репозитория до уровня 0 «Разминка», который проходится целиком на Android и на iOS-симуляторе. Сначала каркас, который собирается на обеих платформах. Затем чистый движок и схема контента (TDD, без UI). На этой основе параллельно идут UI урока, прогресс и пайплайн контента. Финал — сквозной прогон всех 4 уроков на обеих платформах.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

- [x] **Phase 1: Project Skeleton** - KMP + Compose Multiplatform проект запускается на Android и iOS-симуляторе и читает JSON из ресурсов (completed 2026-10-21)
- [x] **Phase 01.1: Knowledge Base** (INSERTED) - Все 23 Obsidian-заметки читаются в приложении: разделы, спойлеры, ссылки, поиск (completed 2026-10-21)
- [x] **Phase 2: Content Schema & Engine** - Чистая модель контента, движок дырок и редьюсер урока, покрытые тестами (completed 2026-10-21)
- [x] **Phase 3: Lesson & Exercise UI** - Урок проходится в приложении: теория, упражнения «дырки», проверка, подсказки, эталон, итог (completed 2026-10-21)
- [x] **Phase 4: Progress & Review** - XP, streak, интервальные повторы и статусы уроков сохраняются между запусками (completed 2026-10-22)
- [x] **Phase 5: Level 0 Content & End-to-End** - Генератор контента из Obsidian, размеченный уровень 0, сквозной прогон на обеих платформах (completed 2026-10-22)
- [x] **Phase 6: Level 1 Content — Hash Map / Set** - Уроки 1.1–1.5 из заметки 05 размечены, отревьюены заказчиком и проходятся на обеих платформах (completed 2026-10-22)
- [ ] **Phase 7: Full Interview Knowledge Base** - Вся база подготовки к собесам (~217 заметок: алгоритмы, вопросы с собесов, матрицы, System Design) читается в приложении по разделам

## Phase Details

### Phase 1: Project Skeleton

**Goal**: Пользователь может установить и запустить одно и то же приложение на Android и на iOS-симуляторе
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: PLAT-01, PLAT-02
**Research**: needed (`--research-phase`): свежие пины версий, вывод KMP-визарда, AGP 9 split, Xcode User Script Sandboxing
**Success Criteria** (what must be TRUE):

  1. Приложение устанавливается и открывает стартовый экран на Android-эмуляторе/устройстве
  2. То же приложение (общий Compose UI) открывается на iOS-симуляторе
  3. Стартовый экран показывает данные, прочитанные из JSON-файла в `files/` ресурсов, на обеих платформах
  4. `./gradlew` (Android) и `xcodebuild` (iOS-симулятор) успешно собирают проект из чистого клона

**Plans:** 2/2 plans complete

Plans:
**Wave 1**

- [x] 01-01-PLAN.md — Wizard scaffold + pins + hello.json slice, run on Android emulator (PLAT-01)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-02-PLAN.md — iOS simulator run, README versions, clean-clone builds (PLAT-02)

### Phase 01.1: Knowledge Base (INSERTED)

**Goal**: Пользователь может читать и учить всю базу знаний из Obsidian (23 заметки) с телефона — в похожей структуре, со свёрнутыми спойлерами и поиском
**Mode:** mvp
**Depends on**: Phase 1 (тема/шрифты — по 03-UI-SPEC)
**Requirements**: KB-01, KB-02, KB-03, KB-04, KB-05, KB-06
**Research**: needed: markdown-рендерер для CMP (multiplatform-markdown-renderer: таблицы, код, кастомные блоки), разбор Obsidian callouts `> [!type]-` и `[[wikilinks]]` (в т.ч. `[[file|alias]]`, `[[file#heading]]`), NFC-нормализация кириллических имён
**Success Criteria** (what must be TRUE):

  1. Скрипт `tools/sync_notes` копирует все 23 заметки из папки Obsidian в ресурсы приложения (ASCII-имена, NFC) с индексом разделов; сборка не читает Яндекс.Диск
  2. Во вкладке «База знаний» заметки сгруппированы по разделам: Навигация (00–04) · Тренажёры (05–06) · Темы · Задачи с собесов
  3. Заметка открывается с заголовками, таблицами, кодом Kotlin с подсветкой и цветными callout'ами; `[!question]-` / `[!success]-` свёрнуты и раскрываются тапом
  4. Тап по `[[ссылке]]` открывает связанную заметку, «назад» возвращает обратно; битая ссылка не роняет приложение
  5. Поиск по названию и тексту заметок находит нужную заметку и открывает её; всё работает офлайн на Android и iOS-симуляторе

**Plans**: TBD
**UI hint**: yes

### Phase 2: Content Schema & Engine

**Goal**: Схема контента и семантика проверки зафиксированы и доказаны тестами до UI и разметки
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: LESS-03, LESS-05, EXER-05
**Success Criteria** (what must be TRUE):

  1. Рукописный `l0.4.json` (бинарный поиск) парсится в модель урока: одна задача, 2–3 упражнения-лесенки с растущим числом дырок
  2. Движок вставляет/убирает облачка и проверяет каждую дырку по её набору верных: результат ok / acceptable («принято, но…» с пояснением) / wrong (с `why`); тесты в `commonTest` проходят
  3. Одинаковые по тексту облачка (`mid`, `1`) ведут себя как отдельные объекты — тест на дубликаты проходит
  4. Редьюсер урока возвращает неверные упражнения в конец очереди (не более 2 повторов), перемешивает банк детерминированно по seed и считает оценку ✅ / 💡 / ❌; урок без подсказок тоже валиден

**Plans**: TBD

### Phase 3: Lesson & Exercise UI

**Goal**: Пользователь проходит урок с телефона без клавиатуры: читает теорию, собирает код из облачков, жмёт «Проверить» и сразу видит, где грабли
**Mode:** mvp
**Depends on**: Phase 2 (UI-SPEC / style sketch через `/gsd:ui-phase` до начала)
**Requirements**: LESS-02, LESS-04, LESS-06, EXER-01, EXER-02, EXER-03, EXER-04, EXER-06, EXER-07, EXER-08, EXER-09, EXER-10, UI-01
**Success Criteria** (what must be TRUE):

  1. Урок открывается 2–4 карточками теории, листаемыми «Дальше», затем идут упражнения с видимым прогресс-баром
  2. Пользователь видит условие и Kotlin-шаблон моноширинным шрифтом с подсветкой и дырками; тап по облачку вставляет его в выделенную/первую пустую дырку (в банке остаётся серый призрак), тап по вставленному возвращает его, тап по дырке выделяет её
  3. «Проверить» активна только при заполненных дырках; после проверки дырки зелёные / жёлтые «принято, но…» / красные, при ошибке выезжает панель с объяснением грабли и верным вариантом
  4. Подсказки открываются по одной (кнопка скрыта, если их нет); после решения показываются эталон, сложность, грабли и вариации
  5. В конце урока пользователь видит итог: ✅ / 💡 / ❌ и заработанные XP
  6. Интерфейс в игривом стиле по UI-SPEC, светлая и тёмная тема переключаются вместе с системой

**Plans:** 10/10 plans complete
**UI hint**: yes

Plans:
- [x] 03-01-PLAN.md — Fonts (Nunito + JetBrains Mono) and theme text styles
- [x] 03-02-PLAN.md — TDD pure UI-state functions (hole visuals, panel model, XP)
- [x] 03-03-PLAN.md — Back-handling spike and lesson plumbing
- [x] 03-04-PLAN.md — Shared components (ChunkyButton, TokenChip, CodeTemplate, ...)
- [x] 03-05-PLAN.md — Theory and exercise screens
- [x] 03-06-PLAN.md — Reveal and summary screens
- [x] 03-07-PLAN.md — Wire Home -> lesson -> Home
- [x] 03-09-PLAN.md — :design module with AlgoTheme tokens, fonts, highlight; shared switched to AlgoTheme
- [x] 03-10-PLAN.md — Reusable components into :design, no raw dp/sp in shared UI
- [x] 03-08-PLAN.md — Full build + device walkthrough (human-verify), runs last

### Phase 4: Progress & Review

**Goal**: Прогресс пользователя живёт между запусками и возвращает задачи на повтор через 1 / 3 / 7 дней
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: LESS-01, PROG-01, PROG-02, PROG-03, PROG-04, PROG-05
**Success Criteria** (what must be TRUE):

  1. На главной — список уроков уровня 0 со статусами «пройден / доступен / закрыт»; следующий урок открывается после прохождения предыдущего
  2. Пользователь получает XP (меньше при подсказках/повторах) и видит streak по локальной календарной дате; после перезапуска приложения XP, streak, пройденные уроки и повторы на месте
  3. Задача уходит на повтор 1 / 3 / 7 дней (✅ — шаг вперёд, 💡 — на месте, ❌ — сброс), ✅ на последнем шаге помечает её «освоено»; тесты с подставной датой (23:59→00:01, два урока в день, пропуск дня, часы назад) проходят
  4. «Повтор сегодня (N)» на главной запускает тот же плеер урока для задач с наступившим сроком

**Plans**: 4 plans

Plans:
- [x] 04-01-PLAN.md — Package gate + deps/minSdk 26 + pure progress engine (XP, streak, 1/3/7 SRS, codec) with fake-date tests
- [x] 04-02-PLAN.md — DataStore repository (AppGraph singleton, app-scope write on Done) + 🔥/⭐ header persisted across restarts
- [x] 04-03-PLAN.md — index.json catalog, statuses/due/homeModel + Home path (done/available/locked) + «Повтор сегодня (N)» review without theory
- [x] 04-04-PLAN.md — Autonomous device verification on Android emulator and iOS simulator (restart, due edit, corrupt file)
**UI hint**: yes

### Phase 5: Level 0 Content & End-to-End

**Goal**: Уровень 0 «Разминка» (0.1–0.4), сгенерированный из Obsidian-заметок и отревьюенный заказчиком, проходится целиком офлайн на Android и iOS-симуляторе
**Mode:** mvp
**Depends on**: Phase 2 (генератор и разметка; может идти параллельно с Phases 3–4); сквозной прогон (критерий 5) требует Phases 3 и 4
**Requirements**: CONT-01, CONT-02, CONT-03, CONT-04, CONT-05, CONT-06, PLAT-03
**Research**: needed (`--research-phase`): синтаксис `⟦⟧`-сайдкаров на всех 4 задачах, golden-тест парсера заметок, как запускаются тесты эталонов
**Success Criteria** (what must be TRUE):

  1. Скрипт-генератор из заметок `05` + сайдкаров разметки выдаёт `index.json` и файл на урок в ресурсах приложения; golden-тест парсера проходит
  2. Генератор падает на невалидном контенте: дырка без верного облачка, ложное облачко без объяснения/контрпримера, пропавший id урока, разметка без дырок не совпадает с эталоном заметки
  3. Уроки 0.1–0.4 имеют карточки теории, лесенки дырок и дистракторы из граблей; `l <= r`, `Int.MIN_VALUE`, `ushr` размечены как «принято, но…»; эталоны и принятые варианты проходят тесты на примерах из условия
  4. Заказчик отревьюил контент уровня 0 (теория, дистракторы, пояснения)
  5. Все 4 урока проходятся от начала до итога на Android и на iOS-симуляторе в режиме без сети, прогресс и повторы работают

**Plans:** 4/4 plans complete

Plans:
**Wave 1**

- [x] 05-01-PLAN.md — Generator tools/gen_content.py + golden/CONT-02 tests; l0.4 regenerated byte-identically; generated ReferenceSolutionsTest (CONT-01, CONT-02, CONT-05)

**Wave 2** *(blocked on Wave 1)*

- [x] 05-02-PLAN.md — Sidecars l0.1–l0.3 (theory, ladders, distractors, Int.MIN_VALUE / l <= r acceptable); 4-lesson index (CONT-01, CONT-03, CONT-04, CONT-05)

**Wave 3** *(blocked on Wave 2)*

- [x] 05-03-PLAN.md — Customer review checkpoint of level 0, edits applied and regenerated (CONT-06)

**Wave 4** *(blocked on Wave 3)*

- [x] 05-04-PLAN.md — Autonomous offline walkthrough of 0.1–0.4 on Android (airplane mode) and iOS simulator + static offline proof (PLAT-03)

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 01.1 → 2 → 3 → 4 → 5 (Phase 5 content work may start right after Phase 2)

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Project Skeleton | 2/2 | Complete    | 2026-10-21 |
| 01.1 Knowledge Base | 4/4 | Complete    | 2026-10-21 |
| 2. Content Schema & Engine | 3/3 | Complete    | 2026-10-21 |
| 3. Lesson & Exercise UI | 10/10 | Complete    | 2026-10-21 |
| 4. Progress & Review | 4/4 | Complete    | 2026-10-22 |
| 5. Level 0 Content & End-to-End | 4/4 | Complete    | 2026-10-22 |

### Phase 6: Level 1 Content — Hash Map / Set

**Goal:** Уровень 1 «Hash Map / Set» (1.1–1.5) сгенерирован из заметки 05 + сайдкаров, отревьюен заказчиком и открывается после 0.4 на Android и iOS-симуляторе
**Requirements**: CNV2-01 (уровень 1)
**Depends on:** Phase 5
**Success Criteria** (what must be TRUE):

  1. Сайдкары l1.1–l1.5: дырки, верные облачка, дистракторы из «Граблей» с объяснением и контрпримером; `gen_content.py --check` и тесты эталонов зелёные
  2. `index.json` перечисляет 0.1–0.4, затем 1.1–1.5; 1.1 открывается после 0.4, следующие — по порядку
  3. Заказчик отревьюил контент уровня 1 (теория, дистракторы, пояснения)
  4. Уроки 1.1–1.5 проходятся с чистой установки на Android (авиарежим) и iOS-симуляторе
**Plans:** 5/5 plans complete

Plans:
**Wave 1**

- [x] 06-01-PLAN.md — Generator: examples inside variant objects + getOrDefault shim (Native); lesson 1.1 (CNV2-01)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 06-02-PLAN.md — Lessons 1.2 «Первый неповторяющийся символ» and 1.3 «Анаграмма» (CNV2-01)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 06-03-PLAN.md — Lessons 1.4 Two Sum and 1.5 «Победители соревнования»; 9-lesson index (CNV2-01)

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 06-04-PLAN.md — Customer review of level 1 (blocking checkpoint) + edits (CNV2-01)

**Wave 5** *(blocked on Wave 4 completion)*

- [x] 06-05-PLAN.md — Autonomous Android (airplane) + iOS sim walkthrough 0.4 → 1.5; CNV2-01 Partial row (CNV2-01)

### Phase 7: Full Interview Knowledge Base

**Goal:** База знаний приложения покрывает всю подготовку к собеседованию из Obsidian-хранилища «Матрицы», а не только алгоритмы
**Requirements**: KB-07 (расширяет KB-01, KB-02, KB-05, KB-06 из Phase 01.1)
**Depends on:** Phase 6
**Scope (решено заказчиком 2026-10-22):** папки Алгоритмы, Вопросы с собесов (с подпапками), Матрицы (Kotlin/Java, Android, Compose, Coroutines, Многопоточность, Архитектура, KMP, Performance, Исключения, Общие вопросы), SystemDesign, «Подготовка к собеседованию», «СОДЕРЖАНИЕ». Не брать: Работа за рубежом, Work, Для работы, Excalidraw, .obsidian/.claude. Публикация в публичный репо одобрена.
**Success Criteria** (what must be TRUE):

  1. `sync_notes.py` рекурсивно обходит выбранные папки хранилища; id заметок стабильны (23 текущие заметки и ссылки на заметку 05 у генератора не ломаются)
  2. Разделы базы знаний строятся по папкам/темам (Алгоритмы, Kotlin/Java, Android, Compose, Корутины, Многопоточность, Архитектура, KMP, Performance, System Design, Вопросы с собесов…); поиск работает по всей базе
  3. [[Ссылки]] между заметками из разных папок кликабельны; заметки с картинками не ломают экран (картинка показана или заменена заглушкой)
  4. Тест бандла: все ~217 заметок декодируются и парсятся; прогон на Android и iOS-симуляторе: разделы, открытие, поиск, переход по ссылке
**Plans:** 2/3 plans executed

Plans:
**Wave 1**

- [x] 07-01-PLAN.md — Recursive allow-list sync (14 sections, stable ids) + inline images (ImageBlock) + ==highlight==; full-bundle test (KB-07, KB-01, KB-05)
- [x] 07-02-PLAN.md — Collapsible section headers + cached/remembered global search (KB-07, KB-02, KB-06)

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 07-03-PLAN.md — Autonomous Android + iOS sim walkthrough of the full KB; KB-07 Complete (KB-07, KB-02, KB-05, KB-06)
