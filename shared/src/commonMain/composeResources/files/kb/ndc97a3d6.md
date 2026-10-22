**Что такое Bundle в Android?**

Bundle — это контейнер для хранения пар ключ-значение, который используется в Android для передачи данных между компонентами приложения (такими как Activity, Fragment, Service) или для сохранения состояния приложения.

Bundle поддерживает сериализацию данных, что делает его удобным для передачи информации между компонентами в рамках одного процесса.
**Основные характеристики Bundle:**

1. **Ключ-значение:**

• Bundle работает на основе пар ключ-значение, где ==ключ — это строка (всегда) (String)==, а значение может быть одним из поддерживаемых типов данных.

2. **Сериализация данных:**

• Значения внутри Bundle сериализуются, чтобы их можно было передать между компонентами.

3. **Частое использование:**

• Для передачи данных через Intent.
• Для сохранения состояния в методе onSaveInstanceState.

**Особенности и ограничения**

1. **Память:**

• Bundle не предназначен для хранения больших объёмов данных, так как может привести к ошибке TransactionTooLargeException при передаче через Intent или сохранении состояния.
(Такое может возникнуть при восстановлении всех фрагментов одной транзакцией)

2. **Не храните большие объекты:**

• Не рекомендуется хранить внутри Bundle сложные структуры данных или большие массивы, так как это может замедлить передачу данных.

**Основные сценарии использования**

**1. Передача данных между Activity через Intent**
При запуске новой Activity с помощью Intent можно передать данные через Bundle.
**Пример:**
```kotlin
// В MainActivity
val intent = Intent(this, SecondActivity::class.java)
intent.putExtra("username", "JohnDoe")
intent.putExtra("age", 25)
startActivity(intent)

// Во SecondActivity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_second)

    val username = intent.getStringExtra("username")
    val age = intent.getIntExtra("age", 0)

    println("Username: $username, Age: $age")
}
```

**2. Передача данных между Fragment**
Bundle используется для передачи аргументов между Fragment через метод setArguments.

**Пример:**
```kotlin
// Создание Fragment и передача данных
val fragment = MyFragment()
val bundle = Bundle()
bundle.putString("username", "JohnDoe")
bundle.putInt("age", 25)
fragment.arguments = bundle

// Получение данных в Fragment
class MyFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = arguments?.getString("username")
        val age = arguments?.getInt("age")
        println("Username: $username, Age: $age")
    }
}
```

**3. Сохранение состояния Activity или Fragment**
Метод onSaveInstanceState используется для сохранения временных данных при изменении конфигурации (например, повороте экрана).
**Пример:**
```kotlin

override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString("username", "JohnDoe")
    outState.putInt("age", 25)
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val username = savedInstanceState?.getString("username")
    val age = savedInstanceState?.getInt("age")
    println("Restored Username: $username, Age: $age")
}
```

**Сравнение Bundle с другими способами передачи данных**

| Метод передачи        | Особенности                                  | Когда использовать                                   |
| --------------------- | -------------------------------------------- | ---------------------------------------------------- |
| Bundle                | Прост для передачи между компонентами.       | Передача внутри одного процесса                      |
| **SharedPreferences** | Подходит для хранения данных между сессиями. | Для простого сохранения данных, таких как настройки. |
| **Room/SQLite**       | Сложнее, но лучше для больших данных.        | Большие обьемы данных                                |
| **File/Cache**        | Используется для больших данных или файлов.  | Когда данные должны быть долговечными.               |

### **Где хранится Bundle в Android:**

1. **В памяти устройства (RAM)**
• Когда вы создаёте объект Bundle, он существует в оперативной памяти (RAM) устройства. Это временные данные, которые не сохраняются на диск и исчезают при завершении работы приложения или его процесса.
• Например, при передаче данных между Activity и Fragment с помощью Intent или setArguments() используется объект Bundle, который хранится в памяти.

2. **В процессе передачи данных между компонентами приложения**
• Когда вы передаете Bundle через Intent или Fragment, это может быть просто структура данных в памяти, которая сохраняется на время работы приложения.
• Для передачи через Intent (например, для запуска новой Activity), Bundle может быть сериализован в формат, подходящий для хранения в Intent, и передан как часть объекта Intent. Этот процесс требует сериализации данных, чтобы они могли быть переданы между процессами, если приложение взаимодействует с другими приложениями или компонентами системы.

