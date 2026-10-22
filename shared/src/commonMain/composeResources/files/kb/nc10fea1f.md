# Подготовка к алгоритмической секции (CodeRun / Яндекс)

Разбор по системе из [[00 Компас алгоритмов]] и [[01 Ключи и сигналы]]:
для каждой задачи — **сигнал в условии → ключ из компаса → метод**, затем каркас **U-E-P-B-O-C-S**
(Understand → Examples → **P**attern → **B**rute → **O**ptimize → **C**ode → **S**anity).

Решения на **Kotlin**. Все переменные — с понятными именами (никаких `u`, `v`, `pq`, `d`).

Ссылки на задачи:
- https://coderun.yandex.ru/problem/hardest-letter — «Самая сложная буква» (medium, implementation)
- https://coderun.yandex.ru/problem/simple-suggest — «Простая подсказка» (easy, counting/strings)
- https://coderun.yandex.ru/problem/flight — «Перелёт» (complex, implementation)
- https://coderun.yandex.ru/problem/summ-of-the-various — «Сумма различных» (data structures)
- https://coderun.yandex.ru/problem/palindroming-check — «Проверка палиндрома» (strings)
- https://coderun.yandex.ru/problem/rle-test — «Кодирование длин серий» (parsing/strings)

> Приём CodeRun: читаем через `BufferedReader`, на больших данных выводим один раз. `readLine()`/`println` в длинном цикле тормозят.

---

## Как я выбираю метод (быстрая навигация по сигналам)

| Что вижу в условии | Ключ (Компас) | Метод |
|---|---|---|
| «чаще всего», «частота», «уникальный», «различных» | тип 1 | **Hash Map / Set** |
| «палиндром», «симметрично», «с двух концов» | тип 2 | **Два указателя** |
| «интервалы», «расписание», «успеть/стыковка» | тип 8 | **Сортировка + жадность** |
| «маршрут», «дешевле/быстрее добраться», «пересадки» | тип 12 | **BFS / Дейкстра** |
| «разобрать строку», «распаковать», «посчитать длину» | — | **Линейный проход + парсер** (один O(n) проход) |

---

# 1. Простая подсказка (simple-suggest) — easy ✅

**Условие.** Дано n команд (1 ≤ n ≤ 100) из латинских букв. Вывести букву, **с которой команды начинаются чаще всего**. Если таких несколько — любую.
Пример: `sayHello, println, sleep, spendMoney, log` → `s` (3 раза первой).

### Сигнал → Ключ → Метод
- Маячок: «**чаще всего**» = частота. Ключ компаса **тип 1 (Hash Map / Set)**.
- **Вслух:** «Просят самый частый первый символ — это classic counting. Беру `HashMap<символ, счётчик>`, иду по командам, в конце беру максимум. O(n)».

### 1. Understand
Вход: n + n строк. Выход: один символ — самая частая первая буква. Tie-break: любой.

### 2. Examples
`[sayHello, sleep, spendMoney]` → все на `s` → `s`. Крайний: 1 команда → её первая буква.

### 3. Pattern
«Частота первого символа» → счётчик в Map (см. таблицу сигналов выше).

### 4–5. Brute → Optimize
Brute: для каждой буквы пройти все команды = O(26·n). Optimize: один проход со счётчиком = O(n).

### 6. Code
```kotlin
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val commandCount = reader.readLine().trim().toInt()

    val firstLetterFrequency = HashMap<Char, Int>()   // первая буква -> сколько раз встретилась
    repeat(commandCount) {
        val command = reader.readLine().trim()
        if (command.isNotEmpty()) {
            val firstLetter = command[0]
            firstLetterFrequency[firstLetter] = (firstLetterFrequency[firstLetter] ?: 0) + 1
        }
    }

    val mostFrequentLetter = firstLetterFrequency.maxByOrNull { entry -> entry.value }!!.key
    println(mostFrequentLetter)
}
```

### 7. Sanity / Сложность
Время **O(n)**, память **O(k)** (k — число разных первых букв ≤ 52).
Если бы tie-break был «лексикографически меньшая» — шёл бы `IntArray(128)` от меньшего кода к большему.

---

# 2. Сумма различных (summ-of-the-various) — easy ✅

**Условие.** Массив из n целых (1 ≤ n ≤ 100000, 0 ≤ aᵢ ≤ 10⁹). Вывести **сумму уникальных значений**.
Примеры: `[3,5,4]→12`; `[5,5,5,5,5]→5`; `[7,10,3,2,7,4,8,5,9,10]→48`.

