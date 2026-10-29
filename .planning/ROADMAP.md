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
- [x] **Phase 7: Full Interview Knowledge Base** - Вся база подготовки к собесам (~217 заметок: алгоритмы, вопросы с собесов, матрицы, System Design) читается в приложении по разделам (completed 2026-10-22)
- [x] **Phase 8: System Design Section** - Вкладка «System Design»: шаги + схема из облачков, первый срез sd0.1 / sd1.1 / sd1.2 (completed 2026-10-28)
- [x] **Phase 9: Language Switch + Python Pilot** - Переключатель «Kotlin | Python» на Home, Python-конвейер контента, пилотный урок l1.4 на Python (completed 2026-10-28)
- [ ] **Phase 10: Python Content** - Остальные 8 уроков (l0.1–l0.4, l1.1–l1.3, l1.5) на Python с ревью заказчика
- [x] **Phase 11: Client-Server Basics Notes** - 12 заметок «Основы» (Kafka, Redis, nginx, Docker, HTTPS/TLS…) в Obsidian → База знаний (completed 2026-10-28)
- [x] **Phase 12: Client-Server Basics Lessons** - SD-уроки с облачками на закрепление «Основ» (completed 2026-10-28)
- [x] **Phase 13: SD Classics and Reliability** - 8 SD-уроков: авторизация, уведомления, лента, чат, платежи, потеря связи, несколько БД, наблюдаемость (completed 2026-10-28)

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

**Plans:** 3/3 plans complete

Plans:
**Wave 1**

- [x] 07-01-PLAN.md — Recursive allow-list sync (14 sections, stable ids) + inline images (ImageBlock) + ==highlight==; full-bundle test (KB-07, KB-01, KB-05)
- [x] 07-02-PLAN.md — Collapsible section headers + cached/remembered global search (KB-07, KB-02, KB-06)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 07-03-PLAN.md — Autonomous Android + iOS sim walkthrough of the full KB; KB-07 Complete (KB-07, KB-02, KB-05, KB-06)

### Phase 07.1: Content correctness & crash safety: wrong-chip proofs, edge cases, fuzz no-crash, safe KB load, fix l1.2/l1.3 (INSERTED)

**Goal:** Каждая оценка облачка в уроках доказана тестом, а приложение не падает ни на одном уроке и ни при сбое загрузки контента (закрывает GAP-1..5 из v1.0-MILESTONE-AUDIT.md)
**Requirements**: EXER-07, EXER-08, CONT-03, CONT-04, CONT-05, LESS-05, KB-01, KB-07, PLAT-03, CNV2-01 (усиливает)
**Depends on:** Phase 7
**Success Criteria** (what must be TRUE):

  1. Для каждого неверного облачка каждого урока есть тест: вариант с ним не компилируется или падает на примерах; причина в `wrongWhy` не противоречит реальной ошибке (GAP-1)
  2. Эталон и все ok/acceptable-варианты каждого урока проходят обязательные граничные случаи (пустой вход, один элемент, дубликаты и т.п. по типу задачи) на JVM и Native (GAP-2)
  3. Фазз-тест по всем урокам: любые последовательности тапов, «Проверить», подсказок и повторов не бросают исключений; monkey-прогон на Android без FATAL EXCEPTION (GAP-3)
  4. Битый или отсутствующий файл базы знаний не роняет приложение: уроки работают, KB показывает мягкую ошибку; KB не грузится целиком на старте (GAP-4)
  5. l1.2 e3 причины и l1.3 подсказка исправлены по 06-REVIEW WR-01/WR-02, `gen_content.py --check` зелёный (GAP-5)
  6. E2E-смоук одной командой проходит все 9 уроков через UI на Android-эмуляторе и iOS-симуляторе

**Plans:** 7/7 plans complete

Plans:

- [x] 07.1-01-PLAN.md — мягкая ленивая загрузка KB + fuzz редьюсера
- [x] 07.1-02-PLAN.md — генератор: метки поломок, checks:, pending
- [x] 07.1-03-PLAN.md — JVM-харнесс неверных облачков (WrongChipsTest)
- [x] 07.1-04-PLAN.md — граничные случаи + оракулы, документ находок
- [x] 07.1-05-PLAN.md — чекпоинт заказчика по находкам
- [x] 07.1-06-PLAN.md — применить вердикты, жёсткие гейты, прогоны на устройствах
- [x] 07.1-07-PLAN.md — UI e2e (Android + iOS) по всем урокам + tools/smoke.sh

