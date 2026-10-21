Необходимо определить userIds участников, которые прошли наибольшее количество шагов steps за все дни, не пропустив ни одного дня соревнований

Пример
```java
statistics = [
	[{userId:1,steps:1000},{userId:2,steps:1500}],
	[{userId:2,steps:1000}]
]
```

Вывод
```kotlin
champioms = {userIds:[2],steps:2500}}
```


```kotlin
data class StepEntry(val userId: Int, val steps: Int)

data class ChampionResult(val userIds: List<Int>, val steps: Int)

fun findChampions(statistics: List<List<StepEntry>>): ChampionResult {
    val totalDays = statistics.size
    val userStepsMap = mutableMapOf<Int, Int>()
    val userParticipationCount = mutableMapOf<Int, Int>()
    
    // Подсчёт шагов и количества дней участия каждого пользователя
    for (dayStats in statistics) {
        for (entry in dayStats) {
            userStepsMap[entry.userId] = userStepsMap.getOrDefault(entry.userId, 0) + entry.steps
            userParticipationCount[entry.userId] = userParticipationCount.getOrDefault(entry.userId, 0) + 1
        }
    }
    
    // Оставляем только тех, кто участвовал во все дни
    val validUsers = userParticipationCount.filterValues { it == totalDays }.keys
    
    // Находим максимальное количество шагов среди участников
    val maxSteps = validUsers.maxOfOrNull { userStepsMap[it] ?: 0 } ?: 0
    
    // Определяем победителей
    val champions = validUsers.filter { userStepsMap[it] == maxSteps }
    
    return ChampionResult(champions, maxSteps)
}

fun main() {
    val statistics = listOf(
        listOf(StepEntry(1, 1000), StepEntry(2, 1500)),
        listOf(StepEntry(2, 1000))
    )
    
    val result = findChampions(statistics)
    println(result) // ChampionResult(userIds=[2], steps=2500)
}

```

