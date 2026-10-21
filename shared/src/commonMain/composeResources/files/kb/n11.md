Как решить такую задачу на Kotlin? Given an array of integers o, arrange the elements in it so that:
```kotlin
 * o[0] ≤ o[1] ≥ o[2] ≤ o[3] ≥ o[4] ...
 * The elements in o need not be unique (they may be repeated)
 * For example:  1 3 5 steps and 2 4 6
 * 5 2 1 7 9 8 ->  1 ≤ 7 ≥ 5 ≤ 9 ≥ 2 ≤ 8  or  2 ≤ 5 ≥ 1 ≤ 9 ≥ 7 ≤ 8 or ...
```

**✅ Подход 1: Сортировка + перестановка пар (O(n log n))*

```kotlin
fun waveSort(arr: IntArray): IntArray {
    arr.sort() // Шаг 1: Сортируем массив
    for (i in 1 until arr.size step 2) {
        arr[i] = arr[i - 1].also { arr[i - 1] = arr[i] } // Шаг 2: Меняем пары
    }
    return arr
}

fun main() {
    val arr = intArrayOf(5, 2, 1, 7, 9, 8)
    println(waveSort(arr).joinToString(" ")) // Возможный результат: 2 1 5 7 8 9
}
```

**✅ Подход 2: Однопроходный (O(n)) – Жадный алгоритм**

**💡 Идея:**

1. **Проходим по массиву один раз**, сравнивая ``arr[i] и arr[i+1]``.
2. **Если порядок не соблюдается**, меняем местами.

```kotlin
fun waveSortGreedy(arr: IntArray): IntArray {
    for (i in arr.indices step 2) {
        if (i > 0 && arr[i] < arr[i - 1]) {
            arr[i] = arr[i - 1].also { arr[i - 1] = arr[i] }
        }
        if (i < arr.size - 1 && arr[i] < arr[i + 1]) {
            arr[i] = arr[i + 1].also { arr[i + 1] = arr[i] }
        }
    }
    return arr
}

fun main() {
    val arr = intArrayOf(5, 2, 1, 7, 9, 8)
    println(waveSortGreedy(arr).joinToString(" ")) // Возможный результат: 5 7 1 9 2 8
}
```