### Phase 8: System Design Section

**Goal:** Вкладка «System Design»: уроки-«шаги + схема» (бэкенд-классика) собираются из облачков на текущем движке дырок; первый срез — sd0.1, sd1.1, sd1.2
**Mode:** mvp
**Spec:** docs/superpowers/specs/2026-10-28-system-design-section-design.md
**Requirements**: SD-01, SD-02, SD-03, SD-04
**Success Criteria** (what must be TRUE):

  1. `Lesson.kind = design` валидируется по своим правилам (2..4 шага, последний шаг == `solution.code`, строка ≤ 34 символов); тесты в commonTest/host проходят
  2. Третья вкладка «System Design» показывает путь из 3 SD-уроков из `files/content/sd/index.json`; XP/streak общие, повторы — на вкладке
  3. Шаги рендерятся без Kotlin-подсветки, с подписью шага; финальная схема из блоков влезает в экран без горизонтального скролла
  4. Эталон SD показывает собранный шаг, на последнем — грабли и «Что спросят дальше», без секции «Сложность»
  5. sd0.1, sd1.1, sd1.2 отревьюены заказчиком и проходятся на Android и iOS-симуляторе; `gen_content.py --check` по-прежнему зелёный

**Depends on:** Phase 7
**Plans:** 3/3 plans complete

Plans:
**Wave 1**

- [x] 08-01-PLAN.md — движок design + вкладка + UI + sd1.1 end-to-end (SD-01, SD-02, SD-03)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 08-02-PLAN.md — sd0.1 + sd1.2 + ревью заказчика (SD-04)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 08-03-PLAN.md — E2E smoke Android + iOS, скриншоты (SD-02, SD-04)

### Phase 9: Language Switch + Python Pilot

**Goal:** Пользователь переключает «Kotlin | Python» на Home, и пилотный урок l1.4 Two Sum собирается из Python-облачков; прогресс общий, выбор языка сохраняется (спека docs/superpowers/specs/2026-10-28-python-language-design.md, компоненты 1–7)
**Requirements**: TBD
**Depends on:** Phase 8
**Plans:** 6/6 plans complete

Plans:
**Wave 1**

- [x] 09-01-PLAN.md — CodeLang, LocalCodeLang, подсветка Python
- [x] 09-03-PLAN.md — генератор: py-sidecar, харнесс python3 -I, G12/G13

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 09-02-PLAN.md — Progress.lang, setLang, lessonPath по языку, фильтр каталога

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 09-04-PLAN.md — UI: сегмент на Home, узел «Скоро на Python», язык в урок
- [x] 09-05-PLAN.md — пилотный урок l1.4 на Python + ревью заказчика

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 09-06-PLAN.md — E2E-сценарий и прогон на Android/iOS

### Phase 10: Python Content

**Goal:** Все 9 уроков с кодом проходятся на Python: свои дистракторы и грабли, оракулы на python3, ревью заказчика по каждому уроку (спека docs/superpowers/specs/2026-10-28-python-language-design.md)
**Requirements**: PY-WR01, PY-BATCH-A, PY-BATCH-B, PY-BATCH-C, PY-E2E, PY-DEVICES
**Depends on:** Phase 9
**Plans:** 4/5 plans executed

Plans:

**Wave 1**

- [x] 10-01-PLAN.md — WR-01: генератор сверяет теги неверных облачков с фактом (G14), перепроверка пилота l1.4

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 10-02-PLAN.md — пачка A: l0.1, l0.2, l0.3 на Python + ревью заказчика

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 10-03-PLAN.md — пачка B: l0.4, l1.1, l1.2 на Python + ревью заказчика

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 10-04-PLAN.md — пачка C: l1.3, l1.5 на Python + ревью заказчика

**Wave 5** *(blocked on Wave 4 completion)*

- [ ] 10-05-PLAN.md — E2E всей Python-цепочки, smoke.sh, прогон на Pixel 7 и iPhone 17 Pro

**Cross-cutting constraints:**

- Заказчик принял тексты пачки

### Phase 11: Client-Server Basics Notes

