# Jetpack Navigation

**Navigation Component** — Jetpack-библиотека для навигации внутри приложения: один **NavController** управляет переходами и back stack по объявленному **графу**.

## Компоненты

| Понятие | Роль |
| --- | --- |
| **NavGraph** | граф назначений (destinations) и переходов |
| **NavController** | управляет навигацией и back stack |
| **NavHost** | контейнер, где отображается текущий destination (`NavHostFragment` / `NavHost` в Compose) |
| **destination** | экран (Fragment/composable) |
| **action / route** | переход между destination |

## Два мира

> [!info] Navigation для Fragment vs для Compose
> • **Fragment-навигация** — граф в XML (`nav_graph.xml`), Safe Args генерирует типобезопасные классы аргументов.
> • **Navigation Compose** — граф в коде, маршруты строками:
> ```kotlin
> NavHost(navController, startDestination = "feed") {
>     composable("feed") { FeedScreen(onItem = { navController.navigate("detail/$it") }) }
>     composable("detail/{id}") { back ->
>         DetailScreen(id = back.arguments?.getString("id"))
>     }
> }
> ```

> [!important] Type-Safe Navigation (Navigation 2.8+)
> Строковые маршруты с ручным парсингом аргументов — источник ошибок (опечатка в route = краш в рантайме). Современный подход — **type-safe navigation на `@Serializable`-классах/объектах**:
> ```kotlin
> @Serializable data class Detail(val id: String)
> composable<Detail> { val args = it.toRoute<Detail>() }
> navController.navigate(Detail(id = "42"))
> ```
> Компилятор проверяет типы аргументов — частый «что нового» вопрос.

## Что важно понимать

> [!tip] Back stack и single source of truth
> NavController — **единый владелец** back stack. Это убирает ручное управление транзакциями фрагментов (см. [[Fragment transaction]]) и его баги (потеря состояния, дубли). Поддерживает вложенные графы, deep links, передачу результата между экранами (`savedStateHandle`).

> [!warning] Передача данных — только примитивы/Parcelable, без тяжёлых объектов
> Аргументы навигации идут через `Bundle` (см. [[Сохранение состояние в android и Bundle]]) — передавать **id/ключи**, а не целые модели/bitmap. Большие данные — через общий слой (репозиторий/VM), по id. Это та же дисциплина, что в [[Передача данных между фрагментами]].

> [!note] Альтернативы в Compose/KMP
> Для мультиплатформы и более «компонентной» навигации берут **Decompose** или **Voyager** (см. [[KMP. Compose Multiplatform]] и [[MVI библиотеки]]) — они дают навигацию, переживающую конфигурацию, и удобную для тестов структуру, чего «ванильный» NavController в KMP пока даёт не везде.

Связано: [[Fragment transaction]], [[Activity и Fragment]], [[Передача данных между фрагментами]], [[Сохранение состояние в android и Bundle]], [[MVI библиотеки]], [[KMP. Compose Multiplatform]]
