CompositionLocal — это механизм для **передачи данных вниз по дереву композиции**, минуя параметры функций.

Он позволяет **глобально управлять состоянием** и **избегать пропа drilling’а (передачи параметров через все функции)**.

🛠 **Используется для передачи:**

• **Текущей темы** (colors, typography, shapes)
• **Локализации** (Locale)
• **Настроек приложения**
• **Контекста (например, Window Size, User Settings)**
• **Доступа к навигации (NavController)**

**🎯 Как работает CompositionLocal под капотом?**

**🔹 1. Создание CompositionLocal**

Все CompositionLocal создаются с помощью staticCompositionLocalOf или compositionLocalOf:

```kotlin
val LocalUser = compositionLocalOf<String> { error("No user found!") }
```

✅ Это создаёт CompositionLocal для хранения строки (например, имени пользователя).
✅ Значение по умолчанию **вызывается только если значение не установлено**.

**🔹 2. Передача значения через CompositionLocalProvider**

Чтобы изменить значение CompositionLocal, используется CompositionLocalProvider:
```kotlin
@Composable
fun MyApp() {
    CompositionLocalProvider(LocalUser provides "Alice") {  // ✅ Устанавливаем новое значение
        UserProfile() // Вложенные Composables смогут использовать LocalUser
    }
}

```

✅ CompositionLocalProvider создаёт новый **скоуп (область)**, в которой LocalUser будет "Alice".
✅ **Все дочерние компоненты** внутри UserProfile() смогут получить "Alice".

**🔹 3. Чтение значения через CompositionLocal.current**

В дочерних @Composable можно получить переданное значение:
```kotlin
@Composable
fun UserProfile() {
    val username = LocalUser.current  // ✅ Получаем "Alice"
    Text("Hello, $username!") // Выведет "Hello, Alice!"
}
```

✅ **Вложенные компоненты получают значение** LocalUser **из ближайшего** CompositionLocalProvider**.**
✅ Если CompositionLocalProvider не установлен, **используется значение по умолчанию**.

### **🔧 Как CompositionLocal работает под капотом?**

 **🏗 Структура работы CompositionLocal**

Внутри CompositionLocal работает с SlotTable — это структура данных Compose, где хранятся все UI-элементы.

1. **Создание** CompositionLocal

• При создании compositionLocalOf { ... } создаётся объект ``CompositionLocal<T>``, который умеет искать ближайшее значение в дереве.

2. **Установка значения через** CompositionLocalProvider

• CompositionLocalProvider создаёт **новый слой в Composition** и записывает значение LocalUser в **SlotTable**.

• Все дочерние Composables теперь **видят это значение**.

3. **Чтение** LocalUser.current

• Когда вызывается LocalUser.current, Compose **ищет ближайший** CompositionLocalProvider вверх по дереву.
• Если он найден, используется **его значение**.
• Если нет, используется значение **по умолчанию**.

### **🔥 Разница между staticCompositionLocalOf и compositionLocalOf***


| Тип                      | Когда использовать                                              | Перерисовка изменений                 |
| ------------------------ | --------------------------------------------------------------- | ------------------------------------- |
| staticCompositionLocalOf | Для неизменяемых данных (например, Context, Resources, Density) | ❌ **Нет** (не вызывает recomposition) |
| compositionLocalOf       | Для изменяемых данных (Theme, User Settings)                    | ✅ **Да** (вызывает recomposition)     |

**✅ Пример: Используем staticCompositionLocalOf для темы**

```kotlin
val LocalTheme = staticCompositionLocalOf { lightColors() }

@Composable
fun MyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTheme provides darkColors()) {
        content()
    }
}

@Composable
fun ThemedScreen() {
    val colors = LocalTheme.current
    Box(modifier = Modifier.background(colors.primary)) { /* UI */ }
}
```

✅ Используем staticCompositionLocalOf, потому что **тема не меняется динамически**.
✅ Это **не вызывает рекомпозицию**, а просто передаёт значение.

**✅ Пример: Используем compositionLocalOf для локализации**
```kotlin
val LocalLocale = compositionLocalOf { Locale("en") }

@Composable
fun LanguageProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLocale provides Locale("fr")) {
        content()
    }
}

@Composable
fun Greeting() {
    val locale = LocalLocale.current
    Text(if (locale.language == "fr") "Bonjour!" else "Hello!")
}
```

✅ Используем compositionLocalOf, потому что локаль **может изменяться**.
✅ При изменении локали **вся композиция перерисуется**

### **🎯 Когда использовать CompositionLocal?**

✅ **Когда данные нужны в нескольких Composables, но передавать их вручную неудобно**
✅ **Когда данные редко изменяются (**staticCompositionLocalOf**)**
✅ **Когда данные могут часто меняться (**compositionLocalOf**)**

  
❌ **Не используйте** CompositionLocal **для всего подряд!**

Если данные изменяются **очень часто**, лучше использовать ViewModel или State.

### **📌 Вывод**

• CompositionLocal — механизм глобального хранения данных в дереве Compose.
• CompositionLocalProvider создаёт область, в которой CompositionLocal имеет определённое значение.
• current читает значение из ближайшего CompositionLocalProvider.
• staticCompositionLocalOf → **для неизменяемых данных (не вызывает recomposition).**
• compositionLocalOf → **для изменяемых данных (вызывает recomposition).**

> [!important] В чём реальная разница static vs обычный
> - `compositionLocalOf` — Compose **отслеживает чтения** значения: при смене значения рекомпозируются **только** те composable, что его читают. Гибко, но есть накладные расходы на отслеживание.
> - `staticCompositionLocalOf` — чтения **не отслеживаются**: при смене значения рекомпозируется **всё** поддерево провайдера. Зато само чтение дешевле. Поэтому его берут для значений, которые почти не меняются (`LocalContext`, `LocalDensity`).
>
> Правило: меняется часто → `compositionLocalOf`; задаётся один раз → `staticCompositionLocalOf`.

> [!warning] Не злоупотреблять
> `CompositionLocal` — это неявная зависимость (как [[DI VS SERVICE LOCATOR|Service Locator]] для UI): её не видно в сигнатуре функции, что усложняет понимание и тесты. Для обычных данных предпочитайте **явные параметры**. `CompositionLocal` оправдан для «сквозных» вещей: тема, локаль, плотность экрана.

Связано: [[Compose. Remember]], [[Compose. Slot table]], [[DI VS SERVICE LOCATOR]]
