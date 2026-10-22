### **Оптимизированный поиск через Flow (для больших данных)**


Если данных **очень много (~100 000+ записей)**, то:

1. Фильтрацию лучше **делать в фоне (Dispatchers.IO)**.
2. Использовать **Flow + debounce()**, чтобы не фильтровать **на каждую букву**.


📌 **Как это сделать?**
```kotlin
class SearchViewModel : ViewModel() {
    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults: StateFlow<List<Product>> = _searchResults

    private var allProducts: List<Product> = listOf()

    fun setProducts(products: List<Product>) {
        allProducts = products
    }

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(300) // ⏳ debounce – ждем паузу перед фильтрацией
            val result = allProducts.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
            _searchResults.value = result // Обновляем UI
        }
    }
}
```

📌 **Связываем с UI (в Activity)**
```kotlin
val viewModel = ViewModelProvider(this)[SearchViewModel::class.java]
viewModel.setProducts(products)

val adapter = ProductAdapter(emptyList())
recyclerView.adapter = adapter

viewModel.searchResults.observe(this) { filteredList ->
    adapter.updateList(filteredList)
}

searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
    override fun onQueryTextSubmit(query: String?): Boolean = false
    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.search(newText ?: "")
        return true
    }
})
```


### **🔹 1. Базовая реализация TextField с поиском**

📌 **Как сделать SearchView.setOnQueryTextListener в Compose?**

• Используем var searchQuery by remember { mutableStateOf("") }.
• Отправляем текст в onValueChange.

```kotlin
@Composable
fun SearchBar(onQueryChanged: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    TextField(
        value = searchQuery,
        onValueChange = {
            searchQuery = it
            onQueryChanged(it) // Отдаем изменения
        },
        label = { Text("Поиск") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
    )
}
```

✅ **Как это работает?**

✔️ onQueryChanged(it) вызывает **колбэк при каждом вводе** символа.
✔️ Можно передавать данные в ViewModel или LazyColumn.


### **🔹 2. Добавляем LazyColumn с фильтрацией**


📌 **Полный пример поиска с отображением списка товаров**
```kotlin
@Composable
fun ProductSearchScreen() {
    var searchQuery by remember { mutableStateOf("") }

    val products = listOf("Телефон", "Ноутбук", "Чайник", "Клавиатура", "Монитор", "Молоко")
    val filteredProducts = products.filter { it.contains(searchQuery, ignoreCase = true) }

    Column {
        SearchBar { searchQuery = it } // Передаем изменения в поиск

        LazyColumn {
            items(filteredProducts) { product ->
                Text(product, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```

### **🔹 3. Оптимизация через debounce() (Flow)**


Если в списке **100 000+ записей**, лучше **не фильтровать сразу**, а использовать **debounce()**.

📌 **ViewModel с Flow (оптимизация для больших списков)**

```kotlin
class SearchViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<String>> = _searchQuery
        .debounce(300) // Ждем 300 мс перед фильтрацией
        .mapLatest { query ->
            products.filter { it.contains(query, ignoreCase = true) } // Фильтруем
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    companion object {
        val products = listOf("Телефон", "Ноутбук", "Чайник", "Клавиатура", "Монитор", "Молоко")
    }
}
```

📌 **Использование в Composable**

```kotlin
@Composable
fun ProductSearchScreen(viewModel: SearchViewModel = viewModel()) {
    val searchResults by viewModel.searchResults.collectAsState()

    Column {
        SearchBar { viewModel.updateQuery(it) } // Передаем запрос в ViewModel

        LazyColumn {
            items(searchResults) { product ->
                Text(product, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
```