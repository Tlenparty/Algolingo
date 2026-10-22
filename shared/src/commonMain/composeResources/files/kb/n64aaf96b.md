**🛠 Простой Service Locator (ручная регистрация)**

```kotlin
object ServiceLocator {
    private val services = mutableMapOf<Class<*>, Any>()

    fun <T : Any> register(service: T) {
        services[service::class.java] = service
    }

    fun <T : Any> resolve(clazz: Class<T>): T {
        return services[clazz] as? T
            ?: throw IllegalStateException("Сервис ${clazz.simpleName} не зарегистрирован!")
    }
}

// Пример сервиса
class UserService {
    fun getUser() = "Пользователь: Иван"
}

fun main() {
    // Регистрируем сервис
    ServiceLocator.register(UserService())

    // Получаем сервис
    val userService = ServiceLocator.resolve(UserService::class.java)
    println(userService.getUser()) // Пользователь: Иван
}
```

**🛠 Улучшенная версия (Generics + Lazy)**

Делаем **ленивую инициализацию** зависимостей и **автоматическую регистрацию**:

```kotlin
object DIContainer {
    private val services = mutableMapOf<Class<*>, () -> Any>()

    fun <T : Any> register(clazz: Class<T>, provider: () -> T) {
        services[clazz] = provider
    }

    fun <T : Any> resolve(clazz: Class<T>): T {
        val provider = services[clazz] ?: throw IllegalStateException("Сервис ${clazz.simpleName} не зарегистрирован!")
        return provider.invoke() as T
    }
}

// Пример классов
class Logger {
    fun log(message: String) = println("LOG: $message")
}

class UserService(private val logger: Logger) {
    fun getUser(): String {
        logger.log("Получение пользователя")
        return "Пользователь: Олег"
    }
}

fun main() {
    // Регистрируем зависимости
    DIContainer.register(Logger::class.java) { Logger() }
    DIContainer.register(UserService::class.java) { UserService(DIContainer.resolve(Logger::class.java)) }

    // Используем
    val userService = DIContainer.resolve(UserService::class.java)
    println(userService.getUser()) // LOG: Получение пользователя -> Пользователь: Олег
}
```

**🏗 Полноценный DI Container с delegate и by**


Теперь сделаем **автоматический DI** с **делегатом**:

```kotlin
import kotlin.reflect.KClass

class DIContainer {
    private val services = mutableMapOf<KClass<*>, Any>()

    inline fun <reified T : Any> register(instance: T) {
        services[T::class] = instance
    }

    inline fun <reified T : Any> resolve(): T {
        return services[T::class] as? T
            ?: throw IllegalStateException("Сервис ${T::class.simpleName} не зарегистрирован!")
    }
}

// Делегат для автоматического DI
class Inject<T : Any>(private val di: DIContainer, private val clazz: KClass<T>) {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        return di.resolve()
    }
}

inline fun <reified T : Any> inject(di: DIContainer) = Inject(di, T::class)

// Пример
class Logger {
    fun log(message: String) = println("LOG: $message")
}

class UserService(di: DIContainer) {
    private val logger by inject<Logger>(di) // Авто-инъекция через делегат

    fun getUser(): String {
        logger.log("Запрос пользователя")
        return "Пользователь: Андрей"
    }
}

fun main() {
    val di = DIContainer()
    di.register(Logger())  // Регистрируем сервис
    di.register(UserService(di)) // Передаем DI-контейнер в зависимости

    val userService: UserService = di.resolve()
    println(userService.getUser()) // LOG: Запрос пользователя -> Пользователь: Андрей
}
```

**🔥 Итог**

1. **Service Locator (MutableMap)** → простая версия, но сервисы регистрируются вручную.
2. **Lazy DI (с** provider**)** → позволяет передавать зависимости внутри DI.
3. **Полноценный DI (**by inject**)** → удобный вариант, который похож на настоящие DI-фреймворки.

> [!important] Строго говоря, это Service Locator, а не DI
> Во всех трёх примерах объект **сам достаёт** зависимость из контейнера (`resolve()` / `by inject`). Это **Service Locator**: зависимость от контейнера «прошита» в класс, его сложнее тестировать (нужен контейнер) и не видно зависимостей по сигнатуре. Настоящий **Dependency Injection** — когда зависимости **передают снаружи** (через конструктор), а класс о контейнере не знает. Вариант 2 ближе к DI (`UserService(logger)`), но регистрация всё равно тянет из контейнера. См. [[DI VS SERVICE LOCATOR]].

> [!warning] `as? T` / `as T` — небезопасный каст из-за стирания типов
> `services[clazz] as? T` и `provider.invoke() as T` дают **unchecked cast**: дженерик-тип `T` стёрт на рантайме, реальную проверку обеспечивает только то, что ключом служит сам `Class`/`KClass`. Если в мапу по ошибке положить другой тип под тем же ключом — поймаем `ClassCastException` уже в месте использования. `inline`+`reified` (3-й пример) с `T::class` надёжнее, чем передача `Class<T>` руками.

> [!warning] Потокобезопасность и время жизни
> • `mutableMapOf` **не потокобезопасен** — при регистрации/резолве из разных потоков нужен `ConcurrentHashMap` или синхронизация.
> • В примерах нет **скоупов** и **синглтон-семантики**: 2-й контейнер создаёт новый объект на **каждый** `resolve` (provider зовётся каждый раз), 1-й и 3-й, наоборот, хранят единственный инстанс без возможности пересоздать. Настоящие DI управляют жизненным циклом (singleton/scoped/factory).
> • `di.register(UserService(di))` создаёт сервис **сразу** (eager) и требует, чтобы `Logger` уже был зарегистрирован — порядок регистрации критичен. Ленивость (provider/`by lazy`) эту проблему снимает.

> [!tip] С чем сравнивать в проде
> • **Dagger 2 / Hilt** — кодогенерация, граф проверяется **на этапе компиляции** (ошибки до запуска), нулевой рантайм-оверхед на рефлексии.
> • **Koin** — рантайм, по духу ближе к этому «DI на коленке» (Service Locator с DSL), проще, но ошибки графа всплывают в рантайме.

Связано: [[DI VS SERVICE LOCATOR]], [[Отличия между Service Locator и Dependency injection.]], [[Dagger 2]], [[Inline, Noinline, cross inline, reified]], [[Делегаты.  Lateinit and Lazy]], [[object и companion object]]