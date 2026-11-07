# bk6.1
note: 6.1 Token bucket
title: Token bucket
examples:
- assertEquals(listOf(true, true, false), TokenBucket(2, 1.0).let { b -> listOf(b.allow(0), b.allow(0), b.allow(0)) })
- assertEquals(listOf(true, false, true), TokenBucket(1, 1.0).let { b -> listOf(b.allow(0), b.allow(500), b.allow(1000)) })
- assertEquals(listOf(true, true), TokenBucket(1, 2.0).let { b -> listOf(b.allow(0), b.allow(500)) })
checks:
```kotlin
// оракул: миллитокены на Long; шаг времени кратен 250 мс, скорости 1/2/4 — ни одного округления в Double
for (cap in 1..3) for (rate in listOf(1.0, 2.0, 4.0)) case {
    val b = TokenBucket(cap, rate)
    var milli = cap * 1000L
    var last = 0L
    var t = 0L
    for (i in 0 until 40) {
        t += (i * 7 % 5) * 250L
        milli = minOf(cap * 1000L, milli + (t - last) * rate.toLong())
        last = t
        val expected = milli >= 1000L
        if (expected) milli -= 1000L
        assertEquals(expected, b.allow(t), "cap=$cap rate=$rate t=$t")
    }
}
// ровно одна секунда — ровно один токен
case { assertFalse(TokenBucket(1, 1.0).let { b -> b.allow(0); b.allow(999) }) }
case { assertTrue(TokenBucket(1, 1.0).let { b -> b.allow(0); b.allow(1000) }) }
// всплеск до ёмкости проходит, следующий нет
case { assertEquals(listOf(true, true, true, false), TokenBucket(3, 1.0).let { b -> listOf(b.allow(5), b.allow(5), b.allow(5), b.allow(5)) }) }
// долгая пауза не копит больше ёмкости
case { assertEquals(listOf(true, true, false), TokenBucket(2, 1.0).let { b -> b.allow(0); b.allow(0); listOf(b.allow(60_000), b.allow(60_000), b.allow(60_000)) }) }
// контрпримеры из причин
case { assertTrue(TokenBucket(1, 1.0).allow(0)) }
case { assertEquals(listOf(true, false), TokenBucket(1, 1.0).let { b -> listOf(b.allow(0), b.allow(0)) }) }
case { assertEquals(listOf(true, false), TokenBucket(1, 1.0).let { b -> listOf(b.allow(0), b.allow(1)) }) }
case { assertEquals(listOf(true, false, true), TokenBucket(1, 1.0).let { b -> listOf(b.allow(0), b.allow(0), b.allow(5000)) }) }
case { assertEquals(listOf(true, true, false), TokenBucket(2, 1.0).let { b -> listOf(b.allow(0), b.allow(0), b.allow(0)) }) }
```

## theory Когда token bucket
signals:
- «Не больше N запросов в секунду / минуту»
- «Всплески допустимы, но средняя скорость ограничена»
- «Защитить API от спама» (урок `sd1.2`)
body: Ведро вмещает `capacity` токенов и пополняется со скоростью `refillPerSec`; запрос забирает токен или получает отказ. Всплеск до `capacity` проходит разом, дальше ровно `refillPerSec` в секунду. Пополнение ленивое: считается в момент запроса, таймер не нужен.

## theory Каркас
skeleton:
```kotlin
// долить за прошедшее время
val elapsed = (nowMs - lastMs) / 1000.0
tokens = minOf(capacity.toDouble(), tokens + elapsed * refillPerSec)
lastMs = nowMs
// есть целый токен — тратим
if (tokens < 1.0) return false
tokens -= 1.0
return true
```
body: Токены дробные (`Double`): за 500 мс при 1 токене/с наливается 0.5. `minOf(capacity, …)` не даёт накопить больше ёмкости за долгую паузу. Состояние одного ведра живёт в памяти инстанса: за балансером ×N общий счётчик уезжает в Redis.

## theory Грабли
pitfalls:
- `tokens <= 1.0`: при ровно одном токене откажет — ведро на 1 токен не пропустит даже первый запрос
- `tokens == 0.0` вместо `< 1.0`: дробный остаток 0.5 сойдёт за токен
- Без `minOf(capacity, …)` после паузы ведро «накопит» тысячи токенов и лимит исчезнет
- `elapsed` в миллисекундах: долив в тысячу раз быстрее

