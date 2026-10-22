# Отличия MVI от MVVM и MVC

Все три — паттерны представления (presentation), различаются связью между UI и логикой и направлением потока данных.

## MVC (Model-View-Controller)

Самый старый паттерн. Три компонента:

1. **Model** — данные и бизнес-логика.
2. **View** — отображение (в классическом MVC — пассивное).
3. **Controller** — принимает ввод пользователя, обновляет Model, выбирает View.

**Проблема в Android:** роль Controller фактически берут на себя `Activity`/`Fragment`, которые одновременно являются и View. Из-за этого Controller и View сливаются, классы разрастаются («God Activity»), логика смешивается с UI и плохо тестируется.

```text
Пользователь → Controller → Model → View → Пользователь
```

## MVVM (Model-View-ViewModel)

Разделяет логику на три части и убирает прямую связь View ↔ Model.

1. **Model** — данные и бизнес-логика (репозитории, сеть, БД).
2. **View** — UI (Activity, Fragment, Compose). Наблюдает за состоянием и обновляет интерфейс.
3. **ViewModel** — связующее звено: хранит состояние и UI-логику, отдаёт данные через `LiveData`/`StateFlow`. **Не знает о View** (нет ссылки на Activity).

**Поток данных:**

1. Пользователь взаимодействует с View.
2. View вызывает метод ViewModel.
3. ViewModel обращается к Model (репозиторию).
4. Model возвращает данные.
5. ViewModel обновляет состояние → View наблюдает и перерисовывается.

### Пример MVVM

**Model:**

```kotlin
data class Item(val id: Int, val name: String)

class Repository {
    fun getItems(): List<Item> = listOf(Item(1, "Item 1"), Item(2, "Item 2"))
}
```

**ViewModel:**

```kotlin
class MainViewModel(private val repository: Repository) : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    fun loadItems() {
        _items.value = repository.getItems()
    }
}
```

**View (Activity):**

```kotlin
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer { MainViewModel(Repository()) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    textView.text = items.joinToString(", ") { it.name }
                }
            }
        }

        button.setOnClickListener { viewModel.loadItems() }
    }
}
```

> [!warning] Частые ошибки, которые ловят на собесе
> - `by viewModels { ViewModelProvider.Factory { ... } }` — **невалидно**: `ViewModelProvider.Factory` это интерфейс, а не builder-лямбда. Используйте `viewModelFactory { initializer { ... } }`.
> - `lifecycleScope.launchWhenStarted { ... }` — **устарело**: корутина не отменяется, а лишь приостанавливается. Правильно — `repeatOnLifecycle(Lifecycle.State.STARTED)`.

## MVI (Model-View-Intent)

Развитие MVVM с **однонаправленным потоком данных** (unidirectional data flow) и **единым неизменяемым состоянием**.

1. **Model** — состояние экрана (`State`) + `Reducer` (чистая функция, которая по старому State и событию строит новый State).
2. **View** — рендерит State и шлёт **Intent**'ы (намерения пользователя).
3. **Intent** — действия пользователя (нажатие кнопки), которые ViewModel превращает в новое состояние.

Ключевая идея: **состояние одно (single source of truth), неизменяемое, меняется только через reducer**. Поток строго в одну сторону: `Intent → ViewModel (reducer) → State → View`.

### Пример MVI

**State:**

```kotlin
data class ScreenState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val error: String? = null,
)
```

**Intent:**

```kotlin
sealed interface ScreenIntent {
    data object LoadData : ScreenIntent
    data class Refresh(val query: String) : ScreenIntent
}
```

**ViewModel:**

```kotlin
class ScreenViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScreenState())
    val state: StateFlow<ScreenState> = _state.asStateFlow()

    fun processIntent(intent: ScreenIntent) {
        when (intent) {
            is ScreenIntent.LoadData -> loadData()
            is ScreenIntent.Refresh -> refreshData(intent.query)
        }
    }

    private fun loadData() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            delay(1000)
            _state.update {
                it.copy(isLoading = false, data = listOf(Item(1, "Sample 1")))
            }
        }
    }

    private fun refreshData(query: String) { /* ... */ }
}
```

**View подписывается на State и шлёт Intent:**

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { state ->
            if (state.isLoading) showLoading() else showData(state.data)
        }
    }
}

button.setOnClickListener {
    viewModel.processIntent(ScreenIntent.LoadData)
}
```

## Сравнение

| Критерий               | MVC                          | MVVM                                  | MVI                                          |
| ---------------------- | ---------------------------- | ------------------------------------- | -------------------------------------------- |
| Поток данных           | Двунаправленный              | Двунаправленный (View ↔ ViewModel)    | **Однонаправленный** (Intent → State → View) |
| Состояние              | Размазано по Controller/View | Несколько `LiveData`/`StateFlow`      | **Единый неизменяемый `State`**              |
| Связь UI и логики      | Controller ≈ View (слиты)    | ViewModel не знает о View             | ViewModel не знает о View                    |
| Предсказуемость        | Низкая                       | Средняя                               | Высокая (изменения через reducer)            |
| Бойлерплейт            | Мало                         | Средне                                | Больше (State/Intent/Reducer)                |
| Тестируемость          | Плохая                       | Хорошая                               | Отличная (reducer — чистая функция)          |

> [!tip] Как отвечать
> - **MVVM** — стандарт Android: ViewModel + наблюдаемое состояние. Минус — состояние может быть «размазано» по нескольким `LiveData`/`StateFlow`, легко получить несогласованность.
> - **MVI** — когда нужно **строгое единое состояние** и предсказуемость (сложные экраны, много событий). Минус — больше бойлерплейта.
> - **MVC** на собесе обычно упоминают как «откуда всё началось» и почему в Android он выродился в God-Activity.

Связано: [[Чистая архитектура]], [[Паттерны]], [[SharedFlow vs Flow vs StateFlow]]
