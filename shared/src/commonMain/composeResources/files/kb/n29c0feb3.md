# Performance. Оптимизация Compose

Перформанс Compose = **минимум лишних рекомпозиций** + **дешёвые фазы Layout/Draw**. Теория стабильности — в [[Compose. Стабильность]] и [[KPI]]; здесь — практика оптимизации и измерения.

## Три фазы и отложенное чтение state

Compose-кадр: **Composition → Layout → Draw**. Главная идея оптимизации — **читать `State` как можно позже**, чтобы инвалидировать только нужную фазу:

> [!important] Defer reads — читай состояние в самой поздней фазе
> Если значение нужно только для отрисовки/позиции, читай его **в Layout/Draw**, а не в Composition:
> ```kotlin
> // ПЛОХО: чтение offset в композиции → рекомпозиция на каждый кадр анимации
> Box(Modifier.offset(x = scroll.value.dp))
> // ХОРОШО: лямбда-версия читает в Layout → композиция не инвалидируется
> Box(Modifier.offset { IntOffset(scroll.value, 0) })
> ```
> Аналогично `graphicsLayer { … }` для анимаций alpha/scale/translation — меняет только Draw, минуя рекомпозицию и Layout. Это даёт плавные 60–120 fps на анимациях.

## Чек-лист рекомпозиций

> [!tip] Что сокращает рекомпозиции
> • **Стабильные типы** параметров (`@Immutable`/`@Stable`, immutable-коллекции) — composable становится `skippable`. См. [[Compose. Стабильность]].
> • **`key`** в `LazyColumn`/`LazyRow` (`items(list, key = { it.id })`) — корректный диффинг при вставке/перестановке.
> • **`derivedStateOf`** — когда часто меняющийся state влияет на результат **редко** (напр. `firstVisibleItemIndex > 0`): пересчёт только при смене булева, а не на каждый пиксель скролла.
> • **Стабильные лямбды** — method reference или `remember`-нутая лямбда вместо новой на каждый вызов (в strong skipping компилятор оборачивает сам).
> • Выносить часто меняющийся параметр из большого `data class` (точечная инвалидация).

## Как измерять

> [!info] Recomposition counts
> • **Layout Inspector** в Android Studio показывает счётчики **recomposition / skip** по каждому composable вживую — видно, что перерисовывается зря.
> • **Compose Compiler Reports** (`-Pandroidx.compose.compiler...reportsDestination`) генерируют отчёт: какие функции `restartable/skippable`, какие классы `stable/unstable`.
> • Для воспроизводимых замеров скролла — [[Performance. Рендеринг, jank и Macrobenchmark|Macrobenchmark + FrameTimingMetric]].

## Debug vs Release — критично

> [!warning] Не меряй перформанс Compose в debug
> В **debug**-сборке Compose работает в разы медленнее: нет оптимизаций компилятора, включены лишние проверки/трейсинг, код не AOT-компилирован. Реальные замеры — только на **release** (`R8` + минификация) с [[Performance. Baseline и Startup Profiles|Baseline Profile]]. Иначе сделаешь ложные выводы.

> [!note] Strong Skipping Mode меняет приоритеты
> С Kotlin 2.0+ (strong skipping включён по умолчанию) даже composable с нестабильными параметрами становятся skippable (сравнение по ссылке), а лямбды оборачиваются автоматически. Поэтому гонка за стабильностью каждого класса стала менее критичной — но `@Immutable` всё ещё ускоряет за счёт сравнения по `equals`. Детали — [[KPI]].

Связано: [[Performance. Обзор и метрики]], [[Performance. Рендеринг, jank и Macrobenchmark]], [[Compose. Стабильность]], [[Compose. Recomposition]], [[KPI]], [[Compose. Оптимизация]], [[Compose. Snapshot]]
