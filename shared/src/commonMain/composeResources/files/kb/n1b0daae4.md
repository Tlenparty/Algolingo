В Kotlin для работы с финансовыми значениями, такими как деньги, рекомендуется использовать тип данных `BigDecimal`, особенно когда точность вычислений имеет значение. `BigDecimal` предоставляет высокую точность при работе с числами, что важно для финансовых операций, где даже малейшие ошибки могут быть критическими.

```kotlin
import java.math.BigDecimal

fun main() { val amount1 = BigDecimal("10.25") 
val amount2 = BigDecimal("20.50") 
val sum = amount1 + amount2 
val difference = amount2 - amount1 
val product = amount1 * amount2 
val division = amount2 / amount1 
println("Sum: $sum") 
println("Difference: $difference") 
println("Product: $product") 
println("Division: $division") }
```