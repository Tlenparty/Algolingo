# Атомарность

Атомарность в многопоточном программировании означает, что операция выполняется **неделимо** — как единое целое: либо целиком, либо никак. Промежуточных состояний, видимых другим потокам, не существует.

> **На собес одной фразой:** атомарная операция неделима и не имеет промежуточных состояний → безопасна при конкурентном доступе без блокировок. В Kotlin/JVM это `Atomic*`-классы поверх CAS.

## Проблемы, которые решает атомарность

Атомарные операции устраняют [[Гонка данных|гонки данных]] (race conditions) и некорректный доступ к общим ресурсам:

1. **[[Гонка данных|Гонки данных]] (Race Conditions)** — несколько потоков одновременно меняют общий ресурс.
2. **Взаимодействие потоков (Thread Interference)** — потоки вмешиваются в операции друг друга → непредсказуемый результат.
3. **Несогласованные чтения/записи (Inconsistent Reads/Writes)** — поток видит частично выполненную операцию другого потока.

## Atomic-классы

`Atomic`-классы (`AtomicInteger`, `AtomicLong`, `AtomicBoolean`, `AtomicReference` и др.) предоставляют атомарные операции для своих типов. Под капотом — CAS (compare-and-swap). Подробнее — [[Atomic принципы]].

### `AtomicInteger` — атомарный счётчик

```kotlin
import java.util.concurrent.atomic.AtomicInteger

val atomicCounter = AtomicInteger(0)

fun main() {
    val threads = List(100) {
        Thread {
            repeat(1000) {
                atomicCounter.incrementAndGet()   // атомарный инкремент
            }
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }

    println(atomicCounter.get())   // 100000 — без гонок
}
```
`incrementAndGet()` атомарно увеличивает значение на 1 → корректный результат даже при 100 потоках. Обычный `Int` тут дал бы значение меньше 100000 из-за гонки.

### `AtomicBoolean` — атомарный флаг

```kotlin
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

val atomicFlag = AtomicBoolean(false)

fun main() = runBlocking {
    val jobs = List(10) {
        launch(Dispatchers.Default) {
            if (atomicFlag.compareAndSet(false, true)) {
                println("Корутина ${it}: установила флаг в true")
            } else {
                println("Корутина ${it}: флаг уже true")
            }
        }
    }
    jobs.forEach { it.join() }
}
```
`compareAndSet(false, true)` атомарно проверяет и меняет значение: только **один** поток/корутина сможет перевести флаг в `true` (классический «однократный запуск»).

### `AtomicReference` — атомарная ссылка на объект

```kotlin
import java.util.concurrent.atomic.AtomicReference

data class User(val name: String, val visits: Int)

val atomicUser = AtomicReference(User("Аня", 0))

fun main() {
    val threads = List(10) {
        Thread {
            // updateAndGet атомарно применяет функцию к текущему значению
            atomicUser.updateAndGet { current -> current.copy(visits = current.visits + 1) }
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }

    println(atomicUser.get())   // User(name=Аня, visits=10)
}
```
`updateAndGet { ... }` атомарно обновляет ссылку через CAS-цикл: если между чтением и записью значение изменил другой поток, операция повторяется. Так безопасно обновлять неизменяемый (`immutable`) объект целиком.

## Преимущества

1. **Потокобезопасность** без ручных блокировок — нет гонок данных.
2. **Производительность** — CAS обычно быстрее `synchronized` (низкоуровневые примитивы, нет блокировки потока).
3. **Простота** — код с атомарными переменными читабельнее, чем ручная синхронизация.

> ⚠️ Атомарны только **отдельные** операции (`incrementAndGet`, `compareAndSet`). Если нужно атомарно изменить **несколько** переменных сразу — атомиков мало, нужен `Mutex`/`synchronized`.
