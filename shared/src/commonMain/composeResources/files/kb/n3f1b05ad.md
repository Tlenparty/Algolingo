**Паттерн “Стратегия” (Strategy Pattern)** — это **поведенческий паттерн**, который позволяет **выбирать поведение объекта во время выполнения** без изменения его структуры.

📌 **Где в Android можно применять стратегию?**

✅ Разные алгоритмы сортировки (Comparator)
✅ Разные способы авторизации (Google, Facebook, Email)
✅ Разные механизмы кэширования (LruCache, Room, DataStore)
✅ Разные стратегии загрузки изображений (Glide, Picasso, Coil)
✅ Разные варианты анимаций (ObjectAnimator, ViewPropertyAnimator)

**1️⃣ Пример: Стратегия для Авторизации (Google, Email, Facebook)**
**✅ Создаем интерфейс стратегии авторизации**
```kotlin
interface AuthStrategy {
    fun authenticate(username: String, password: String): Boolean
}
```
**✅ Реализация Google авторизации**
```kotlin
class GoogleAuthStrategy : AuthStrategy {
    override fun authenticate(username: String, password: String): Boolean {
        println("Authenticating with Google...")
        return true // Здесь будет реальный код Google API
    }
}
```
**✅ Реализация Email авторизации**
```kotlin
class EmailAuthStrategy : AuthStrategy {
    override fun authenticate(username: String, password: String): Boolean {
        println("Authenticating with Email and Password...")
        return username == "user@example.com" && password == "password123"
    }
}
```

**✅ Реализация Facebook авторизации**

```kotlin
class FacebookAuthStrategy : AuthStrategy {
    override fun authenticate(username: String, password: String): Boolean {
        println("Authenticating with Facebook...")
        return true // Здесь будет реальный код Facebook API
    }
}
```

**✅ Класс, использующий стратегию**
```kotlin
class Authenticator(private var authStrategy: AuthStrategy) {
    fun setStrategy(strategy: AuthStrategy) {
        authStrategy = strategy
    }

    fun login(username: String, password: String): Boolean {
        return authStrategy.authenticate(username, password)
    }
}
```

**✅ Использование**
```kotlin
fun main() {
    val authenticator = Authenticator(GoogleAuthStrategy()) // По умолчанию Google

    authenticator.login("user@gmail.com", "password") // Google авторизация

    authenticator.setStrategy(EmailAuthStrategy()) // Меняем стратегию
    authenticator.login("user@example.com", "password123") // Email авторизация

    authenticator.setStrategy(FacebookAuthStrategy()) // Меняем стратегию
    authenticator.login("user@facebook.com", "password456") // Facebook авторизация
}
```

📌 **Теперь можно легко менять способ авторизации без переписывания кода!** 🚀

**2️⃣ Пример: Стратегия для загрузки изображений (Glide, Picasso, Coil)**

Допустим, мы хотим использовать **разные библиотеки** (Glide, Picasso, Coil) для загрузки изображений.

**✅ Создаем интерфейс стратегии**
```kotlin
interface ImageLoaderStrategy {
    fun loadImage(url: String, imageView: ImageView)
}
```

**✅ Реализация для Glide**
```kotlin
class GlideImageLoader : ImageLoaderStrategy {
    override fun loadImage(url: String, imageView: ImageView) {
        Glide.with(imageView.context)
            .load(url)
            .into(imageView)
    }
}
```

**✅ Реализация для Picasso**
```kotlin
class PicassoImageLoader : ImageLoaderStrategy {
    override fun loadImage(url: String, imageView: ImageView) {
        Picasso.get()
            .load(url)
            .into(imageView)
    }
}
```
**✅ Реализация для Coil**
```kotlin
class CoilImageLoader : ImageLoaderStrategy {
    override fun loadImage(url: String, imageView: ImageView) {
        imageView.load(url) // Coil встроен в Kotlin
    }
}
```

**✅ Контроллер для смены стратегии**
```kotlin
class ImageLoader(private var strategy: ImageLoaderStrategy) {
    fun setStrategy(newStrategy: ImageLoaderStrategy) {
        strategy = newStrategy
    }

    fun load(url: String, imageView: ImageView) {
        strategy.loadImage(url, imageView)
    }
}
```

**✅ Использование**

```kotlin
val imageLoader = ImageLoader(GlideImageLoader()) // По умолчанию Glide

imageLoader.load("https://example.com/image.jpg", imageView) // Загрузит через Glide

imageLoader.setStrategy(PicassoImageLoader()) // Меняем стратегию на Picasso
imageLoader.load("https://example.com/image.jpg", imageView)

imageLoader.setStrategy(CoilImageLoader()) // Меняем стратегию на Coil
imageLoader.load("https://example.com/image.jpg", imageView)
```
📌 Теперь можно **легко менять библиотеку загрузки изображений** без переписывания логики!

**📌 Когда Использовать Стратегию?**

✅ Когда **один объект должен поддерживать несколько вариантов поведения**, но выбор делается во **время выполнения**.
✅ Когда **нельзя** или **неудобно** использовать if/else или when.
✅ Когда **хочешь упростить поддержку кода** и **избежать дублирования логики**.
✅ Когда **разные реализации могут меняться без изменения основного кода**.

> [!tip] В Kotlin часто без интерфейса
> Если у стратегии один метод, в Kotlin её идиоматичнее задать **функциональным типом**, а не интерфейсом с классами:
> ```kotlin
> class Authenticator(private var authStrategy: (username: String, password: String) -> Boolean) {
>     fun login(u: String, p: String) = authStrategy(u, p)
> }
>
> val auth = Authenticator { u, p -> u == "user@example.com" && p == "password123" } // email
> // сменить стратегию — просто передать другую лямбду
> ```
> Это та же «Стратегия», но без бойлерплейта из интерфейса и трёх классов. Полноценный интерфейс оправдан, когда у стратегии несколько методов или нужно хранить состояние.

Связано: [[Паттерны]], [[Примеры декораторов]], [[SOLID]], [[Функциональные типы в Kotlin]]