### Сигнал → Ключ → Метод
- Маячок: «**различных / уникальных**» = наличие/дубликаты. Ключ **тип 1 (Hash Set)**.
- **Вслух:** «Нужны только уникальные — кладу в `HashSet`, суммирую то, что добавилось впервые. O(n)».

### 1. Understand
Уникальные значения — суммируем по одному разу каждое. ⚠️ Сумма до 10⁵·10⁹ = **10¹⁴** → нужен `Long`.

### 2. Examples
`[5,5,5,5,5]` → одно уникальное `5` → `5`. Крайний: все разные → сумма всех.

### 3. Pattern
«Уникальность» → `HashSet` (`add` возвращает `true`, если элемент новый).

### 4–5. Brute → Optimize
Brute: для каждого проверять, встречался ли раньше = O(n²). Optimize: Set даёт проверку за O(1) → O(n).

### 6. Code
```kotlin
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.StringTokenizer

fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val numberCount = reader.readLine().trim().toInt()

    val seenValues = HashSet<Int>(numberCount * 2)   // запас ёмкости — меньше рехешей
    var uniqueSum = 0L                               // Long! сумма большая

    val tokenizer = StringTokenizer(reader.readLine())
    repeat(numberCount) {
        val value = tokenizer.nextToken().toInt()
        if (seenValues.add(value)) {                 // add == true → значение новое
            uniqueSum += value
        }
    }
    println(uniqueSum)
}
```

### 7. Sanity / Сложность
Время **O(n)** в среднем, память **O(n)**. Главный подвох — тип суммы (`Long`, иначе переполнение `Int`).
Альтернатива: отсортировать и пропускать повторы — O(n log n), но Set быстрее.

---

# 3. Проверка палиндрома (palindroming-check) — easy ✅

**Условие.** Строка из английских букв и пробелов (1..500000). **Пробелы игнорируем, регистр не важен.** Вывести `It is a palindrome` / `It is not a palindrome`.
Примеры: `JmrrmJ`→да; `f AoAO  AF`→да (`faoaoaf`); `qOH hra`→нет.

### Сигнал → Ключ → Метод
- Маячок: «**палиндром**» = симметрия / сравнение с двух концов. Ключ **тип 2 (Два указателя)**.
- **Вслух:** «Палиндром — ставлю два указателя по краям, иду навстречу, пропускаю пробелы, сравниваю в нижнем регистре. O(n), память O(1)».

### 1. Understand
Сравнить строку с собой задом наперёд, игнорируя пробелы и регистр. Длина до 5·10⁵ → не плодить лишнюю память.

### 2. Examples
`f AoAO AF` → без пробелов `faoaoaf` → симметрично → да. Крайний: пустая/один символ → палиндром.

### 3. Pattern
Симметрия → два указателя навстречу (скелет из [[02 Шаблон решения]]).

### 4–5. Brute → Optimize
Brute: очистить строку, развернуть, сравнить — O(n) времени, но O(n) лишней памяти. Optimize: два указателя — O(1) памяти, ничего не копируем.

### 6. Code
```kotlin
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val text = reader.readLine() ?: ""

    var left = 0
    var right = text.length - 1
    var isPalindrome = true

    while (left < right) {
        when {
            text[left] == ' '  -> left++           // пропускаем пробел слева
            text[right] == ' ' -> right--          // пропускаем пробел справа
            text[left].lowercaseChar() != text[right].lowercaseChar() -> {
                isPalindrome = false               // символы не совпали → не палиндром
                break
            }
            else -> { left++; right-- }            // совпали → сходимся к центру
        }
    }

    println(if (isPalindrome) "It is a palindrome" else "It is not a palindrome")
}
```

### 7. Sanity / Сложность
Время **O(n)**, память **O(1)**. Инвариант: всё, что вне `[left, right]`, уже проверено и симметрично.

---

# 4. Кодирование длин серий / RLE (rle-test) — easy ✅

**Условие.** RLE сжимает повторы как «символ + число». Буква может сопровождаться **многозначным** числом; если числа нет — повтор = 1. Вывести **длину распакованной строки**.
Примеры: `A15BA5`→21 (15+1+5); `ABCDR`→5; `Z123XY`→125 (123+1+1).