**Goal:** Мобильный разработчик понимает бэкенд-кирпичи и клиент-серверное взаимодействие: 12 заметок «Основы» в Obsidian `SystemDesign/Основы/` синхронизированы в «Базу знаний» (раздел System Design)
**Mode:** mvp
**Requirements**: SD-05
**Success Criteria** (what must be TRUE):

  1. В хранилище `SystemDesign/Основы/` лежат 12 заметок `01`–`12`: путь запроса, HTTP, HTTPS/TLS/SSL, авторизация, nginx/LB/Gateway, Redis, Kafka и очереди, БД, Docker/K8s, real-time, файлы/CDN, наблюдаемость
  2. Каждая заметка: «зачем», аналогия из Android-мира, как применить в SD-задаче, грабли, вопросы с собеса под спойлером `[!question]-`; ссылки `[[…]]` между заметками и на SD-уроки/существующие SD-заметки резолвятся
  3. `СОДЕРЖАНИЕ.md` ссылается на все 12 заметок в разделе System Design
  4. `tools/sync_notes.py` переносит заметки в `files/kb/`; заметки открываются в приложении на Android и iOS-симуляторе; KB-тесты зелёные

**Depends on:** Phase 8
**Plans:** 2/2 plans complete

Plans:

- [x] 11-01-PLAN.md — заметки 01–06 (путь запроса, HTTP, TLS, авторизация, nginx/LB, Redis) в vault + sync + тесты + проверка на устройствах
- [x] 11-02-PLAN.md — заметки 07–12 (Kafka, БД, Docker/K8s, real-time, файлы/CDN, наблюдаемость) + СОДЕРЖАНИЕ + финальный sync и проверка

### Phase 12: Client-Server Basics Lessons

**Goal:** Теория из Phase 11 закрепляется SD-уроками с облачками на вкладке System Design (путь запроса, TLS-рукопожатие, компонент → зачем и др.)
**Mode:** mvp
**Requirements**: SD-06
**Success Criteria** (what must be TRUE):

  1. 4 новых design-урока sd0.3 «Строительные блоки», sd0.4 «Путь запроса», sd0.5 «HTTPS и TLS», sd0.6 «Real-time» валидны (validate, LessonContentTest) и стоят в SD-каталоге между sd0.1 и sd1.1
  2. У каждого неверного облачка и вероятной перестановки правильных есть объяснение; факты не противоречат заметкам Phase 11
  3. Тексты отревьюены заказчиком
  4. smoke.sh проходит все SD-уроки на Android-эмуляторе и iOS-симуляторе (физический телефон не используется)

**Depends on:** Phase 11
**Plans:** 2/2 plans complete

Plans:

- [x] 12-01-PLAN.md — 4 SD-урока (sd0.3-sd0.6) + каталог + ревью заказчика (SD-06)
- [x] 12-02-PLAN.md — smoke.sh на Android-эмуляторе и iOS-симуляторе (SD-06)

### Phase 13: SD Classics and Reliability

**Goal:** На вкладке System Design есть 8 новых design-уроков: классические задачи sd1.3 «Авторизация», sd1.4 «Уведомления», sd1.5 «Лента», sd1.6 «Чат», sd1.7 «Платежи» и надёжность sd2.1 «Потеря связи», sd2.2 «Несколько БД», sd2.3 «Наблюдаемость»
**Mode:** mvp
**Requirements**: SD-07
**Success Criteria** (what must be TRUE):

  1. 8 уроков валидны (validate, LessonContentTest) и стоят в SD-каталоге после sd1.2 в порядке sd1.3…sd1.7, sd2.1…sd2.3
  2. У каждого неверного облачка и каждой перестановки правильных есть объяснение; факты не противоречат заметкам «Основы» 01–12 и спеке SD
  3. Дайджест уроков в SUMMARY для неблокирующего ревью заказчика (заказчик: «давай сам»); факты сверены исполнителем с заметками «Основы»
  4. smoke.sh проходит все SD-уроки на Android-эмуляторе и iOS-симуляторе (физический Pixel 7 не используется)

**Depends on:** Phase 12
**Plans:** 3/3 plans complete

Plans:
**Wave 1**

- [x] 13-01-PLAN.md — sd1.3-sd1.7 (Авторизация, Уведомления, Лента, Чат, Платежи) + каталог, самосверка фактов (SD-07)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 13-02-PLAN.md — sd2.1-sd2.3 (Потеря связи, Несколько БД, Наблюдаемость) + каталог (SD-07)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 13-03-PLAN.md — E2E smoke на Android-эмуляторе и iOS-симуляторе, gen_content --check, host-тесты (SD-07)
