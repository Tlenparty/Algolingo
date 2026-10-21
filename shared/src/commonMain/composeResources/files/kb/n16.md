Одной из классических задач, которые решаются методом двух указателей, является **поиск суммы двух чисел в отсортированном массиве**, дающей заданное значение.
```kotlin

fun twoSumSorted(nums: IntArray, target: Int): Pair<Int, Int>? {
    var left = 0
    var right = nums.size - 1

    while (left < right) {
        val sum = nums[left] + nums[right]

        when {
            sum == target -> return Pair(left, right)  // Нашли нужную пару
            sum < target -> left++  // Увеличиваем левый указатель (нам нужно больше)
            else -> right--  // Уменьшаем правый указатель (нам нужно меньше)
        }
    }

    return null  // Если пары нет
}

fun main() {
    val nums = intArrayOf(1, 2, 3, 4, 6, 8, 9)  // Отсортированный массив
    val target = 10
    val result = twoSumSorted(nums, target)

    println(result)  // Ожидаемый результат: (1, 5), потому что nums[1] + nums[5] = 2 + 8 = 10
}
```