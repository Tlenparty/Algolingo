# Корутины: вопрос-ответ

## 1. Ленивый запуск корутины

```kotlin
scope.launch(start = CoroutineStart.LAZY)
```

Корутина не стартует сразу — запустится при первом `job.start()` или `job.join()`.

## 2. Запуск в одном потоке

`newSingleThreadContext` создаёт контекст с **одним** выделенным потоком:

```kotlin
import kotlinx.coroutines.*

fun main() {
    val singleThreadContext = newSingleThreadContext("SingleThread")

    runBlocking {
        launch(singleThreadContext) {
            println("On single thread: ${Thread.currentThread().name}")
            delay(1000)
            println("Still same thread: ${Thread.currentThread().name}")
        }
    }

    singleThreadContext.close() // обязательно закрываем — иначе утечка потока
}
```

> [!note]
> `newSingleThreadContext` создаёт **отдельный поток**, который нужно явно закрывать (`close()`). Для разовых нужд лучше `limitedParallelism(1)` на существующем диспетчере — он не плодит новые потоки:
> ```kotlin
> val singleThread = Dispatchers.Default.limitedParallelism(1)
> ```

## 3. Как дождаться выполнения корутины

### 3.1. async + await (нужен результат)

```kotlin
fun main() = runBlocking {
    val deferred: Deferred<String> = async { someSuspendFunction() }
    val result = deferred.await() // ждём результат
    println(result)
}

suspend fun someSuspendFunction(): String {
    delay(1000)
    return "Hello from coroutine!"
}
```

### 3.2. launch + join (результат не нужен)

```kotlin
fun main() = runBlocking {
    val job = launch { someSuspendFunction() }
    job.join() // ждём завершения
    println("Coroutine has finished")
}

suspend fun someSuspendFunction() {
    delay(1000)
    println("Hello from coroutine!")
}
```

`launch` не возвращает результат; `join()` ждёт завершения, не блокируя поток (приостанавливает корутину).

### 3.3. withContext (выполнить блок и дождаться)

`withContext` меняет контекст и дожидается завершения блока, возвращая результат:

```kotlin
fun main() = runBlocking {
    val result = withContext(Dispatchers.IO) {
        someSuspendFunction() // выполняется в контексте IO
    }
    println(result)
}

suspend fun someSuspendFunction(): String {
    delay(1000)
    return "Hello from coroutine!"
}
```

## 4. На каком диспетчере делать маппинг данных?

Сеть — на `Dispatchers.IO`, маппинг (CPU-работа) — на `Dispatchers.Default`:

```kotlin
suspend fun fetchAndMapData(api: ApiService): List<User> {
    // 1. Сетевой запрос — Dispatchers.IO
    val response = withContext(Dispatchers.IO) {
        api.getUsers()
    }

    // 2. Маппинг — Dispatchers.Default (CPU-bound)
    return withContext(Dispatchers.Default) {
        response.map { dto ->
            User(id = dto.id, displayName = dto.name.uppercase())
        }
    }
}
```

> [!tip]
> Для лёгкого маппинга (несколько полей) переключение на `Default` избыточно — оверхед смены потока дороже самой работы. `Default` оправдан на тяжёлых преобразованиях больших списков. Современный Retrofit с suspend-функциями сам уходит с главного потока, так что явный `withContext(Dispatchers.IO)` вокруг запроса часто не нужен.

Связано: [[Coroutines]], [[Задачи на корутины]]