## e1
```kotlin
// Случай 1: ведро 2, 1 токен/с: 0 мс → true, 0 мс → true, 0 мс → false
// Случай 2: ведро 1, 1 токен/с: 0 мс → true, 500 мс → false, 1000 мс → true
// Случай 3: ведро 1, 2 токена/с: 0 мс → true, 500 мс → true
class TokenBucket(private val capacity: Int, private val refillPerSec: Double) {
    // токенов в ведре сейчас (дробно)
    private var tokens = capacity.toDouble()
    // когда доливали в последний раз, мс
    private var lastMs = 0L

    // долить за прошедшее время
    private fun refill(nowMs: Long) {
        // сколько секунд прошло
        val elapsed = (nowMs - lastMs) / 1000.0
        // долив, но не выше ёмкости
        tokens = minOf(capacity.toDouble(), tokens + elapsed * refillPerSec)
        lastMs = nowMs
    }

    // пропустить запрос в момент nowMs?
    fun allow(nowMs: Long): Boolean {
        // сначала долить
        refill(nowMs)
        // меньше токена — отказ
        if (⟦tokens < 1.0⟧) return false
        // тратим один токен
        tokens -= 1.0
        return true
    }
}
```
- `tokens <= 1.0` [wrong]: при ровно одном токене откажет: `TokenBucket(1, 1.0).allow(0)` → `false`, ведро на 1 токен не пропустит ничего
- `tokens == 0.0` [wrong]: дробный остаток сойдёт за токен: `TokenBucket(1, 1.0)` после `allow(0)` пропустит `allow(500)` с 0.5 токена
- `tokens > 1.0` [wrong]: условие перевёрнуто, пустое ведро не отказывает: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
? Пропускаем, только если в ведре есть целый токен.
? Ровно один токен — тоже пропускаем: нужен строгий `<`.

## e2
```kotlin
// Случай 1: ведро 2, 1 токен/с: 0 мс → true, 0 мс → true, 0 мс → false
// Случай 2: ведро 1, 1 токен/с: 0 мс → true, 500 мс → false, 1000 мс → true
// Случай 3: ведро 1, 2 токена/с: 0 мс → true, 500 мс → true
class TokenBucket(private val capacity: Int, private val refillPerSec: Double) {
    // токенов в ведре сейчас (дробно)
    private var tokens = capacity.toDouble()
    // когда доливали в последний раз, мс
    private var lastMs = 0L

    // долить за прошедшее время
    private fun refill(nowMs: Long) {
        // сколько секунд прошло
        val elapsed = (nowMs - lastMs) / 1000.0
        // долив, но не выше ёмкости
        tokens = minOf(⟦capacity.toDouble()⟧, tokens + ⟦elapsed * refillPerSec⟧)
        lastMs = nowMs
    }

    // пропустить запрос в момент nowMs?
    fun allow(nowMs: Long): Boolean {
        // сначала долить
        refill(nowMs)
        // меньше токена — отказ
        if (tokens < 1.0) return false
        // тратим один токен
        tokens -= 1.0
        return true
    }
}
```
- `capacity` [compile]: `minOf(capacity, tokens + …)` смешивает `Int` и `Double`: не скомпилируется
- @2 `capacity` [wrong]: прибавляет всю ёмкость при каждом запросе, ведро всегда полное: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
- `tokens` [wrong]: `minOf(tokens, …)` никогда не доливает: `TokenBucket(1, 1.0)` после `allow(0)` откажет даже на `allow(5000)`
- @2 `tokens` [wrong]: `tokens + tokens` удваивает запас вместо долива: `TokenBucket(2, 1.0)` пропустит три `allow(0)` подряд
- `elapsed / refillPerSec` [wrong]: при 2 токенах/с долив вдвое медленнее: `TokenBucket(1, 2.0)` после `allow(0)` откажет на `allow(500)`
- @1 `elapsed / refillPerSec` [wrong]: потолок равен прошедшему времени, а не ёмкости: первый же `TokenBucket(1, 1.0).allow(0)` → `false`
- `refillPerSec` [wrong]: доливает скорость за любой интервал, даже нулевой: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
- @1 `refillPerSec` [wrong]: потолок равен скорости, а не ёмкости: `TokenBucket(2, 1.0)` откажет на втором `allow(0)`
- @1 `elapsed * refillPerSec` [wrong]: потолок — ёмкость ведра, а долив стоит справа: при первом запросе `elapsed = 0`, и `TokenBucket(1, 1.0).allow(0)` → `false`
- @2 `capacity.toDouble()` [wrong]: справа должен быть долив за время, а ёмкость — это потолок: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
? Долив = прошедшие секунды × скорость.
? Потолок — ёмкость ведра, приведённая к `Double`.

