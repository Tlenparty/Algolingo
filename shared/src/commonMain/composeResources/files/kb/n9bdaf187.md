# ReentrantLock, Semaphore, CyclicBarrier, CountDownLatch

В многопоточном программировании важен контроль доступа к ресурсам. Эти механизмы управляют потоками, но применяются в разных сценариях.

## 1. ReentrantLock — альтернатива `synchronized`

**Когда использовать:**

- Нужен **эксклюзивный доступ** к ресурсу (один поток в момент времени).
- Нужно **гибкое управление блокировкой** (`tryLock()`, `lockInterruptibly()`, таймауты).
- Один поток может **несколько раз захватить** ту же блокировку (реентрантность).

**Пример: несколько потоков пишут в общий ресурс**

```kotlin
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.*

class SharedResource {
    private val lock = ReentrantLock()

    fun accessResource(id: Int) {
        lock.lock() // захват блокировки
        try {
            println("Поток $id получил доступ к ресурсу")
            Thread.sleep(1000)
        } finally {
            lock.unlock() // освобождение блокировки
            println("Поток $id освободил ресурс")
        }
    }
}

fun main() = runBlocking {
    val resource = SharedResource()
    val jobs = List(3) { id ->
        launch(Dispatchers.Default) { resource.accessResource(id) }
    }
    jobs.forEach { it.join() }
}
```

**Вывод:** `ReentrantLock` нужен, когда с ресурсом одновременно работает только один поток, но требуется гибкость управления блокировкой.

## 2. Semaphore — ограничение числа потоков

**Когда использовать:**

- Нужно **ограничить количество потоков**, работающих с ресурсом одновременно.
- Например: пул подключений к БД, ограниченное число касс в магазине.

**Пример: парковка на 2 места**

```kotlin
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore

class ParkingLot(slots: Int) {
    private val semaphore = Semaphore(slots)

    fun parkCar(id: Int) {
        semaphore.acquire() // захват разрешения
        try {
            println("🚗 Машина $id припарковалась")
            Thread.sleep(2000)
        } finally {
            semaphore.release() // освобождение разрешения
            println("🚙 Машина $id уехала")
        }
    }
}

fun main() = runBlocking {
    val parkingLot = ParkingLot(2)
    val jobs = List(5) { id ->
        launch(Dispatchers.Default) { parkingLot.parkCar(id) }
    }
    jobs.forEach { it.join() }
}
```

**Вывод:** `Semaphore` полезен, когда ресурс может использовать ограниченное число потоков одновременно.

> [!tip] Для корутин — другой Semaphore
> `java.util.concurrent.Semaphore.acquire()` **блокирует поток**. В корутинах используйте `kotlinx.coroutines.sync.Semaphore` с suspend-методом `acquire()` (или `withPermit { ... }`) — он приостанавливает корутину, не занимая поток. Аналогично для эксклюзивной секции есть `kotlinx.coroutines.sync.Mutex` с `withLock { ... }`.

## 3. CyclicBarrier — ожидание всех потоков

**Когда использовать:**

- Нужно, чтобы **все потоки дошли до общей точки**, прежде чем продолжить.
- Полезно в этапных вычислениях (сначала все загрузили данные → затем вместе обрабатывают).
- Барьер можно **использовать повторно** (он «цикличный»).

**Пример: ожидание всех гонщиков перед стартом**

```kotlin
import java.util.concurrent.CyclicBarrier
import kotlinx.coroutines.*

class Race(private val barrier: CyclicBarrier) {
    fun prepare(id: Int) {
        println("🏁 Гонщик $id готовится...")
        Thread.sleep((1000..3000).random().toLong())
        println("✅ Гонщик $id ждёт старта...")
        barrier.await() // ждём остальных
        println("🚀 Гонщик $id стартует!")
    }
}

fun main() = runBlocking {
    val numRacers = 3
    val barrier = CyclicBarrier(numRacers) {
        println("🎉 Все гонщики готовы, стартуем!")
    }
    val jobs = List(numRacers) { id ->
        launch(Dispatchers.Default) { Race(barrier).prepare(id) }
    }
    jobs.forEach { it.join() }
}
```

**Вывод:** `CyclicBarrier` полезен, когда все потоки должны дождаться друг друга перед продолжением.

## 4. CountDownLatch — ожидание завершения потоков

**Когда использовать:**

- Один или несколько потоков должны **дождаться завершения других**.
- Например: главный поток ждёт окончания фоновых задач.
- В отличие от `CyclicBarrier`, **одноразовый** (счётчик не перезаряжается).

**Пример: ждём загрузку данных перед стартом**

```kotlin
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.*

class DataLoader(private val latch: CountDownLatch, private val id: Int) {
    fun loadData() {
        println("📦 Загрузка данных $id...")
        Thread.sleep((1000..3000).random().toLong())
        println("✅ Данные $id загружены")
        latch.countDown() // уменьшаем счётчик
    }
}

fun main() = runBlocking {
    val numTasks = 3
    val latch = CountDownLatch(numTasks)

    val jobs = List(numTasks) { id ->
        launch(Dispatchers.Default) { DataLoader(latch, id).loadData() }
    }

    latch.await() // ждём завершения всех загрузок
    println("🚀 Все данные загружены, запускаем систему!")

    jobs.forEach { it.join() }
}
```

**Вывод:** `CountDownLatch` полезен, когда ждём завершения нескольких потоков, прежде чем продолжить.

## Сравнение

| Инструмент         | Когда использовать                                                                                      | Что делает                                                                                                             |
| ------------------ | ------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------- |
| **ReentrantLock**  | Только один поток работает с ресурсом, но нужна гибкость управления блокировкой.                        | Захват/освобождение вручную (`lock()`/`unlock()`), таймауты, прерывания, реентрантность.                              |
| **Semaphore**      | Ресурс можно использовать ограниченному числу потоков одновременно.                                     | Ограничивает количество одновременных потоков (`acquire()`/`release()`).                                              |
| **CyclicBarrier**  | Все потоки должны дождаться друг друга перед продолжением.                                              | Блокирует потоки, пока не соберётся заданное число, затем отпускает всех сразу. **Можно использовать повторно.**       |
| **CountDownLatch** | Нужно дождаться завершения нескольких задач, прежде чем продолжить.                                     | Позволяет потоку ждать на `await()`, пока счётчик не дойдёт до 0. **Одноразовый.**                                     |

## Итог

- **ReentrantLock** — гибкое управление эксклюзивной блокировкой.
- **Semaphore** — ограничение числа одновременно работающих потоков.
- **CyclicBarrier** — все потоки ждут друг друга; барьер переиспользуемый.
- **CountDownLatch** — ждём завершения нескольких задач; одноразовый.

Связано: [[Как работает CountDownLatch]], [[Volatile & Synchronized, Atomic]], [[Процессы и потоки]]
