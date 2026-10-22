# ExecutorService

`ExecutorService` — интерфейс из `java.util.concurrent` для управления **пулом потоков** и выполнения фоновых задач. Позволяет запускать задачи асинхронно, контролировать количество потоков и управлять их жизненным циклом (запуск, завершение, отмена).

> **На собес одной фразой:** `ExecutorService` — пул переиспользуемых потоков; отделяет «что выполнить» (задача) от «на чём выполнить» (поток). Дешевле, чем плодить `Thread` вручную, и даёт очередь задач, `Future` и управление жизненным циклом.

## Основные компоненты

1. **Thread pool (пул потоков)** — переиспользует ограниченное число потоков для множества задач, снижая накладные расходы на создание новых.
2. **Отправка задач (task submission):**
   - `execute(Runnable)` — выполнить задачу **без** результата.
   - `submit(Callable)` — выполнить задачу и получить результат через `Future`.

## Использование

### 1. Создание пула
Проще всего — через фабричные методы `Executors`.
```kotlin
import java.util.concurrent.Executors

val executorService = Executors.newFixedThreadPool(4)  // пул из 4 потоков
```

### 2. Выполнение задачи без результата
```kotlin
executorService.execute {
    println("Выполняется в фоновом потоке: ${Thread.currentThread().name}")
}
```

### 3. Получение результата через `submit()` + `Future`
```kotlin
import java.util.concurrent.Callable
import java.util.concurrent.Future

val future: Future<String> = executorService.submit(Callable {
    Thread.sleep(1000)   // симулируем долгую задачу
    "Task Completed"
})

val result = future.get()   // блокирует до готовности результата
println(result)             // Task Completed
```

### 4. Завершение работы
После отправки всех задач пул нужно закрыть.
```kotlin
executorService.shutdown()      // доработать принятые задачи и закрыться
// executorService.shutdownNow() // прервать выполняемые немедленно
```
- `shutdown()` — не принимает новые задачи, доделывает уже отправленные.
- `shutdownNow()` — пытается прервать выполняемые задачи и вернуть невыполненные.

> ⚠️ Забыть `shutdown()` — частая ошибка: потоки пула могут не дать процессу завершиться (утечка ресурсов).

## Типы пулов

| Фабрика | Что делает |
|---|---|
| `newFixedThreadPool(n)` | фиксированное число потоков |
| `newCachedThreadPool()` | растёт под нагрузку, свободные потоки переиспользуются/удаляются |
| `newSingleThreadExecutor()` | один поток, задачи строго последовательно |
| `newScheduledThreadPool(n)` | задачи по расписанию / с задержкой |

## В контексте Android/Kotlin

`ExecutorService` — «классический» инструмент. В современном Kotlin/Android его роль занимают **корутины** + `Dispatchers` (легче, есть structured concurrency и отмена). Полезно знать связь: диспетчер корутин можно построить поверх пула через `executor.asCoroutineDispatcher()`.

## Связанные заметки
- [[Потоки]] — базовая работа с `Thread`.
- [[Главный поток]] — куда возвращать результат для обновления UI.
