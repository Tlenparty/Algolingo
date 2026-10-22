# 📚 СОДЕРЖАНИЕ

Навигатор по всему хранилищу. Клик по ссылке → переход к заметке (Obsidian).
Разбито по темам для подготовки к собеседованиям (Android / Kotlin).

**Быстрый переход:** [Алгоритмы](#-алгоритмы-и-структуры-данных) · [Kotlin/Java](#-kotlin--java) · [Многопоточность](#-многопоточность) · [Корутины и Flow](#-корутины-и-flow) · [Compose](#-jetpack-compose) · [Android](#-android) · [Архитектура](#-архитектура) · [Performance](#-performance) · [KMP](#-kmp) · [System Design](#-system-design) · [Исключения](#-исключения) · [Вопросы с собесов](#-вопросы-с-собесов) · [Для работы](#-для-работы) · [Работа за рубежом](#-работа-за-рубежом)

---

## 🧭 Алгоритмы и структуры данных

### Метод и подготовка
- [[00 Компас алгоритмов]] — с чего начать: тип задачи → метод
- [[01 Ключи и сигналы]] — распознать задачу по словам в условии
- [[02 Шаблон решения]] — каркас U-E-P-B-O-C-S + worked example
- [[03 Тренажёр задач]]
- [[04 Маркеры + Заготовки]]
- [[Подготовка к алгоритмической секции]] — разбор задач CodeRun по компасу
- [[BIG O]] · [[Алгоритмические сложности]]

### Приёмы и шаблоны
- [[Бинарный поиск]]
- [[Задача на 2 указателя]]
- [[Задача на stack и map]]
- [[Задача на поиск в ширину]] — BFS
- [[Поиск в глубину (DFS)]] — DFS / backtracking
- [[Динамическое программирование]]
- [[Бублесорт]] — сортировка

### Строки
- [[Если слово палиндром]]
- [[Проверка является ли слово анаграммой]]

### Структуры данных
- [[LRU Cache]]
- [[Структуры данных]]

### Задачи компаний
- [[Алгоритмы Avito]] · [[Задачи с Avito]]
- [[Алгоритмы Glovo]]
- [[Задача от Яндекс со станциями]]

---

## ☕ Kotlin / Java

### ООП и базис языка
- [[ООП]]
- [[Abstract class vs Interface,  Полиморфизм]]
- [[Композиция vs наследование]]
- [[Модификаторы доступа и их видимость]]
- [[Типы данных, отличие const val от val, есть ли в Котлин примитивы]]
- [[Any,Nothing,Unit]]
- [[Object]]

### Классы и объекты
- [[Data class]]
- [[Sealed class vs enum]]
- [[object и companion object]]
- [[Nested class vs inner class]]
- [[Вторичный конструктор]]
- [[Backing Field в Kotlin]]
- [[Inline class, value class, typealias, infix]]
- [[Делегаты.  Lateinit and Lazy]]
- [[Кто поддерживает деструктуризацию в Kotlin ?]]

### Функции, лямбды, дженерики
- [[Лямбда]]
- [[Функциональные типы в Kotlin]]
- [[Reference function]]
- [[Extension функции]]
- [[let, apply,  with, run, also]]
- [[Inline, Noinline, cross inline, reified]]
- [[Spread operator и vararg]]
- [[Generic]] — ковариантность / контравариантность / PECS

### Равенство и коллекции
- [[EQUALS, HASHCODE]]
- [[HashMap vs HashSet]]
- [[Stream vs Collection vs Sequence]]
- [[String]]
- [[Побитовое сравнение]]

### Память и JVM
- [[Heap & Stack]]
- [[Аллокация в jvm]]
- [[GC, Виды ссылок, GC roots]]

---

## 🧵 Многопоточность

### Базовые понятия
- [[Процессы и потоки]]
- [[Синхронные и асинхронные операции]]
- [[Handler и Looper]]

### Синхронизация и память
- [[Volatile & Synchronized, Atomic]]
- [[Happens before]]
- [[Правила семантики в jvm]]
- [[ReentrantLock, Semaphore и CyclicBarrier]]
- [[Как работает CountDownLatch]]
- [[Structured Concurrency]]

> Низкоуровневое из «Вопросы с собесов»: [[Atomic принципы]] · [[Атомарность]] · [[Volatile]] · [[Потоки]] · [[Пул потоков]] · [[ExecutorService]]

---

## 🌀 Корутины и Flow

### Корутины
- [[Coroutines]]
- [[Coroutines. Вопрос ответ]]
- [[Coroutines + callback]]
- [[Задачи на корутины]]
- [[КОРУТИНЫ]] — задачи на вывод (structured concurrency, async/await)

### Flow
- [[FLOW]]
- [[SharedFlow vs Flow vs StateFlow]]
- [[BackPressure в Flow что это и как решается]]

---

## 🎨 Jetpack Compose

### Основы и внутреннее устройство
- [[Compose]]
- [[Compose. Recomposition]]
- [[Compose. Remember]]
- [[Compose. Effect Handlers]]
- [[Compose. Composition Local]]
- [[Compose. Composer and Snapshot]]
- [[Compose. Snapshot]] · [[Compose. Snapshot 1]]
- [[Compose. Slot api]] · [[Compose. Slot table]]
- [[Compose. SubcomposeLayout]]

### Оптимизация и анимации
- [[Compose. Оптимизация]]
- [[Compose. Стабильность]]
- [[Compose. Анимации]]
- [[KPI]] · [[KPI (2)]]

---

## 🤖 Android

### Компоненты приложения
- [[Android]]
- [[Activity и Fragment]]
- [[Activity, Service,  Broadcast Receiver,  Content Provider]]
- [[Intent]]
- [[Что такое Manifest]]
- [[Jetpack Navigation]]
- [[Передача данных между фрагментами]]
- [[Context]]

### ViewModel и состояние
- [[AndroidViewModel vs ViewModel]]
- [[ViewModel, SaveStateHandle, SavedStateRegistryOwner]]
- [[Сохранение состояние в android и Bundle]]
- [[Parcelable vs Serializable]]

### UI, View, рендеринг
- [[Custom view]]
- [[EditText]]
- [[RecyclerView, Diff Utils, List Adapter]]
- [[Recycler. ViewPool, PayLoads]]
- [[Анимации в Android]]
- [[Choreographer, SurfaceFlinger]]
- [[Render Thread]]
- [[Вызов в потоке UI]]
- [[Плотность экрана в Android]]
- [[Coil]]
- [[Exo player]]

### Данные и сеть
- [[Retrofit]]
- [[Room]]
- [[Room or SharedPreferences]]
- [[DataStore]]

### Фон, система, процессы
- [[WorkManager]]
- [[Standby Bucket + Doze Mode]]
- [[Приоритеты процессов]]
- [[Способы передачи геолокации в фоновом режиме]]
- [[Передача точной геолокации с интервалом времени]]

### DI
- [[Dagger 2]]
- [[Hilt]]
- [[Свой DI на коленке]]

### Сборка, безопасность, версии
- [[BuildType, BuildFlavour, BuildVariant]]
- [[Gradle build-оптимизация]]
- [[R8, ProGuard, обфускация]]
- [[Безопасность в Android]]
- [[Разница в SDK]]
- [[ML Kit Barcode Scanning в Android]]

---

## 🏛 Архитектура

### Принципы и паттерны
- [[SOLID]]
- [[Чистая архитектура]]
- [[Паттерны]]
- [[Примеры декораторов]]
- [[Примеры стратегии]]
- [[Модуляризация]]

### Презентационные архитектуры
- [[Отличия MVI от MVVM,  MVC]]
- [[MVI библиотеки]]
- [[UDF (Unidirectional Data Flow)]]

### DI как подход
- [[DI VS SERVICE LOCATOR]]
- [[Отличия между Service Locator и Dependency injection.]]

---

## ⚡ Performance

- [[Performance. Обзор и метрики]]
- [[Performance. Рендеринг, jank и Macrobenchmark]]
- [[Performance. Память и утечки]]
- [[Performance. Оптимизация Compose]]
- [[Performance. Baseline и Startup Profiles]]

---

## 📱 KMP

- [[KMP]]
- [[KMP. expect-actual]]
- [[KMP. Source sets и Gradle]]
- [[KMP. Compose Multiplatform]]
- [[KMP. iOS interop и корутины]]
- [[KMP. Библиотеки экосистемы]]
- [[KMP. Сборка и доставка iOS-приложения]]
- [[KMP. Вопросы с собеса]]

---

## 🛰 System Design

- [[План интервью]]
- [[Основные разделы и Ключевые принципы мобильного системного дизайна**]]
- [[System Design с учетом Kotlin Multiplatform (KMP)]]
- [[Кеширование через заголовки]]
- [[Очередь сообщений]]
- [[Возможные варианты формирования буфера]]
- [[Локальный поиск на устройстве]]
- [[Конфликты состояний с backend]]
- [[Решение конфликта если несколько пользваков текст редактируют]]
- [[Как вернуть WorkManager к жизни]]
- [[Как вернуть сделать апдейт изменений если изменились условия связи?]]

---

## ⚠️ Исключения

- [[Иерархия]]
- [[Как перехватить все исключения в run time]]

---

## 🎯 Вопросы с собесов

### Сводные разборы
- [[Тех собес яндекс]] — полный разбор вопросов (Kotlin, корутины, Compose, архитектура + задачи)
- [[Подготовка к алгоритмической секции]]

### Коллекции (под капотом)
- [[Коллекции, основные виды]]
- [[Как работает HashMap]]
- [[HashMap]] · [[HashSet]] · [[TreeSet]] · [[LinkedList]]
- [[Как выглядит Bucket при ключах с одинаковым hashCode в HashMap]]
- [[Как сравниваются ключи в HashMap при одинаковом hashCode]]

### Примитивы
- [[Базовые примитивные типы]]
- [[Почему в коллекциях не используются примитивные типы]]
- [[Финансовые форматы]]

### Многопоточка (вопросы)
- [[Вопросы про многопоточку от Газпромбанка]]
- [[Atomic принципы]] · [[Атомарность]] · [[Volatile]] · [[Потоки]] · [[Пул потоков]] · [[ExecutorService]]

### Прочее
- [[Вопросы с собесов про apply]]
- [[Оптимизация UI, вопрос с WB]]

### Сеть и базы (общие вопросы)
- [[HTTP]]
- [[WebSocket для чего]]
- [[Атомарность и Транзакции]]
- [[Метод main в Android]]
- [[Fragment transaction]]
- [[Могут ли объекты перемещаться в памяти]]
- [[Приватный конструктор]]
- [[CodeReview]]

### Лайвкодинг и задачи компаний
- [[Задача на flow от Совкомбанка]]
- [[Корутины от Альфы]]
- [[Задача на оптимизацию Compse кода c WB]]
- [[Задача на рефакторинг от Т-Банка]]
- [[Задача от VK на лайвкодинг]]
- [[Задача по легенде с Я. Кеш и Mutex]]
- [[Задача с Авито]]
- [[Создание своего Executer]]
- [[Пиздец от Яндекса]]
- [[Вопросы с WB]]
- [[CityDrive]] · [[Х5]]

---

## 🛠 Для работы

- [[Extension для List]]
- [[Spec]]

---

## 🌍 Работа за рубежом

- [[00 Remote worldwide — чеклист]] — компании без релокейта, аргентинский трек, борды, трекер откликов, шаблон письма
- [[01 LinkedIn — итоговый профиль]] — готовый текст под копипаст: Headline, About, Experience, Skills, настройки

---

> 💡 Файл-оглавление. Если добавляешь новую заметку — допиши ссылку в нужный раздел, чтобы навигатор оставался полным.
