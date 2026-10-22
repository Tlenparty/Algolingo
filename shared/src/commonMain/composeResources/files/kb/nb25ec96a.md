**Когда нужно использовать WebSockets:**

1. **Приложения для чатов и мессенджеров**:

• Сообщения должны доставляться мгновенно без необходимости постоянно опрашивать сервер.
• Например: WhatsApp, Telegram.

**Пример:**

Используйте WebSocket для получения новых сообщений от сервера и отправки сообщений в реальном времени.

2. **Онлайн-игры**:

• Требуется минимальная задержка и постоянное обновление данных между игроками.
• Например: многопользовательские игры, шахматы онлайн.

3. **Финансовые приложения**:

• Потоковые котировки акций, обменные курсы валют или криптовалют.
• Например: трейдинговые платформы.

4. **Уведомления в реальном времени**:

• Уведомления, которые нужно отправлять сразу после события на сервере, например, уведомления об обновлении заказа.
• Например: доставка еды, системы отслеживания заказов.

5. **Совместная работа в реальном времени**:

• Одновременное редактирование документа, обновление данных для всех подключенных клиентов.

• Например: Google Docs, совместные редакторы кода.

6. **IoT и умные устройства**:

• Обмен данными между приложением и устройствами в реальном времени, например, мониторинг датчиков.

```kotlin
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class WebSocketExample {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)  // Без таймаута для долгих соединений
        .build()

    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder()
            .url("wss://example.com/socket")  // URL WebSocket-сервера
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                println("Соединение установлено")
                webSocket.send("Привет серверу!")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                println("Получено сообщение: $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                println("Получено бинарное сообщение")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                println("Соединение закрыто: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                println("Ошибка: ${t.message}")
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun close() {
        webSocket?.close(1000, "Завершение соединения")
    }
}
```

Что можно передать через Сокет?

**1. Текстовые данные**

• Формат: **JSON** (очень популярен для передачи структурированной информации).

```json
{
    "type": "message",
    "user": "John",
    "content": "Hello, how are you?"
}
```
```kotlin
// На сервер уйдет
val message = """
    {
        "type": "message",
        "user": "John",
        "content": "Hello, how are you?"
    }
"""
webSocket.send(message)
```


**2. Бинарные данные**

• Формат: ByteString или ``byte[]``.
• Применяется для передачи изображений, аудио, видео, файлов или других данных.

```kotlin
val imageBytes: ByteArray = File("image.jpg").readBytes()
val byteString = ByteString.of(*imageBytes)
webSocket.send(byteString)
```

> [!info] Чем WebSocket отличается от HTTP
> Соединение **начинается как обычный HTTP-запрос** с заголовком `Upgrade: websocket` (handshake). Если сервер согласен — протокол переключается (код `101 Switching Protocols`), и поверх того же TCP открывается **постоянный полнодуплексный канал**: и клиент, и сервер могут слать данные в любой момент, без нового запроса. Схемы — `ws://` и `wss://` (поверх TLS, аналог HTTPS).
> В отличие от HTTP (модель запрос-ответ, сервер не может инициировать передачу), WebSocket держит соединение открытым — отсюда мгновенный push.

> [!warning] Что важно не забыть на проде
> • **Переподключение** — соединение рвётся (смена сети, сон, таймаут). Нужна логика reconnect с backoff. OkHttp сам этого **не делает**.
> • **Heartbeat (ping/pong)** — чтобы детектировать «мёртвое» соединение и не дать прокси/NAT его закрыть. `OkHttpClient.Builder().pingInterval(...)`.
> • **`readTimeout(0)`** в примере отключает таймаут чтения — это правильно для долгоживущего сокета, иначе OkHttp разорвёт «простаивающее» соединение.
> • **Код закрытия `1000`** — нормальное завершение. Есть и другие (`1001` — уход, `1006` — аномальный обрыв).

> [!tip] Когда WebSocket — избыточен
> Если данные текут **только от сервера к клиенту** (лента новостей, прогресс), проще взять **SSE (Server-Sent Events)** — это обычный HTTP-стрим, легче в эксплуатации. **Long-polling** — легаси-фоллбэк там, где WebSocket недоступен. WebSocket нужен именно когда требуется **двусторонний** обмен в реальном времени.

Связано: [[HTTP]], [[Retrofit]], [[FLOW]], [[Coroutines]]

