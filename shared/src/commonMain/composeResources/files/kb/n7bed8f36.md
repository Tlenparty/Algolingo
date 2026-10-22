# Отличия между Service Locator и Dependency Injection

> [!note]
> Тема пересекается с [[DI VS SERVICE LOCATOR]] — там подробнее про плюсы/минусы. Здесь — кратко про принцип и виды внедрения.

## Service Locator

Паттерн, дающий централизованный механизм получения зависимостей. Вместо передачи зависимостей объектам они «запрашиваются» из глобального регистратора (локатора).

**Как работает:**

1. Есть класс-локатор, хранящий все зависимости.
2. Объекты запрашивают зависимости методом вроде `getService()`.

```kotlin
object ServiceLocator {
    private val services = mutableMapOf<Class<*>, Any>()

    fun <T : Any> registerService(serviceClass: Class<T>, implementation: T) {
        services[serviceClass] = implementation
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getService(serviceClass: Class<T>): T =
        services[serviceClass] as? T
            ?: throw IllegalArgumentException("Service not found: ${serviceClass.name}")
}

// Регистрация
ServiceLocator.registerService(ApiService::class.java, ApiServiceImpl())

// Получение
val apiService = ServiceLocator.getService(ApiService::class.java)
```

## Dependency Injection (DI)

Паттерн, при котором зависимости **предоставляются объекту извне**, а не создаются/запрашиваются им самим. Это реализация принципа **Inversion of Control (IoC)**: управление зависимостями делегируется внешней системе.

Зависимости передаются через:

1. **Конструктор** (Constructor Injection) — предпочтительный способ.
2. **Сеттеры / свойства** (Setter Injection).
3. **Интерфейсы** (Interface Injection).

```kotlin
interface ApiService {
    fun fetchData(): String
}

class ApiServiceImpl : ApiService {
    override fun fetchData(): String = "Data from API"
}

class MyClass(private val apiService: ApiService) { // constructor injection
    fun printData() = println(apiService.fetchData())
}

val myClass = MyClass(ApiServiceImpl())
myClass.printData()
```

## Основные отличия

| Критерий                | Dependency Injection                          | Service Locator                              |
| ----------------------- | --------------------------------------------- | -------------------------------------------- |
| Получение зависимостей  | Передаются объекту извне                       | Объект сам запрашивает у локатора            |
| Явность зависимостей    | Явные — видны в конструкторе/свойствах        | Неявные — спрятаны в коде, тянутся из локатора |
| Простота реализации     | Нужен DI-контейнер или ручная передача         | Проще: достаточно создать локатор            |
| Тестируемость           | Высокая — подмена моков через конструктор     | Ниже — нужно мокать сам локатор              |

> [!tip]
> На собесе ключевой тезис: **DI делает зависимости явными, Service Locator — скрывает их**. Поэтому DI предпочтительнее (честная сигнатура, проще тесты), а Service Locator считают антипаттерном. Оба — частные случаи **IoC**.

Связано: [[DI VS SERVICE LOCATOR]], [[Dagger 2]], [[Чистая архитектура]]
