# Как выполнить код в UI-потоке

## 1. Handler (старый способ)

`Handler` передаёт задачу в главный поток через его `Looper`, который обрабатывает очередь сообщений:

```kotlin
val mainHandler = Handler(Looper.getMainLooper())
mainHandler.post {
    // выполнится в главном потоке
    myMethod()
}
```

- `Looper.getMainLooper()` — `Looper` главного потока.
- `post {}` — отправляет задачу на выполнение в главный поток. Есть `postDelayed { }` для отложенного запуска.

## 2. runOnUiThread() (внутри Activity)

```kotlin
runOnUiThread {
    myMethod() // выполнится в главном потоке
}
```

Удобный метод `Activity`: если уже на UI-потоке — выполнит сразу, иначе запостит в очередь.

## 3. View.post()

Если есть ссылка на `View`:

```kotlin
myView.post {
    myMethod() // выполнится в главном потоке
}
```

Бонус: `View.post {}` гарантирует, что код выполнится **после** того, как View пройдёт layout (полезно, когда нужны его размеры).

## 4. Корутины (современный способ)

```kotlin
// переключиться на главный поток из любой корутины
withContext(Dispatchers.Main) {
    textView.text = data
}

// или запустить корутину сразу на главном потоке
lifecycleScope.launch(Dispatchers.Main) {
    myMethod()
}
```

> [!tip] Что использовать
> - В современном коде — **корутины** (`Dispatchers.Main` / `viewModelScope` / `lifecycleScope`) или `Flow.collect` на главном потоке.
> - `Handler(Looper.getMainLooper())` ещё встречается в legacy и под капотом; помни про утечки при отложенных задачах (см. [[Handler и Looper]]).
> - **Зачем вообще:** UI в Android можно трогать **только из главного потока**. Обращение к View из фонового потока → `CalledFromWrongThreadException`.

Связано: [[Handler и Looper]], [[Coroutines]], [[Choreographer, SurfaceFlinger]]
