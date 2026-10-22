# Happens-before

Гарантии **happens-before** в JVM обеспечиваются **моделью памяти Java (Java Memory Model, JMM)**. JMM определяет правила, по которым операции в разных потоках взаимодействуют через память, и гарантирует согласованность и упорядоченность их выполнения.

Если действие `A` *happens-before* действия `B`, то результат `A` гарантированно виден в `B`. Ниже — конструкции, которые создают такие отношения.

## 1. Синхронизированные блоки (`synchronized`)

- Выход из `synchronized`-блока на объекте *happens-before* следующего входа в `synchronized`-блок на **том же** объекте: изменения первого потока будут видны второму.
- Гарантирует **взаимное исключение** и упорядоченность.

```kotlin
class Example {
    private var counter = 0

    @Synchronized
    fun increment() {
        counter++ // happens-before следующего входа в synchronized
    }

    @Synchronized
    fun getCounter(): Int = counter // видно всем потокам
}
// Изменения в increment() гарантированно видны в getCounter().
```

## 2. Volatile-переменные

- Запись в `volatile`-переменную *happens-before* любого последующего её чтения другим потоком.
- Чтение/запись `volatile` не кэшируются: значение всегда берётся из основной памяти.

```kotlin
class Example {
    @Volatile
    private var ready = false

    fun setReady() {
        ready = true // happens-before следующего чтения ready
    }

    fun checkReady() {
        if (ready) {
            println("Ready is true")
        }
    }
}
```

## 3. Запуск и завершение потоков

- `Thread.start()`: всё, что записано в память **до** вызова `start()`, видно запущенному потоку.
- `Thread.join()`: после завершения потока результаты его работы видны вызывающему потоку.

```kotlin
fun main() {
    var counter = 0

    val thread = Thread { counter = 42 }

    thread.start() // happens-before кода внутри потока
    thread.join()  // happens-before чтения counter ниже

    println(counter) // гарантированно 42
}
```

## 4. Блокировки (`Lock`)

Классы из `java.util.concurrent.locks` (например, `ReentrantLock`) дают те же happens-before гарантии, что и `synchronized`: `unlock()` *happens-before* следующего `lock()`.

```kotlin
import java.util.concurrent.locks.ReentrantLock

class Example {
    private var counter = 0
    private val lock = ReentrantLock()

    fun increment() {
        lock.lock()
        try {
            counter++
        } finally {
            lock.unlock() // happens-before следующего lock.lock()
        }
    }

    fun getCounter(): Int {
        lock.lock()
        try {
            return counter // гарантированно актуальное значение
        } finally {
            lock.unlock()
        }
    }
}
```

## 5. Конструкции из `java.util.concurrent`

Многие классы пакета дают внутренние happens-before гарантии:

- **Экзекьюторы (`Executor`, `ExecutorService`).** Изменения в потоке-отправителе до `submit()` / `execute()` видны потоку, выполняющему задачу.
- **Очереди (`BlockingQueue`, `ConcurrentLinkedQueue`).** `put()` *happens-before* соответствующего `take()`.
- **Синхронизаторы (`CountDownLatch`, `Semaphore`, `CyclicBarrier`).** `countDown()` / `release()` *happens-before* `await()` / `acquire()`.

## 6. Потокобезопасные коллекции

Коллекции из `java.util.concurrent` (`ConcurrentHashMap`, `CopyOnWriteArrayList`) предоставляют встроенные happens-before гарантии между записью и последующим чтением.

```kotlin
import java.util.concurrent.ConcurrentHashMap

val map = ConcurrentHashMap<String, String>()

fun main() {
    map["key"] = "value"     // happens-before чтения из map
    println(map["key"])      // гарантированно "value"
}
```

> [!tip] Как формулировать на собесе
> Happens-before — это **не про «выполнилось раньше по времени»**, а про **гарантию видимости и порядка**. Без такого отношения JIT-компилятор и процессор вправе переупорядочить операции или закэшировать значение, и один поток не увидит изменений другого. Ключевые «источники» happens-before: `synchronized`, `volatile`, `Thread.start/join`, `Lock`, инструменты `java.util.concurrent`.

Связано: [[Volatile & Synchronized, Atomic]], [[Правила семантики в jvm]]