## e3
```kotlin
// Случай 1: ведро 2, 1 токен/с: 0 мс → true, 0 мс → true, 0 мс → false
// Случай 2: ведро 1, 1 токен/с: 0 мс → true, 500 мс → false, 1000 мс → true
// Случай 3: ведро 1, 2 токена/с: 0 мс → true, 500 мс → true
class TokenBucket(private val capacity: Int, private val refillPerSec: Double) {
    // токенов в ведре сейчас (дробно)
    private var tokens = capacity.toDouble()
    // когда доливали в последний раз, мс
    private var lastMs = 0L

    // долить за прошедшее время
    private fun refill(nowMs: Long) {
        // сколько секунд прошло
        val elapsed = ⟦(nowMs - lastMs) / 1000.0⟧
        // долив, но не выше ёмкости
        tokens = minOf(capacity.toDouble(), tokens + elapsed * refillPerSec)
        lastMs = nowMs
    }

    // пропустить запрос в момент nowMs?
    fun allow(nowMs: Long): Boolean {
        // сначала долить
        refill(nowMs)
        // меньше токена — отказ
        if (⟦tokens < 1.0⟧) return false
        // тратим один токен
        ⟦tokens -= 1.0⟧
        return true
    }
}
```
- `(nowMs - lastMs)` [wrong]: миллисекунды вместо секунд, долив в 1000 раз быстрее: `TokenBucket(1, 1.0)` после `allow(0)` пропустит `allow(1)`
- @2 `(nowMs - lastMs)` [compile]: в `if` нужен `Boolean`, а разность времени — `Long`: не скомпилируется
- @3 `(nowMs - lastMs)` [wrong]: токен не тратится, разность просто вычисляется и выбрасывается: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
- `tokens <= 1.0` [wrong]: при ровно одном токене откажет: `TokenBucket(1, 1.0).allow(0)` → `false`
- @1 `tokens <= 1.0` [compile]: `elapsed` станет `Boolean`, и `elapsed * refillPerSec` не скомпилируется
- @3 `tokens <= 1.0` [wrong]: сравнение вместо траты, токен остаётся: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
- `tokens == 0.0` [wrong]: дробный остаток сойдёт за токен: `TokenBucket(1, 1.0)` после `allow(0)` пропустит `allow(500)`
- @1 `tokens == 0.0` [compile]: `elapsed` станет `Boolean`, и `elapsed * refillPerSec` не скомпилируется
- @3 `tokens == 0.0` [wrong]: сравнение вместо траты, токен остаётся: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
- `tokens = 0.0` [wrong]: сжигает весь запас одним запросом: `TokenBucket(2, 1.0)` откажет на втором `allow(0)`
- @1 `tokens = 0.0` [compile]: присваивание — не выражение, `val elapsed = tokens = 0.0` не скомпилируется
- @2 `tokens = 0.0` [compile]: в `if` присваивание недопустимо: не скомпилируется
- @1 `tokens < 1.0` [compile]: `elapsed` станет `Boolean`, и `elapsed * refillPerSec` не скомпилируется
- @1 `tokens -= 1.0` [compile]: присваивание — не выражение, `val elapsed = tokens -= 1.0` не скомпилируется
- @2 `(nowMs - lastMs) / 1000.0` [compile]: в `if` нужен `Boolean`, а секунды — `Double`: не скомпилируется
- @2 `tokens -= 1.0` [compile]: в `if` присваивание недопустимо: не скомпилируется
- @3 `(nowMs - lastMs) / 1000.0` [wrong]: токен не тратится, секунды просто вычисляются и выбрасываются: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
- @3 `tokens < 1.0` [wrong]: сравнение вместо траты, токен остаётся: `TokenBucket(1, 1.0)` пропустит `allow(0)` и ещё раз `allow(0)`
? Сначала секунды, потом проверка целого токена, потом трата.
? Тратим ровно один токен: `-= 1.0`, а не обнуляем.

## e4 lines
? Долить, проверить, потратить, вернуть `true`.
