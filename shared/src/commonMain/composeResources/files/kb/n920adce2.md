```kotlin
  
open class Outer {  
    private val a = 1  
    protected open val b = 2  
    internal open val c = 3  
    val d = 4  
  
    class Nested {  
        public val e: Int = 5  
    }  
}  
  
fun main(args: Array<String>) {  
    class Subclass : Outer() {  
        val nested = Nested().e  
        // a -  
        // b +      
        // c +     
        // d +      
        // Nested +   
         // e +    
    }  
  
    class Unrelated(o:Outer){  
        val nested = Outer.Nested()  
        val anwser = Outer.Nested::e  
        // o.a -  
        // o.b - 
	    // o.c +     
        // o.d +    
         // Outer.Nested и Nested::e (-)  
    }  
}
```


### Есть ли тут проблемы ? 

```kotlin
Во первых тут всегда надо else,
Если убрать присваивание, то else не нужен.

private val testString = "fourth"  
  
fun main(args: Array<String>) {  
    val x = when(testString){  
        "first" -> println("ok_first")  
        "second" -> println("ok_second")  
        else -> { println("no")}  
    }  
    println(x)  
}
```

Что будет выведено, что тут лишнее. Как оптимизировать? 
```kotlin
Убрать private 

data class Person(private val firstName:String, private val secondName:String)  
  
fun Person.fullName() = "${this.firstName} ${this.secondName}"

fun main(args: Array<String>) {  
    println(Person("John","Done").fullName())  
}
```

Что не так с кодом??

```kotlin
class Student(var name: String) {  
    init {  
        println("$name")  
    }  
	// Вторичный конструктор не может содержать var 
    constructor(sectionName: String, var id: Int) : this(sectionName)  
  
}  
  
fun main(args: Array<String>) {  
  
}
```


### Задача на scope функции

```kotlin
data class Student(val firstName: String, val secondName: String)  
  
fun main(args: Array<String>) {  
    val student = Student("John", "Doe")  
    val newSecondName = "Petrov"  
    val applyStudent = student.apply { Student(firstName, newSecondName) }  
    val letStudent =  student.let { Student(it.firstName, newSecondName)  }  
    // В applyStudent не перезапишет, а в letStudent перезапишет (возвращает последнее) если сделать also то будет true!!
    println(applyStudent == letStudent)
}
```


```kotlin
import kotlinx.coroutines.*

fun main() {
    runBlocking {
        println("Weather forecast")
        println(getWeatherReport())
        println("Have a good day!")
    }
}

suspend fun getWeatherReport(): String = coroutineScope {
    val forecast = async { getForecast() }
    val temperature = async { getTemperature() }
	// Из-за того что delay 200, а не 500 мы не упадем с ошибкой!!
    delay(200)
    temperature.cancel()
    //"${forecast.await()} ${temperature.await()}" // Тут kotlinx.coroutines.JobCancellationException 
    //"${forecast.await()} 
}

suspend fun getForecast(): String {
    delay(1000)
    return "Sunny"
}
//Метод выбрасывает AssertionError, но он никогда не будет вызван, так как отменяется корутина.
suspend fun getTemperature(): String {
    delay(500)
    throw AssertionError("Temperature is invalid")
    return "30 graduses"
}
Вывод!

Weather forecast
Sunny
Have a good day!
```

> [!abstract] Модификаторы доступа (задача 1)
> • **`private a`** — виден только внутри `Outer`, недоступен и в наследнике, и снаружи.
> • **`protected b`** — виден в `Outer` и его наследниках (`Subclass`), но **не** в несвязанном классе.
> • **`internal c`** — виден везде в пределах **того же модуля** (и в наследнике, и в `Unrelated`).
> • **`d` (public)** — виден везде.
> • **`Nested`** — вложенный (не `inner`) класс, доступен как `Outer.Nested`; его `public val e` читается. Ссылка `Outer.Nested::e` валидна.
> Ключевое: в Kotlin `protected` = «класс + наследники» (в отличие от Java — **без** package-видимости), а `private` члена класса виден только внутри самого класса, **не файла**.

> [!warning] `when` как выражение требует `else` (задача 2)
> Раз результат присваивается (`val x = when(...)`), `when` — **выражение** и обязан быть исчерпывающим → нужен `else`. Если убрать присваивание и использовать `when` как **statement**, `else` не обязателен. Здесь каждая ветка — `println(...)`, возвращающий `Unit`, поэтому `x: Unit`, и `println(x)` выведет **`kotlin.Unit`**.

> [!warning] `private` в конструкторе ломает extension (задача 3)
> `Person.fullName()` — top-level extension. Свойства, помеченные `private val` в первичном конструкторе, видны **только внутри класса**, а extension к классу не относится → **не скомпилируется**. Фикс — убрать `private` (или сделать extension членом класса). Сам `data class` с приватными полями допустим, но снаружи их не прочитать.

> [!warning] Вторичный конструктор не объявляет свойства (задача 4)
> `constructor(sectionName: String, var id: Int)` — **ошибка компиляции**: параметры вторичного конструктора **не могут** иметь `val`/`var` (они не объявляют свойства, в отличие от первичного). Нужно либо убрать `var`, либо завести свойство в теле класса и присвоить его.

> [!tip] Scope-функции: что возвращается (задача 5)
> • **`apply`/`also`** возвращают **сам receiver** → `Student(firstName, newSecondName)` внутри `apply` создаётся и **выбрасывается**, `applyStudent` == исходный `student` (John, Doe).
> • **`let`/`run`** возвращают **результат лямбды** → `letStudent` = новый `Student(John, "Petrov")`.
> Поэтому `applyStudent == letStudent` → **`false`**. С `also` вместо `let` обе ветки вернули бы receiver → `true`.

> [!tip] Structured concurrency: ранняя отмена спасает (задача 6)
> `getTemperature()` бросает `AssertionError` на `delay(500)`, но его **отменяют на `delay(200)`** (`temperature.cancel()`) — до того, как исполнится `throw`. Поэтому исключения нет, и выводится только `forecast` → `Sunny`. Если бы `delay` был ≥ 500 и мы вызвали `temperature.await()`, исключение в дочернем `async` **отменило бы весь `coroutineScope`**. Кстати, `throw` стоит до `return "30 graduses"` → этот `return` недостижим.

Связано: [[Модификаторы доступа и их видимость]], [[Sealed class vs enum]], [[Вторичный конструктор]], [[let, apply,  with, run, also]], [[Coroutines]], [[Structured Concurrency]], [[Nested class vs inner class]]
