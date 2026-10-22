**Intent** — это объект в Android, используемый для передачи данных между компонентами приложения (Activity, Service, Broadcast Receiver) или для взаимодействия с другими приложениями. Intent представляет собой намерение выполнить какое-либо действие, такое как запуск нового экрана, выполнение фоновой операции или отправка сообщения.

**Основные задачи Intent**:

1. **Запуск Activity**:
	• Например, открыть новый экран.
2. **Запуск Service**:
	• Например, начать выполнение фоновой задачи.
3. **Отправка Broadcast**:
	• Например, уведомить систему или приложение о наступившем событии.
4. **Обмен данными между компонентами**:
	• Например, передать данные из одного экрана на другой.

**Типы Intent**
1. **Явный (Explicit Intent)**:
• Используется для запуска компонентов внутри приложения, когда известен точный класс, который нужно вызвать.
• Пример: запуск новой Activity.
2. **Неявный (Implicit Intent)**:
• Используется, когда точный компонент неизвестен, и система должна выбрать подходящий компонент на основе действия и типа данных.
• Пример: открытие ссылки в браузере.

### 1 Явный

**🚦 1. Явный Intent (Explicit Intent)**

**📌 Что это?**

• Указывает **точный класс** компонента, который нужно запустить.
• Используется, когда компонент известен заранее (например, переход от одной Activity к другой внутри вашего приложения).

**🛠 Пример: запуск другой Activity**
```kotlin
val intent = Intent(this, SecondActivity::class.java)
intent.putExtra("key", "value") // Передача данных
startActivity(intent)
```

**📦 Когда использовать?**

• Переход между Activity в одном приложении.
• Запуск Service внутри своего приложения.
• Взаимодействие с собственными BroadcastReceiver.

**2. Получение данных в другой Activity**
`val value = intent.getStringExtra("key")`

### **3. Неявный Intent**

• Не указывает конкретный компонент. Вместо этого указывает ==**действие (Action)**, которое нужно выполнить (например, открыть веб-страницу, сделать звонок или отправить сообщение).==
• Система Android сама выбирает подходящее приложение для выполнения действия, основываясь на ==**Intent Filter**.==


**🛠 Пример: открытие веб-сайта**
```kotlin
val intent = Intent(Intent.ACTION_VIEW)
intent.data = Uri.parse("https://www.google.com")
startActivity(intent)
```

**🔄 Пример: отправка email**
```kotlin
val emailIntent = Intent(Intent.ACTION_SEND)
emailIntent.type = "text/plain"
emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("example@mail.com"))
emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Тема письма")
emailIntent.putExtra(Intent.EXTRA_TEXT, "Текст письма")

if (emailIntent.resolveActivity(packageManager) != null) {
    startActivity(emailIntent)
}
```

### **Составляющие Intent**

1. **Action**:
• Указывает, что нужно сделать (Просмотреть, Отправить, Запустить Activity и т.д.).
🛠 **Примеры популярных Actions:**
• Intent.ACTION_VIEW — открыть ресурс (например, веб-страницу или контакт).
• Intent.ACTION_SEND — отправить данные (текст, файлы).
• Intent.ACTION_DIAL — открыть экран звонков с номером.
• Intent.ACTION_PICK — выбрать элемент из списка (например, контакт или изображение).

1. **Data**:
• URI данных, с которыми нужно выполнить действие (например, ссылка на сайт, телефонный номер).
• Пример: Uri.parse("https://example.com").
Ещё пример — для звонка:

```kotlin
val callIntent = Intent(Intent.ACTION_DIAL)
callIntent.data = Uri.parse("tel:+123456789")
startActivity(callIntent)
```

1. **Category**:
• Дополнительная информация о типе целевого компонента.
• Обычно используется с Intent Filters в манифесте.
🛠 **Примеры категорий:**
• CATEGORY_DEFAULT — для действий по умолчанию.
• CATEGORY_BROWSABLE — открытие ссылки в браузере.
• CATEGORY_LAUNCHER — запуск Activity из лаунчера.

 4 **Extras (Дополнительные данные)**
• Позволяют передавать ключ-значение данные (Bundle).
• Дополнительные данные, которые передаются через Intent.
• Пример: intent.putExtra("key", "value").

**6. Flags (Флаги)**
• Модифицируют поведение Intent (например, очистка стека Activity, запуск в новом Task’е).
• Задают дополнительные параметры, такие как поведение Activity в стеке.
• Пример: Intent.FLAG_ACTIVITY_NEW_TASK.
📌 **Примеры флагов:**

• FLAG_ACTIVITY_NEW_TASK — запуск Activity в новом Task’е.
• FLAG_ACTIVITY_CLEAR_TOP — завершает все Activity выше целевой.
• FLAG_GRANT_READ_URI_PERMISSION — даёт приложению доступ к URI данных.

**5. Component (Компонент)**

• Указывает конкретный компонент (Activity, Service, BroadcastReceiver), который нужно запустить.
```kotlin
intent.component = ComponentName("com.example.app", "com.example.app.SecondActivity")
```

**🎯 Итог: Из чего состоит Intent?**