3. **В** onSaveInstanceState **и** onRestoreInstanceState **(сохранение состояния** Activity**)*
• Когда Activity изменяет свое состояние (например, при изменении конфигурации, как поворот экрана), данные из Bundle могут быть сохранены в ==**временной области памяти устройства**==. В onSaveInstanceState вы сохраняете состояние, а в onRestoreInstanceState восстанавливаете его. Эти данные также не сохраняются на диск, а лишь ==сохраняются в памяти в рамках текущего жизненного цикла компонента.==

4. **Внутри** SavedInstanceState **(для сохранения состояния при изменении конфигурации)**
• Когда приложение проходит через изменения конфигурации (например, поворот экрана), Android автоматически сохраняет состояние приложения в объект Bundle, который передается в метод onCreate() или onRestoreInstanceState(). Этот Bundle может содержать информацию о текущем состоянии UI или других переменных, чтобы восстановить их после изменения конфигурации.

5. **Сериализация** Bundle **для передачи между процессами**
• В случае использования механизма межпроцессного взаимодействия (например, с помощью Intent между различными приложениями), данные из Bundle могут быть сериализованы и переданы через механизмы IPC (межпроцессного взаимодействия), такие как Binder. Это также не подразумевает долговременное хранение на диске, а скорее временную передачу данных.

### **Что такое SharedPreferences в Android?**

SharedPreferences — это механизм для хранения небольших наборов данных в виде пар ключ-значение в постоянной памяти устройства. Эти данные сохраняются в файлах XML и остаются доступными даже после закрытия приложения или перезагрузки устройства.
Этот способ хранения данных особенно подходит для сохранения таких настроек и данных, как:
• Предпочтения пользователя (например, настройки темы).
• Простые состояния приложения.
• Локальные флаги (например, первый запуск приложения).
**Особенности SharedPreferences**
1. **Ключ-значение:**
• Данные хранятся в формате пар ключ-значение.
• Ключи — строки (String).
• Значения могут быть простыми типами данных (int, float, boolean, String,~`` Set<String>``~).
2. **Сохранение в XML:**
• Данные сохраняются в виде XML-файлов в внутренней памяти приложения.
3. **Запись:**
• `apply()` — **асинхронный**: меняет значения в памяти сразу, на диск пишет в фоне, ничего не возвращает (не блокирует поток). **Рекомендуется.**
• `commit()` — **синхронный**: блокирует поток до записи на диск, возвращает `Boolean` (успех). Не вызывать с главного потока.
> ⚠️ Частая ошибка на собесе — перепутать их местами. Запомни: **apply = async, commit = sync (с результатом)**.
4. **Простота использования:**
• Удобный интерфейс для чтения, записи и удаления данных.
5. Минусы использования
• **Чтение и запись данных:** При работе с **SharedPreferences** вся структура XML-файла парсится в оперативной памяти. Чем больше данных, тем больше времени требуется на парсинг и тем выше нагрузка на память.
• **Блокировка потока:** Операции записи в **SharedPreferences** блокируются (синхронизированы), чтобы избежать конфликтов при доступе к данным из нескольких потоков. Это может замедлить работу приложения, если файл слишком большой.

Данные в **SharedPreferences** хранятся в текстовом формате **XML**:

• XML содержит дополнительные теги, которые увеличивают объём хранимых данных.
• Для чтения/записи данных XML-файл полностью загружается в оперативную память, что ограничивает его размер.
• При увеличении объема данных производительность операций чтения и записи существенно снижается.

**Режимы доступа**
При вызове метода getSharedPreferences можно указать режим доступа:
• MODE_PRIVATE **(по умолчанию):**
• Файл доступен только для вашего приложения.
• MODE_WORLD_READABLE **(устаревший):**
• Файл доступен для других приложений (не рекомендуется).
• MODE_WORLD_WRITEABLE **(устаревший):**
• Файл доступен для чтения и записи другими приложениями.

**Когда использовать SharedPreferences**
1. Для небольших данных, таких как:
• Логические флаги (например, первый запуск).
• Пользовательские настройки (тема, язык).
• Простые состояния (например, сохранение последней позиции).
2. **Когда НЕ использовать:**
• Для хранения больших данных.
• Для хранения конфиденциальной информации (используйте EncryptedSharedPreferences).
• Для структурированных данных (лучше использовать базы данных, такие как Room).

**Расширенные возможности**
**1. EncryptedSharedPreferences**

