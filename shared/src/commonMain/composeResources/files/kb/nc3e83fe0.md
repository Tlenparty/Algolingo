**Retrofit** — это популярная библиотека для сетевых запросов в Android и Kotlin, которая упрощает взаимодействие с **REST API**. Под капотом **Retrofit** использует **OkHttp** для выполнения HTTP-запросов, а **Java Dynamic Proxy** для автоматической генерации реализации интерфейсов API.

**📌 Как это работает: Основной принцип**

1. **Описание API**: Вы создаёте интерфейс с аннотациями Retrofit для описания API-запросов.

2. **Создание экземпляра API через Retrofit.create()**: Здесь и используется **Dynamic Proxy**.

3. **Вызов метода API**: Прокси-объект перехватывает вызов метода и формирует HTTP-запрос с помощью **OkHttp**.

4. **Получение ответа и конвертация**: Ответ обрабатывается **Converter’ом** (например, Gson) и возвращается в удобном формате.

**📌 Пример: Как это выглядит на практике?**


**📍 1. Интерфейс API**
```kotlin
import retrofit2.http.GET
import retrofit2.Call

interface MyApiService {
    @GET("data")
    fun getData(): Call<String>
}
```

**📍 2. Настройка Retrofit**
```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit = Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

// Создание экземпляра API через Dynamic Proxy
val api = retrofit.create(MyApiService::class.java)
```

**📍 3. Выполнение запроса**
```kotlin
import kotlinx.coroutines.runBlocking

runBlocking {
    try {
        val response = api.getData().execute()
        if (response.isSuccessful) {
            println("Response: ${response.body()}")
        } else {
            println("Error: ${response.errorBody()}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

**📌 Разбор: Как всё это работает?**

**📍 1. Dynamic Proxy: Создание API**

Когда вы вызываете:
```kotlin
val api = retrofit.create(MyApiService::class.java)
```

**Retrofit** создаёт **Dynamic Proxy** для интерфейса **MyApiService**:
```java
// Пример внутреннего кода Retrofit
public <T> T create(final Class<T> service) {
    return (T) Proxy.newProxyInstance(
        service.getClassLoader(),
        new Class<?>[] {service},
        new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(this, args);
                }
                ServiceMethod<Object, Object> serviceMethod = loadServiceMethod(method);
                OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
                return serviceMethod.adapt(okHttpCall);
            }
        });
}
```

**📍 2. Вызов метода API**


Когда вы вызываете **api.getData()**, **Dynamic Proxy**:

• Перехватывает вызов метода.
• Создаёт объект **OkHttpCall**, который умеет выполнять HTTP-запрос через **OkHttp**.

**📍 3. Формирование HTTP-запроса через OkHttp**

**Retrofit** переводит метод @GET("data") в полноценный HTTP-запрос:
```java
Request request = new Request.Builder()
    .url("https://example.com/data")
    .get()
    .build();

OkHttpClient client = new OkHttpClient();
Response response = client.newCall(request).execute();
```

**📌 Как OkHttp и Retrofit работают вместе?**

1. **Retrofit** конвертирует аннотации и аргументы метода в **Request** для **OkHttp**.

2. **OkHttp** выполняет сетевой запрос.

3. **Retrofit** обрабатывает **Response** и преобразует его в нужный формат с помощью **Converter** (например, **GsonConverter**).

**📌 Когда именно используется Dynamic Proxy?**

• Только при вызове метода API (например, **getData()**).

• Для каждого метода интерфейса создаётся уникальный обработчик (InvocationHandler).

• Обработчик формирует и адаптирует вызов через **OkHttp**.


**🚀 Итог:**

1. 🛠️ **Java Dynamic Proxy**:

• Автоматически генерирует реализацию API-интерфейса.

• Перенаправляет вызовы методов к **InvocationHandler**, который обрабатывает запрос.

2. 🚦 **Retrofit**:

• Формирует HTTP-запрос из аннотаций (@GET, @POST и т.д.).

• Использует **OkHttp** для отправки запроса.

• Преобразует ответ через **Converter**.

3. 🚀 **OkHttp**:

• Выполняет реальный HTTP-запрос.

• Поддерживает перехватчики, таймауты, кеширование и асинхронные запросы.

4. ⚡ **Корректная обработка асинхронных вызовов**:

• Поддержка **suspend** функций и интеграция с **Kotlin Coroutines**.

> [!tip] Современный стиль вместо `Call.execute()`
> В примере выше `api.getData().execute()` — **синхронный, блокирующий** вызов (поэтому он в `runBlocking`). На практике так не делают. Варианты:
> - **suspend-функция** (рекомендуется) — Retrofit сам уводит запрос с главного потока:
>   ```kotlin
>   @GET("data")
>   suspend fun getData(): MyResponse           // тело сразу, без Call
>
>   // или с метаданными ответа:
>   suspend fun getData(): Response<MyResponse>  // код, заголовки, errorBody
>   ```
> - **`Call.enqueue { }`** — асинхронный колбэк (legacy).
>
> Где что настраивается: **таймауты, логирование, авторизация, повторы** — это **OkHttp Interceptors** (`addInterceptor`), а не Retrofit. Конвертер (`GsonConverterFactory` / `kotlinx.serialization` / Moshi) превращает JSON в модели.

Связано: [[WebSocket для чего]], [[HTTP]], [[Coroutines]], [[Coroutines + callback]], [[Чистая архитектура]]