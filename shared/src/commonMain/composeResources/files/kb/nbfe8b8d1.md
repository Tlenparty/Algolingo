

Введение. 
	Сompose - это плагин для *компилятора* , а значит он может к функциями добавлять ``$composer.startRestartGroup()``  вначале и в конце группы  ``$composer.endRestartGroup()`` а так же добавлять ``$changed:Int``
	У Compose есть 3 фазы для отрисовки UI: Composition (какой UI отобразить), -> Layout (как расположить) -> Drawing (как отрисовать (Canvas))
	Эти три фазы выполняются для каждого кадра, но Compose может пропускать некоторые фазы, если данные для них не поменялись, проходя по дереву и помечая функции @StabilityInferred

Стабильные типы:
	 1) Все примитивы: Int, Long, Double и String
	 2) Классы и sealed классы  у которых все поля val
	 3) Все enum (даже с var)
	 4) Типы, помеченные @Immutable и @Stable.
	 5)  Pair, Result, ImmutableCollections

Нестабильные типы:
	1) Классы у которых хотя бы одно поле var
	2) Внешние библиотеки, List, Set, Map и прочие коллекции. LocalDate(Time), Flow
	3) Стабильные классы с нестабильным джереником. 

Обратить стоит внимание на интерфейсы, проверка идет по типу-насследнику, объект которого передан в аргументы)

@Immutable & @Stable

Аннотацию @Immutable мы можем применить, чтобы пометить класс или интерфейс как стабильны. Все наследники такого интерфейса будут тоже стабильными

```kotlin
@Immutable interface Parent // стабильный тип
class Child1(val age: Int) : Parent // стабильный тип
class Child2(var list: List<String>) : Parent // тоже стабильный (наследует от @Immutable)
```

Эта аннотация своего рода обещание компилятору, что мы даем ему стабильный класс
Аннотация @Stable полезна для классов и потомков, которые сами оповещают Compose об изменении. Например, `@Stable` подойдёт, если в классе есть поле типа `State<T>` или `MutableState<T>` (`mutableStateOf()` создаёт такой объект).
```kotlin
@Stable data class MyUiState2(val timer: MutableState<Int>)
```

Такие аннотации дают обещание Compose, что мы выполняем следующий контракт
1. `equals` всегда будет возвращать одинаковое значение для одной и той же пары объектов.
2. Когда публичные поля типа изменяются, нужно оповестить об этом Compose.
3. Все публичные поля стабильны.

Пример как сделать List стабильным без ImmutableList из библиотеки 
```kotlin
@JvmInline
@Immutable
value class ImmutableList<T>(val data: List<T>):List<T> by data
```

Лямбды
Лямбды сами по себе в Compose стабильно, но есть несколько правил, если лямбда захватывает переменные. Такие лямбды оборачиваться в remember в Compose не будут.

Лямбды в Compose оборачиваются в remember, где ключом будет захваченная переменная 
```kotlin
// До компиляции
val number: Int = 6
val lambda = { Log.d(TAG, "number = $number") }
// После компиляции
val number: Int = 6
val lambda = remember(number) { { Log.d(TAG, "number = $number") } }
```

Для незменяемых или редко изменяемых параметров используем rememberUpdatedState
Например
```kotlin
@Composable
fun Chapter(model:Model, onSelectedItem:(String) -> Unit){
	val title = rememberUpdatedState { model.title }
	
	Column{
		Title(item.title.text)
		Card(
			// При изменении model 
			onSelectedItem = { onSelectedItem.invoke(model.title) }
			// При изменении title 
			onSelectedItem = { onSelectedItem.invoke(title) }
		)
	}
}
```

**@NonRestartableComposable**
Так помечены `Spacer` и многие inline-функции. Аннотация отключает генерацию restart-кода — Compose не делает такую функцию самостоятельно рекомпозируемой (она рекомпозируется только вместе с родителем).

Как отслеживать рекомпозиции:
1) LayoutInspector 
2) Plugin Compose Stability Analyzer
3) Debugger

Plugin Compose Stability Analyzer - Работает с Kotlin 2.2.1, повышении версии Kotlin у нас в проекте приведет к повышению версии в либе Vico до 2.4.0-alpha.3 и к непониманию как добавить зависимость 
```kotlin
// add("debugImplementation", compose.uiTooling)
```

После подключения плагина будут подсвечиваться:
	1) Зеленым стабильные функции
	2) Желтым функции стабильность которых вычисляется в runtime
	3) Красным нестабильные функции, которые будут вызывать рекомпозицию каждый раз
Плагин позволяет навесить аннотацию @TraceRecomposition с помощью которой будут в логи выводиться все рекомпозиции данной функции с ее аргументами. Аргументы которые аффектят функцию будут помечены как changed

Некоторые best practise
1) Из больших стейтов выносить часто меняющиеся параметры. Например: 
```kotlin
@Immutable
data class UiState(
	val id:String,
	val header:String,
	val status:Status // Меняется часто. Выносить часто меняющиеся 
)
```

remember: Когда использовать
- Для обёртки лямбд с нестабильными внешними переменными.
- Для классов без переопределённого `equals`.
- Для объектов, которые нужно сохранить между рекомпозициями (например, `State<T>).
-  Не оборачиваться простые вычисления в remember, создание remember обойдется дороже, чем каждую рекомпозицю делать вычисление.



> [!important] Strong Skipping Mode — меняет правила игры (Compose 1.7 / Kotlin 2.0)
> Начиная с Compose Compiler в составе Kotlin 2.0.20+, **strong skipping mode включён по умолчанию**. Это важное обновление к написанному выше:
> • Composable-функции с **нестабильными** параметрами теперь тоже становятся **skippable** — Compose сравнивает такие параметры по **ссылке** (instance equality), а стабильные — по `equals`.
> • **Лямбды с захватом** автоматически оборачиваются в `remember` компилятором — ручной `remember` для них чаще уже не нужен.
> Практический вывод: гоняться за стабильностью каждого класса стало менее критично, но `@Immutable`/`@Stable` всё ещё полезны — они дают сравнение по значению (`equals`) вместо сравнения по ссылке, что пропускает рекомпозицию чаще.

> [!warning] `rememberUpdatedState` принимает значение, а не лямбду
> В примере выше `rememberUpdatedState { model.title }` — некорректное использование. Сигнатура: `rememberUpdatedState(newValue: T): State<T>`. Передаётся **значение**:
> ```kotlin
> val currentTitle by rememberUpdatedState(model.title)
> ```
> Назначение — иное, чем у `remember`: он нужен, когда долгоживущий эффект (`LaunchedEffect`, колбэк) должен видеть **актуальное** значение, **не перезапускаясь**. Для дорогих вычислений между рекомпозициями — `remember(key)`.

> [!note] Уточнения к спискам стабильности
> • `Pair`/`Triple`/`Result` стабильны **только если стабильны их типы-аргументы** (стабильные «при условии» дженерики).
> • `enum` стабильны всегда (даже с `var`-полями).
> • `kotlinx.collections.immutable` (`persistentListOf`/`ImmutableList`) — канонический способ дать стабильную коллекцию без самодельной обёртки.

Связано: [[Compose. Стабильность]], [[Compose. Recomposition]], [[Compose. Remember]], [[Compose. Оптимизация]], [[Data class]], [[Inline class, value class, typealias, infix]]
