Slot Table **— это внутренняя структура данных в Jetpack Compose, которая хранит и отслеживает состояние композиции.**

Это основной механизм, который **позволяет эффективно выполнять рекомпозицию (recomposition)**, избегая полного пересоздания UI.

🛠 **Проще говоря,** Slot Table **— это “память” Compose, которая хранит:**

• Какие @Composable функции были вызваны
• Какие параметры использовались
• Какие состояния были созданы (remember, rememberSaveable)
• Как UI связан с деревом композиции

📌 Благодаря этому Compose **не перерисовывает весь UI, а обновляет только изменённые части**.

**🔧 Как работает Slot Table под капотом?**

Slot Table можно представить как **список (таблицу) слотов**, где каждый слот — это **элемент UI и его состояние**.

**🔹 1. Slot Table создаётся при первой композиции**

Когда Jetpack Compose **впервые рендерит UI**, он:

• Записывает в Slot Table порядок вызовов @Composable функций
• Запоминает **значения состояний** (remember, mutableStateOf)
• Записывает ссылки на параметры и дочерние элементы

Пример:

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}
```


При первой композиции Compose:

1. Запишет в Slot Table, что Counter() содержит Button и Text
2. Запомнит, что count = 0 (состояние)
3. Запишет, что onClick обновляет count


**🔹 2. При изменении состояния Slot Table обновляет только нужные части**

Когда пользователь нажимает на кнопку (onClick увеличивает count):

• mutableStateOf(0) меняется на mutableStateOf(1)
• Jetpack Compose **ищет в** Slot Table**, где используется** count
• Находит, что он влияет только на Text("Clicked $count times")
• **Перерисовывает только** Text**, а** Button **остаётся неизменным**


> 📌 **Это ключевое преимущество** Slot Table **— частичная рекомпозиция без полного пересоздания UI!**

**🔥 Разбор Slot Table на примере**


Допустим, у нас есть такой код:
```kotlin
@Composable
fun MyScreen() {
    Column {
        Text("Hello")
        Text("World")
    }
}
```

**🔹 Как Slot Table выглядит после первой композиции?**

| Слот | Элемент | Значение |
| ---- | ------- | -------- |
| 1    | Column  | -        |
| 2    | Text    | "Hello"  |
| 3    | Text    | "World"  |
**🔹 Что происходит, если меняем текст второго Text?**

| Слот | Элемент | Значение  |
| ---- | ------- | --------- |
| 1    | Column  | -         |
| 2    | Text    | "Hello"   |
| 3    | Text    | "Compose" |


### **🎯 Как Slot Table работает с remember?**

remember записывает своё значение в Slot Table, чтобы сохранять данные при рекомпозиции.

**🔹 Пример:**
```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }  // Сохраняется в `Slot Table`
    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}
```

1. remember { mutableStateOf(0) } создаёт слот в Slot Table
2. При клике count++ **обновляет только** Text
3. Button остаётся неизменным

📌 **Без** remember count **сбрасывался бы при каждой рекомпозиции!**

### **⚠️ Когда Slot Table пересоздаётся полностью?**

**❌ Slot Table обновляется полностью, если:**

• **Изменяется структура UI**
• **Изменяются ключевые параметры у** remember
• **Используется список без** key

> [!tip] Почему важен порядок вызовов и `key`
> Slot Table — **позиционная** структура: Compose идентифицирует состояние по **позиции вызова** в дереве, а не по содержимому. Поэтому:
> - Нельзя вызывать composable условно так, чтобы «съезжал» порядок — состояние перепутается. Для этого есть `key(id) { ... }`.
> - В `LazyColumn` указывайте `items(list, key = { it.id })` — иначе при перестановке/удалении элементов Compose неверно сопоставит слоты, и состояние (например, развёрнутость item) «прилипнет» не к тому элементу.
>
> Это та же идея, что у `DiffUtil` в RecyclerView, но на уровне runtime.

Связано: [[Compose. Slot api]], [[Compose. Recomposition]], [[Compose. Remember]], [[RecyclerView, Diff Utils, List Adapter]]