### Сигнал → Ключ → Метод
- Маячок: «разобрать строку посимвольно», «распаковать», «посчитать длину». Чёткого паттерна из компаса нет — это **линейный проход + парсер чисел** (один проход O(n)). Честно скажи на собесе: «здесь не нужен спец-алгоритм, нужен аккуратный посимвольный разбор с look-ahead».
- **Вслух:** «Иду по строке: встретил букву → смотрю вперёд, собираю подряд идущие цифры в число. Нет цифр → +1. Сумму держу в Long».

### 1. Understand
Формат серии: `<буква>[<число>]`. Число многозначное. Нужна суммарная длина. ⚠️ Большие счётчики → `Long`.

### 2. Examples
`A15BA5`: `A15`→15, `B`→1, `A5`→5 = 21. Крайний: `ABCDR` (нет цифр) → 5.

### 3. Pattern
Парсинг с заглядыванием вперёд (look-ahead) на цифры.

### 6. Code
```kotlin
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val compressed = reader.readLine().trim()

    var totalLength = 0L
    var index = 0

    while (index < compressed.length) {
        // на этой позиции — буква (серия в RLE начинается с символа)
        index++

        var repeatCount = 0L
        var hasDigits = false
        while (index < compressed.length && compressed[index].isDigit()) {
            hasDigits = true
            repeatCount = repeatCount * 10 + (compressed[index] - '0')  // набираем многозначное число
            index++
        }

        totalLength += if (hasDigits) repeatCount else 1L   // нет цифр → серия из одного символа
    }

    println(totalLength)
}
```

### 7. Sanity / Сложность
Время **O(n)**, память **O(1)**. Аккуратно с границей `index < length` во внутреннем `while`.

---

# 5. Самая сложная буква (hardest-letter) — medium ⚠️ нужно точное условие

> Платформа не отдала полный текст без авторизации (метки: **implementation, medium**, лимит 5 c).
> **Действие:** вставь сюда точный текст условия с примерами — добью точное решение. Ниже две частые трактовки названия, каждая со своим сигналом.

### Гипотеза A — «буква с максимальной частотой/весом»
- **Сигнал → Ключ:** «самая ... буква» = частота → **тип 1 (счётчик)**.
- **Вслух:** «Считаю частоты букв в `LongArray(128)`, беру максимум. O(n)».
```kotlin
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val text = reader.readLine() ?: ""

    val letterFrequency = LongArray(128)                 // код символа -> частота
    for (symbol in text) {
        if (!symbol.isWhitespace()) letterFrequency[symbol.code]++
    }

    var hardestLetter = ' '
    var maxFrequency = -1L
    for (code in letterFrequency.indices) {
        if (letterFrequency[code] > maxFrequency) {
            maxFrequency = letterFrequency[code]
            hardestLetter = code.toChar()
        }
    }
    println(hardestLetter)
}
```

### Гипотеза B — «лишняя добавленная буква» (как LeetCode *Find the Difference*)
- **Сигнал → Ключ:** «найти непарный/добавленный элемент» = **тип 1, приём XOR** (всё парное взаимоуничтожается).
- **Вслух:** «XOR всех кодов обеих строк — парные сократятся, останется код добавленной буквы. O(n), память O(1)».
```kotlin
fun findExtraLetter(original: String, modified: String): Char {
    var xorAccumulator = 0
    for (symbol in original) xorAccumulator = xorAccumulator xor symbol.code
    for (symbol in modified) xorAccumulator = xorAccumulator xor symbol.code
    return xorAccumulator.toChar()   // останется код непарной (добавленной) буквы
}
```

---

# 6. Перелёт (flight) — complex ⚠️ нужно точное условие

> Метки: **implementation, complex**, лимит 1 c. Полный текст без авторизации недоступен.
> **Действие:** вставь точное условие с примерами — соберу решение под него. Ниже — два самых вероятных сценария с привязкой к сигналам компаса.

### Сценарий 1 — расписание/стыковки рейсов → Сортировка + жадность (тип 8)
- **Сигнал → Ключ:** «интервалы», «расписание», «успеть на пересадку», «время вылета/прилёта» → **тип 8**.
- **Вслух:** «Сортирую рейсы по времени прилёта/вылета и жадно беру стыкуемые. Сортировка O(n log n)».
```kotlin
data class Flight(val departure: Int, val arrival: Int)   // время в минутах от начала суток

// Максимум стыкуемых рейсов (следующий вылетает не раньше прилёта предыдущего)
fun maxChainedFlights(flights: List<Flight>): Int {
    val sortedByArrival = flights.sortedBy { flight -> flight.arrival }  // жадность по концу
    var lastArrival = Int.MIN_VALUE
    var chosenCount = 0
    for (flight in sortedByArrival) {
        if (flight.departure >= lastArrival) {   // можем успеть на этот рейс
            chosenCount++
            lastArrival = flight.arrival
        }
    }
    return chosenCount
}
```

