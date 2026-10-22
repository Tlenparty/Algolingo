# UDF (Unidirectional Data Flow)

**Однонаправленный поток данных** — архитектурный принцип, на котором стоят MVI и рекомендованный Google подход к Compose. Senior должен уметь объяснить, *почему* UDF, а не просто «у меня MVVM».

## Суть

```text
       (State) ↓                    ↑ (Event/Intent)
   ┌─────────────┐   рендерит   ┌──────────┐
   │     UI      │ ◄──────────  │  State   │
   │ (Compose)   │              │  holder  │ (ViewModel)
   └─────────────┘  события →   └──────────┘
```

> [!important] Два правила UDF
> 1. **State течёт вниз** — от источника истины (ViewModel) к UI. UI его только **отображает**, не меняет напрямую.
> 2. **События текут вверх** — UI шлёт события (клики, ввод) наверх; их обрабатывает state holder и порождает **новое состояние**.
> UI — **функция от состояния**: `UI = f(state)`. При том же состоянии экран всегда выглядит одинаково.

## Почему это хорошо

> [!tip] Что отвечать про выгоды
> • **Single source of truth** — состояние в одном месте, нет рассинхрона между UI-полями.
> • **Предсказуемость** — состояние меняется только через явные события, легко проследить причину.
> • **Тестируемость** — логика state holder тестируется без UI (подать события → проверить состояния).
> • **Естественно ложится на Compose** — рекомпозиция как раз и есть «перерисовать UI при новом state».
> • **Concurrency-safe** — состояние иммутабельно, обновляется атомарно (новый объект), нет гонок за общими мутабельными полями.

## Реализация в Compose

```kotlin
data class FeedState(val items: List<Item> = emptyList(), val loading: Boolean = false)

class FeedViewModel : ViewModel() {
    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()   // наружу — иммутабельный

    fun onEvent(e: FeedEvent) { /* обработать → _state.update { … } */ }
}

@Composable
fun FeedScreen(vm: FeedViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()      // state вниз
    Feed(state, onRefresh = { vm.onEvent(FeedEvent.Refresh) })// события вверх
}
```

> [!warning] Антипаттерны, нарушающие UDF
> • **Мутабельный state наружу** — `MutableStateFlow`/`MutableLiveData` в публичном API: UI пишет в него напрямую → теряется single source of truth (см. разбор в [[CodeReview]]).
> • **Состояние, размазанное по View** — часть в UI, часть в VM → рассинхрон.
> • **Логика в UI** — решения принимает composable, а не state holder.

> [!note] UDF vs конкретные паттерны
> UDF — это **принцип**, а не паттерн. **MVI** реализует его наиболее строго (единый `State` + `Intent`). **MVVM** может быть UDF (один `StateFlow<UiState>`) или не быть (куча отдельных мутабельных полей). Сравнение — [[Отличия MVI от MVVM,  MVC]] и [[MVI библиотеки]].

Связано: [[Отличия MVI от MVVM,  MVC]], [[MVI библиотеки]], [[Чистая архитектура]], [[SharedFlow vs Flow vs StateFlow]], [[CodeReview]], [[Compose. Recomposition]]