Для безопасного хранения данных можно использовать EncryptedSharedPreferences. Он автоматически шифрует данные.

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedSharedPreferences = EncryptedSharedPreferences.create(
    context,
    "SecurePreferences",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

val editor = encryptedSharedPreferences.edit()
editor.putString("secureKey", "secureValue").apply()
```

| Плюсы                                 | Минусы                                       |
| ------------------------------------- | -------------------------------------------- |
| Простота в использовании              | Ограничено небольшими объёмами данных        |
| Подходит для настроек и мелких данных | Не оптимально для сложных структур           |
| Данные сохраняются между сессиями     | Не рекомендуется для конфиденциальных данных |
|                                       |                                              |
Перечислите все возможные способы сохранения состояния при повороте устройства ??

**✅ 1. onSaveInstanceState() + onRestoreInstanceState() (Bundle)**

📌 **Для небольших данных (числа, строки, Parcelable, Serializable).**


**🔹 Как работает?**
• Перед уничтожением Activity система вызывает onSaveInstanceState().
• Данные сохраняются в Bundle и передаются в onCreate() или onRestoreInstanceState().
**🔹 Пример:**

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString("username", "Alice")
    outState.putInt("score", 100)
}

override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    val username = savedInstanceState.getString("username")
    val score = savedInstanceState.getInt("score")
}
```

**✅ 2. ViewModel**

В Android при повороте экрана Activity уничтожается (onDestroy()) и создается заново (onCreate()). Однако, ViewModel сохраняет данные **и не пересоздается** благодаря механизму ViewModelStore.

📌 **Как это работает?**
• ViewModel привязан к ViewModelStoreOwner (например, Activity или Fragment).
• При пересоздании Activity (из-за поворота экрана) **старый** ViewModel **сохраняется в** ViewModelStore.
• Новый экземпляр Activity получает тот же ViewModel из ViewModelStore, **поэтому данные не теряются**.

**🔹 Пример кода с ViewModel**

```kotlin
class MyViewModel : ViewModel() {
    var score = MutableLiveData<Int>(0) // Живые данные
}

// Использование ViewModel в Activity
class MainActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels() // ViewModel сохраняется при повороте

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.score.observe(this) { score ->
            textView.text = "Score: $score"
        }
    }
}
```

**2️⃣ Как работает ViewModelStore под капотом?**

ViewModelStore — это **контейнер** для хранения ViewModel, связанный с ViewModelStoreOwner (Activity или Fragment).

**🔹 Как хранятся ViewModel?**

1. Когда Activity создается, она **создает (или получает)** ViewModelStore.
2. Если Activity уничтожается из-за поворота экрана, ViewModelStore **не удаляется**.
3. Новый экземпляр Activity получает **тот же** ViewModelStore, а значит **и тот же** ViewModel.
4. Если Activity уничтожается навсегда (finish() или завершение процесса), ViewModelStore **очищается**.

 **Что внутри ViewModelStore?**

==ViewModelStore по сути — это HashMap<String, ViewModel>, где ключ — это уникальное имя ViewModel (обычно его класс).==

📌 **Пример упрощенного кода ViewModelStore:**

```kotlin

class ViewModelStore {
    private val map = mutableMapOf<String, ViewModel>()

    fun put(key: String, viewModel: ViewModel) {
        map[key] = viewModel
    }

    fun get(key: String): ViewModel? {
        return map[key]
    }

    fun clear() {
        map.values.forEach { it.onCleared() }
        map.clear()
    }
}
```

🔹 Когда Activity пересоздается, новый экземпляр получает **уже сохраненный** ViewModel.

**4️⃣ Как ViewModelStore работает в Activity и Fragment?**

**✅ В Activity**
• Каждая Activity имеет свой ViewModelStore (создается в ComponentActivity).
• При повороте экрана ViewModelStore сохраняет ViewModel.

📌 **Пример упрощенного механизма в** Activity**:**

```kotlin
class MyActivity : ComponentActivity() {
    private val viewModelStore = ViewModelStore() // Хранилище ViewModel

    fun getViewModel(): MyViewModel {
        return viewModelStore.get("MyViewModel") ?: MyViewModel().also {
            viewModelStore.put("MyViewModel", it)
        }
    }
}
```

**✅ В Fragment**

• Fragment использует ViewModelStoreOwner, но может хранить ViewModel **либо на уровне** Activity**, либо** Fragment.
• Если вызвать activityViewModels(), то ViewModel **будет один на все фрагменты** Activity.

📌 **Пример ViewModel в Fragment:**

```kotlin
class MyFragment : Fragment() {
    private val viewModel: MyViewModel by activityViewModels() // ViewModel на уровне Activity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.score.observe(viewLifecycleOwner) { score ->
            textView.text = "Score: $score"
        }
    }
}
// ✅ **Результат:** ViewModel **переживет не только поворот экрана, но и смену фрагментов.**
```

**5️⃣ Когда ViewModel уничтожается?**

ViewModel уничтожается, когда **уничтожается его** ViewModelStoreOwner:

| ViewModel хранится в... | Когда уничтожается?                |
| ----------------------- | ---------------------------------- |
| **Activity**            | Когда Activity вызовет finish()    |
| **Fragment**            | Когда Fragment удалится (remove()) |
| **Navigation Graph**    | Когда удалится NavGraph            |
| **ViewModelStoreOwner** | Когда clear()                      |
🔹 При уничтожении ViewModel вызывается метод onCleared(), где можно очистить ресурсы:

```kotlin

class MyViewModel : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        // Очистка ресурсов (например, закрытие потоков)
    }
}
```

**6️⃣ Как вручную управлять ViewModelStore?**

Можно передавать свой ViewModelStore в ViewModelProvider, например, чтобы сохранить ViewModel дольше, чем Activity.  

📌 **Пример использования кастомного** ViewModelStore**:**

```kotlin
val store = ViewModelStore()
val provider = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())
val viewModel = provider.get(MyViewModel::class.java)
```

✅ **Так можно передавать** ViewModelStore **между разными** Activity **или** Fragment**.**

**✅ 4. android:configChanges (Запрет пересоздания Activity)**

📌 **Предотвращает перезапуск** Activity **при изменении конфигурации.**

**🔹 Как работает?**

• Указываем в AndroidManifest.xml, что Activity **не будет пересоздаваться**:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|screenSize" />
```

> [!warning] `configChanges` — обычно НЕ нужно
> Этот способ **отключает** пересоздание Activity, и вы сами обрабатываете смену конфигурации в `onConfigurationChanged`. Считается **антипаттерном** для обычных экранов: ломает подмену ресурсов (layout-land, размеры) и заставляет вручную обновлять UI. Правильный путь — пусть Activity пересоздаётся, а состояние живёт в `ViewModel`/`SavedStateHandle`. `configChanges` оправдан для спецслучаев (видеоплеер, кастомный рендер, WebView).

**✅ 6. SharedPreferences (Для долгосрочного хранения)**

📌 **Для сохранения настроек и небольших данных, переживающих перезапуск приложения.**

**🔹 Как работает?**

• Данные сохраняются в SharedPreferences, а затем загружаются в onCreate().
• Переживают **и поворот экрана, и перезапуск приложения**.

**🔹 Пример:**

```kotlin
// Сохранение данных
val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
prefs.edit().putString("username", "Alice").apply()

// Получение данных
val username = prefs.getString("username", "Default")
```

**✅ 7. База данных (Room)**

📌 **Для хранения больших данных, которые должны пережить закрытие приложения.**

**🔹 Как работает?**

• Данные сохраняются в локальной базе данных (Room) и загружаются при старте Activity.  

**🔹 Пример:**

```kotlin
@Entity
data class User(@PrimaryKey val id: Int, val name: String)

@Dao
interface UserDao {
    @Query("SELECT * FROM User WHERE id = :id")
    fun getUser(id: Int): LiveData<User>
}

val userDao = database.userDao()
userDao.getUser(1).observe(this) { user ->
    textView.text = user.name
}
```

> [!tip] Какой способ для какого состояния (главное на собесе)
> Три разных «уровня» сохранения — их часто путают:
> 1. **`onSaveInstanceState`/`Bundle`** — переживает поворот **и** убийство процесса в фоне, но НЕ закрытие пользователем. Только маленькие данные (Parcelable). Транзиентное UI-состояние.
> 2. **`ViewModel`** — переживает **поворот** (тот же `ViewModelStore`), но **НЕ** убийство процесса. Для состояния экрана/загруженных данных.
> 3. **`SavedStateHandle`** (в ViewModel) — лучшее из обоих: переживает и поворот, и смерть процесса (под капотом сохраняется в Bundle). См. [[ViewModel, SaveStateHandle, SavedStateRegistryOwner]].
> 4. **DataStore/Room** — постоянное хранение (переживает перезапуск приложения).
>
> Эталонный ответ: «эфемерное UI → SavedInstanceState/rememberSaveable; состояние экрана → ViewModel; пережить смерть процесса → SavedStateHandle; навсегда → Room/DataStore».

Связано: [[ViewModel, SaveStateHandle, SavedStateRegistryOwner]], [[EditText]], [[Передача данных между фрагментами]], [[Parcelable vs Serializable]], [[Room or SharedPreferences]]