### Сценарий 2 — дешевле/быстрее добраться между городами → Дейкстра (тип 12)
- **Сигнал → Ключ:** «маршрут», «минимальная цена/время», «пересадки», взвешенный граф → **тип 12**.
- **Вслух:** «Города — вершины, рейсы — взвешенные рёбра. Нужна минимальная стоимость маршрута → Дейкстра с очередью с приоритетом. O(E log V)».

> Это та самая Дейкстра, но с **читаемыми именами** вместо `dist/pq/u/v/w/d`:

```kotlin
import java.util.PriorityQueue

data class Route(val toCity: Int, val price: Long)        // ребро: куда лететь и за сколько
data class CityCost(val city: Int, val costSoFar: Long)   // элемент очереди: город + накопленная цена

fun cheapestCostToEachCity(
    cityCount: Int,
    startCity: Int,
    routesFromCity: Array<MutableList<Route>>             // routesFromCity[город] = список рейсов из него
): LongArray {
    // minCostToCity[город] — минимальная известная цена добраться до этого города
    val minCostToCity = LongArray(cityCount) { Long.MAX_VALUE }
    minCostToCity[startCity] = 0L

    // очередь с приоритетом: всегда достаём город с наименьшей накопленной ценой
    val queue = PriorityQueue<CityCost>(compareBy { cityCost -> cityCost.costSoFar })
    queue.add(CityCost(city = startCity, costSoFar = 0L))

    while (queue.isNotEmpty()) {
        val (currentCity, costSoFar) = queue.poll()

        // этот вариант устарел — до города уже нашли дешевле, пропускаем
        if (costSoFar > minCostToCity[currentCity]) continue

        for (route in routesFromCity[currentCity]) {
            val newCost = costSoFar + route.price
            if (newCost < minCostToCity[route.toCity]) {   // нашли путь дешевле
                minCostToCity[route.toCity] = newCost
                queue.add(CityCost(city = route.toCity, costSoFar = newCost))
            }
        }
    }
    return minCostToCity
}
```
**Чем читаемые имена лучше прежних `u/v/w/d`:**
- `currentCity` (был `u`) — город, который сейчас обрабатываем;
- `route.toCity` (был `v`) — соседний город, куда ведёт рейс;
- `route.price` (был `w`) — вес ребра = цена рейса;
- `costSoFar` (был `d`) — цена, накопленная до текущего города;
- `minCostToCity` (был `dist`) — лучшая цена до каждого города;
- `queue` (был `pq`) — очередь с приоритетом.

---

# Чек-лист перед алгосекцией (из [[00 Компас алгоритмов]])

1. **Understand:** повтори условие, уточни вход/выход и ограничения (размер n → подсказывает сложность).
2. **Examples:** прогони 1–2 примера руками + крайние случаи (пусто, 1 элемент, дубликаты, переполнение).
3. **Pattern:** назови сигнал вслух → выбери метод по таблице.
4. **Brute force** + его Big O (страховка).
5. **Optimize:** какой ключ убирает лишнюю работу.
6. **Code:** маленькими шагами, понятные имена переменных, проговаривай инвариант.
7. **Sanity:** прогон на примере, проверка индексов (off-by-one), финальная сложность.

**Сигналы этих 6 задач одной строкой:**
```
частый первый символ      → Hash Map (частоты)        [simple-suggest]
сумма различных           → Hash Set (уникальность)   [summ-of-the-various]
палиндром                 → Два указателя             [palindroming-check]
распаковать RLE           → линейный проход + парсер  [rle-test]
самая частая/лишняя буква → Hash Map / XOR            [hardest-letter*]
маршрут дешевле/стыковки  → Дейкстра / сортировка+жадность [flight*]
```
`*` — уточнить точное условие.

## TODO
- [ ] Вставить точное условие **«Самая сложная буква»** → финальное решение.
- [ ] Вставить точное условие **«Перелёт»** → финальное решение.
