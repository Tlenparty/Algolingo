**Slot API** в **Jetpack Compose** – это механизм для передачи **компонентов (Composable)** внутрь других компонентов.

**🔹 Как работает Slot API?**

**1. Простая реализация Slot API**

Допустим, у нас есть **кастомный контейнер** MyCard, в который можно передавать **заголовок и контент**.
```kotlin
@Composable
fun MyCard(
    title: @Composable () -> Unit,  // Слот для заголовка
    content: @Composable () -> Unit // Слот для контента
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        title()   // Вставляем переданный заголовок
        Spacer(modifier = Modifier.height(8.dp))
        content() // Вставляем переданный контент
    }
}
```

Теперь мы можем использовать MyCard, передавая в него **разные заголовки и контент**:
```kotlin
@Composable
fun ExampleScreen() {
    MyCard(
        title = { Text("Заголовок", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        content = { Text("Это контент карточки", fontSize = 16.sp) }
    )
}
```

**📌 Что здесь происходит?**

• title и content – это **слоты**, в которые можно передавать любые Composable.
• В title() передаётся Text("Заголовок").
• В content() передаётся Text("Это контент карточки").

🔹 **Таким образом,** MyCard **можно переиспользовать с разными данными!**

**🔹 Slot API в Scaffold**

Scaffold – это компонент, который использует **Slot API** для кастомизации **TopBar, BottomBar, FAB и контента**
```kotlin
 @Composable
fun MyScaffoldExample() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Мой AppBar") }) },
        floatingActionButton = { FloatingActionButton(onClick = {}) { Text("+") } }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Text("Контент экрана")
        }
    }
}
```

**🔹 Когда использовать Slot API?**

✅ Когда хотите сделать **гибкий** Composable, куда можно передавать разные компоненты.
✅ Когда **разные части UI** должны изменяться независимо (например, AppBar, контент и FAB).
✅ Когда нужно **избежать дублирования кода**.

> [!note] Slot API ≠ Slot Table
> Не путать: **Slot API** — это паттерн дизайна публичного API (передаём `@Composable`-лямбды внутрь компонента, как в `Scaffold`). **Slot Table** ([[Compose. Slot table]]) — внутренняя структура данных runtime, хранящая состояние композиции. Связаны только названием.

> [!tip]
> Slot API — это, по сути, [[Паттерны|паттерн «Стратегия»/«Композиция»]] в декларативном UI: вместо параметров-данных передаём параметры-поведение (UI). Примеры из стандартной библиотеки: `Scaffold`, `TopAppBar`, `Surface`, `Box`, кнопки (`content: @Composable RowScope.() -> Unit`).

Связано: [[Compose. Slot table]], [[Compose]], [[Compose. SubcomposeLayout]]