• **Action** — Что нужно сделать? (ACTION_VIEW, ACTION_SEND, …).
• **Data** — С какими данными? (URI: https://, tel:, mailto:…).
• **Category** — Какой тип компонента нужен? (CATEGORY_DEFAULT, …).
• **Extras** — Какие дополнительные данные передать? (putExtra, Bundle).
• **Component** — Какой конкретный компонент запустить? (SecondActivity::class.java).
• **Flags** — Как изменить поведение? (FLAG_ACTIVITY_NEW_TASK, …).

> ==Если интент создать без указания класса компонента, то мы получим неявный интент.== В таком случае нам необходимо указать URI объект (уникальный идентификатор ресурсов) или тип MIME (многоцелевые расширения интернет почты) который был первоначально создан для определения типа переданных данных по электронной почте, но затем распространился и на другие аспекты. Для передачи URI можно воспользоваться конструктором, либо вызвать метод ``setData``. Если необходимо передать MIME тип, то надо воспользоваться методом``setType().`` При передачи и URI, и MIME необходимо воспользоваться методом ``setDataAndType()``

### *Что такое интент-фильтр?*

Интент-фильтр указывается в манифесте, чтобы по инициализации такого фильтра запустить тот или иной компонент. ==В **интент-фильтре** указывается **action** - объявляет значение действия, также можно указать типы принимаемых данных и категорию.==

### PendingIntent 

PendingIntent — это специальный объект в Android, который позволяет передать **интент (Intent)** другому приложению или системе, чтобы он был выполнен от имени вашего приложения с теми же правами.

PendingIntent используется, когда вы хотите отложить выполнение действия или передать право на его выполнение системе (например, уведомлениям, AlarmManager, виджетам или другим компонентам).

**Когда используется PendingIntent?**

1. **Уведомления (Notifications):**

Для указания действий, которые происходят при нажатии на уведомление.

2. AlarmManager**:**

Для планирования выполнения задачи в будущем.

3. BroadcastReceiver**:**

Для отложенной отправки Broadcast.

4. **Виджеты (App Widgets):**

Для обработки кликов по элементам виджета.

**Методы создания PendingIntent**

В зависимости от типа действия, которое вы хотите выполнить, используются три основных метода:

1. PendingIntent.getActivity():

Для запуска Activity.

2. PendingIntent.getService():

Для запуска Service.

3. PendingIntent.getBroadcast():


Для отправки широковещательного сообщения через BroadcastReceiver.

**Примеры использования**

**1. Уведомления**

```kotlin
val intent = Intent(this, MainActivity::class.java)
val pendingIntent = PendingIntent.getActivity(
    this, 
    0, 
    intent, 
    PendingIntent.FLAG_UPDATE_CURRENT
)

val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
    .setContentTitle("My Notification")
    .setContentText("Hello, world!")
    .setSmallIcon(R.drawable.ic_notification)
    .setContentIntent(pendingIntent) // Связываем PendingIntent с уведомлением
    .build()

val notificationManager = NotificationManagerCompat.from(this)
notificationManager.notify(1, notification)
```

**2. Планирование задач через AlarmManager**

```kotlin
val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

val intent = Intent(this, AlarmReceiver::class.java)
val pendingIntent = PendingIntent.getBroadcast(
    this, 
    0, 
    intent, 
    PendingIntent.FLAG_UPDATE_CURRENT
)

// Устанавливаем будильник на заданное время
val triggerTime = System.currentTimeMillis() + 60000 // Через 60 секунд
alarmManager.set(
    AlarmManager.RTC_WAKEUP,
    triggerTime,
    pendingIntent
)
```

**3. Виджеты (App Widgets)**

```kotlin
val intent = Intent(context, MainActivity::class.java)
val pendingIntent = PendingIntent.getActivity(
    context, 
    0, 
    intent, 
    PendingIntent.FLAG_UPDATE_CURRENT
)

val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

// Обновляем виджет
val appWidgetManager = AppWidgetManager.getInstance(context)
appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
```

**Флаги PendingIntent**

При создании PendingIntent можно указывать флаги для контроля его поведения. Основные флаги:

1. FLAG_UPDATE_CURRENT:

Обновляет существующий PendingIntent новыми данными из переданного Intent.

2. FLAG_CANCEL_CURRENT:

Удаляет существующий PendingIntent перед созданием нового.

3. FLAG_ONE_SHOT:

PendingIntent может быть вызван только один раз. После вызова он становится недействительным.

4. FLAG_IMMUTABLE:

Указывает, что PendingIntent нельзя изменять после создания.

> [!important] Android 12+ требует явный mutability-флаг
> Начиная с **Android 12 (API 31)**, при создании `PendingIntent` **обязательно** указывать `FLAG_IMMUTABLE` **или** `FLAG_MUTABLE` — иначе `IllegalArgumentException`. Примеры выше с одним `FLAG_UPDATE_CURRENT` на Android 12+ **упадут**. Правильно:
> ```kotlin
> PendingIntent.getActivity(
>     this, 0, intent,
>     PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
> )
> ```
> По умолчанию выбирайте **`FLAG_IMMUTABLE`** (безопаснее). `FLAG_MUTABLE` нужен редко — например, для inline-reply в уведомлениях.

> [!tip] Явный vs неявный Intent — на собесе
> - **Явный** — указан класс компонента (`Intent(this, X::class.java)`); для навигации внутри приложения.
> - **Неявный** — указано действие (`ACTION_VIEW`) + данные/MIME; систему просим найти обработчик по `intent-filter`. С Android 11+ нужен `<queries>` в манифесте, чтобы видеть другие приложения (package visibility).
> - **`startActivity` для неявного** может бросить `ActivityNotFoundException`, если нет обработчика — оборачивай в проверку/try-catch.

Связано: [[Что такое Manifest]], [[Activity, Service,  Broadcast Receiver,  Content Provider]], [[Activity и Fragment]], [[Parcelable vs Serializable]]
