# TreeSet

Отсортированное множество на основе красно-чёрного дерева. Реализует `NavigableSet`, хранит элементы в упорядоченном виде. Все операции — `O(log n)`.

> **На собес одной фразой:** `TreeSet` = уникальные элементы **в отсортированном порядке**; операции `O(log n)`; умеет диапазоны (`subSet`) и поиск ближайших (`floor`/`ceiling`/`higher`/`lower`).

*Примеры ниже — на Kotlin (в проекте под Android).*

## Когда использовать

### 1. Отсортированное хранение
Элементы автоматически поддерживаются в порядке сортировки при вставке.
```kotlin
val sortedSet = sortedSetOf<Int>()  // TreeSet под капотом
sortedSet.add(3)
sortedSet.add(1)
sortedSet.add(2)
println(sortedSet)   // [1, 2, 3]
```

### 2. Поиск диапазонов (подмножества)
```kotlin
val set = sortedSetOf(1, 2, 3, 4, 5)
val subset = set.subSet(2, 4)   // [fromInclusive, toExclusive)
println(subset)                 // [2, 3]
```

### 3. Минимум и максимум
`first()` и `last()` быстро возвращают наименьший и наибольший элементы.
```kotlin
val set = sortedSetOf(10, 20, 30)
println(set.first())  // 10
println(set.last())   // 30
```

### 4. Итерация в отсортированном порядке
```kotlin
val set = sortedSetOf("banana", "apple", "cherry")
for (fruit in set) println(fruit)   // apple, banana, cherry
```

### 5. Поиск ближайших значений
`higher()` / `lower()` (строго больше/меньше) и `ceiling()` / `floor()` (больше-или-равно / меньше-или-равно).
```kotlin
val set = sortedSetOf(10, 20, 30, 40)
println(set.higher(25))  // 30 — строго больше 25
println(set.lower(25))   // 20 — строго меньше 25
println(set.ceiling(20)) // 20 — >= 20
println(set.floor(20))   // 20 — <= 20
```

## Когда НЕ стоит использовать

1. **Порядок не важен** → `HashSet` быстрее (`O(1)` против `O(log n)`).
2. **Частые изменения** → у `TreeSet` вставка/удаление `O(log n)`, у `HashSet` — `O(1)` в среднем.
3. **Нужен доступ по индексу** → `TreeSet` его не поддерживает; берите `ArrayList`.

> Порядок задаётся естественной сортировкой (`Comparable`) или переданным `Comparator`:
> `val set = sortedSetOf(compareByDescending { it }, 1, 2, 3)  // [3, 2, 1]`
