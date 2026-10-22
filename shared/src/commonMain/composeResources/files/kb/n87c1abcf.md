**📌 1. Пример: Реакция на частые нажатия кнопки**

**Задача:**

• Если пользователь нажал кнопку **50 раз за 5 секунд**, то отправляем **только 1 запрос** после паузы в 500 мс.

**Решение: Flow + debounce (Kotlin, Coroutines)**ё
```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val clickFlow = MutableSharedFlow<Unit>() // Поток для нажатий

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val button = Button(this)
        button.text = "Отправить"

        setContentView(button)

        // Отправляем событие в поток при каждом клике
        button.setOnClickListener {
            clickFlow.tryEmit(Unit)
        }

        // Запускаем сбор событий с debounce
        CoroutineScope(Dispatchers.Main).launch {
            clickFlow
                .debounce(500) // Игнорируем частые нажатия, ждем 500 мс тишины
                .collect {
                    sendRequest() // Отправляем запрос только после паузы
                }
        }
    }

    private fun sendRequest() {
        println("🚀 Отправляем 1 запрос на сервер")
    }
}
```

✅ **Как это работает:**

• ``MutableSharedFlow<Unit>()`` собирает клики.
• .debounce(500) ждет 500 мс **тишины** перед выполнением sendRequest().
• Если клики продолжаются, таймер сбрасывается, и запрос не отправляется.


**📌 2. Пример: Буферизация и отправка пачки данных**

Если вместо одного события нужно отправлять **все данные**, можно **буферизовать** их и отправлять сразу пачкой.


**Задача:**

• Пользователь делает **50 быстрых изменений** (например, вводит текст).
• Нужно отправить **все изменения одним запросом**.

**Решение: Buffer + Collect (Kotlin Flow)**
```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BufferedSender {

    private val updateFlow = MutableSharedFlow<String>(extraBufferCapacity = 50)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            updateFlow
                .buffer() // Буферизуем события
                .debounce(500) // Ждем 500 мс перед отправкой
                .collect { batch ->
                    sendBatchRequest()
                }
        }
    }

    fun update(value: String) {
        updateFlow.tryEmit(value) // Отправляем данные в поток
    }

    private fun sendBatchRequest() {
        println("📤 Отправляем батч обновлений на сервер")
    }
}

// Использование:
val sender = BufferedSender()
sender.update("Изменение 1")
sender.update("Изменение 2") // Эти изменения попадут в 1 запрос
```

✅ **Как это работает:**

• buffer() позволяет хранить **до 50 изменений** перед обработкой.
• .debounce(500) отправляет **все изменения одним батчем**, если пользователь не делает новых за 500 мс.

**📌 3. Пример: Ограничение частоты запросов (Throttling или debounce)**

Если **надо разрешить только 1 запрос в секунду**, а остальные игнорировать:
```kotlin
button.clicks()
    .throttleFirst(1000) // Разрешаем только 1 запрос в 1000 мс
    .onEach { sendRequest() }
    .launchIn(scope)
```

✅ **Когда использовать Throttling:**

• При загрузке **изображений** (не отправлять каждое изменение, а ограничить 1 раз в X секунд).
• При **автоподгрузке списка** (чтобы не запрашивать сервер слишком часто).

**📌 Итог**

| **Метод**                | **Когда использовать?**                                                                    | **Как работает?**                          |
| ------------------------ | ------------------------------------------------------------------------------------------ | ------------------------------------------ |
| **Debounce**             | Когда пользователь делает **быстрые клики**, но нужно **ждать паузу** перед отправкой      | Ждет X мс **тишины** и отправляет 1 запрос |
| **Buffer**               | Когда нужно **накопить пачку изменений** (текст, форма, данные) и отправить одним запросом | Собирает N изменений и отправляет 1 запрос |
| **Throttling**(debounce) | Если надо **разрешить запросы, но не чаще чем раз в X секунд**                             | Ограничивает количество запросов в секунду |
|                          |                                                                                            |                                            |

## Пример буфера если TextFlow

```kotlin
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val textFlow = MutableSharedFlow<String>(extraBufferCapacity = 10) // Поток для текста

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editText = EditText(this)
        setContentView(editText)

        // Запускаем обработку текста с debounce
        CoroutineScope(Dispatchers.IO).launch {
            textFlow
                .debounce(1000) // Ждем 1 сек паузы перед отправкой
                .collect { text ->
                    sendToServer(text)
                }
        }

        // Добавляем слушатель на ввод текста
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let { 
                    textFlow.tryEmit(it.toString()) // Отправляем текст в поток
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun sendToServer(text: String) {
        println("📤 Отправляем текст на сервер: $text")
    }
}
```

**✅ Как это работает?**

1. **Пользователь печатает** – вводится текст.
2. **Текст попадает в textFlow** (буферизуется).
3. **Если в течение 1 секунды не было изменений** – текст отправляется на сервер.
4. **Если пользователь продолжает печатать** – таймер сбрасывается и запрос не отправляется.

✅ **Плюсы:**

• Запросы не отправляются **по одной букве**.
• Экономится трафик и ресурсы сервера.
• Улучшается UX (нет лишней нагрузки на сеть).

**📌 2. Отправка по предложениям (по точке, ? или !)**
Иногда лучше **не ждать таймер**, а отправлять текст, когда пользователь закончил **предложение**.


**🔹 Реализация с отправкой по . ? !**
```kotlin
private val sentenceFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)

override fun onCreate(savedInstanceState: Bundle?) {
    val editText = EditText(this)
    setContentView(editText)

    CoroutineScope(Dispatchers.IO).launch {
        sentenceFlow
            .debounce(1500) // Ждем 1.5 сек перед отправкой
            .collect { text ->
                sendToServer(text)
            }
    }

    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            s?.let {
                val text = it.toString()

                // Проверяем, есть ли конец предложения (точка, вопросительный или восклицательный знак)
                if (text.endsWith(".") || text.endsWith("?") || text.endsWith("!")) {
                    sentenceFlow.tryEmit(text) // Отправляем сразу
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
```

✅ **Как работает?**

• Если пользователь набрал "Привет, как дела?" **и поставил ?**, текст **сразу отправляется**.
• Если пользователь печатает "Сегодня хорошая погода" и **не ставит точку**, сработает debounce(1500), и текст отправится через 1.5 сек паузы.

**📌 3. Буферизация и отправка пачками (каждые N символов)**

Если важно **отправлять текст частями**, например, **по 10 слов или 50 символов**, можно использовать **буферизацию**.
```kotlin
private val bufferedFlow = MutableSharedFlow<String>(extraBufferCapacity = 20)

override fun onCreate(savedInstanceState: Bundle?) {
    val editText = EditText(this)
    setContentView(editText)

    CoroutineScope(Dispatchers.IO).launch {
        bufferedFlow
            .buffer() // Сохраняем в буфере
            .debounce(2000) // Отправляем через 2 сек, если нет новых символов
            .collect { text ->
                sendToServer(text)
            }
    }

    editText.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            s?.let {
                val text = it.toString()
                if (text.length % 50 == 0) { // Отправляем каждые 50 символов
                    bufferedFlow.tryEmit(text)
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}
```

✅ **Как работает?**

• Текст **отправляется каждые 50 символов** или **если пользователь остановился на 2 секунды**.
• Подходит, если нужно **постепенно загружать текст** (например, Google Docs, онлайн-редактор).