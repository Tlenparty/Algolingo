В Android есть несколько способов реализовать **фоновую передачу геопозиции**, в зависимости от требований к частоте обновлений, энергопотребления и ограничений системы.

**🔹 1. Foreground Service (Служба переднего плана) **

Это **лучший способ** для непрерывного отслеживания местоположения в фоне. Используется Foreground Service с уведомлением.

✅ **Работает на Android 8+**, даже если приложение свернуто.

🚀 **Лучше всего подходит для GPS-трекинга в реальном времени**.


**📌 Как реализовать?**

• Используем FusedLocationProviderClient для получения геопозиции.
• Запускаем **Foreground Service** с уведомлением.
• Используем LocationRequest с подходящими настройками.
```kotlin

class LocationForegroundService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, createNotification()) // Обязательно уведомление
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 сек
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("LocationService", "New location: ${location.latitude}, ${location.longitude}")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun createNotification(): Notification {
        val channelId = "location_channel"
        val notificationChannel = NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(notificationChannel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Tracking")
            .setContentText("Tracking location in background")
            .setSmallIcon(R.drawable.ic_location)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

**Плюсы Foreground Service**

✅ Работает стабильно даже при сворачивании приложения.
✅ Поддерживается на всех версиях Android (начиная с 8.0+).
✅ Можно использовать Notification для информирования пользователя.

**Минусы**
❌ Повышенное энергопотребление.
❌ Требует разрешения **ACCESS_BACKGROUND_LOCATION** на Android 10+.


**🔹 2. WorkManager (Для периодической передачи координат)**

Если вам **не нужно** отслеживать геопозицию **в реальном времени**, можно использовать WorkManager.

✅ **Работает даже после перезапуска устройства**.
🚀 **Хороший вариант для обновления местоположения раз в 15-30 минут**.

**📌 Как реализовать?**

```kotlin

class LocationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("LocationWorker", "Location: ${location.latitude}, ${location.longitude}")
            }
        }
        return Result.success()
    }
}

// Запуск WorkManager каждые 15 минут
val workRequest = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES).build()
WorkManager.getInstance(context).enqueue(workRequest)
```

**Плюсы WorkManager**
✅ Экономит заряд батареи.
✅ Работает даже после перезапуска устройства.

**Минусы**

❌ Не поддерживает частые обновления (раз в **15 минут** минимум).
❌ **Не подходит для GPS-трекинга в реальном времени**.


**🔹 3. AlarmManager + BroadcastReceiver**

Используется для запуска фонового трекинга **через интервалы времени** (например, каждые 30 минут).

🚀 **Лучший вариант для обновлений “раз в N минут/часов”**.

**📌 Как реализовать?**

1. Запускаем AlarmManager, который раз в N минут будит устройство.
2. BroadcastReceiver получает сигнал и обновляет местоположение.

```kotlin

class LocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            Log.d("LocationReceiver", "Location: ${location.latitude}, ${location.longitude}")
        }
    }
}

// Запускаем AlarmManager
val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
val intent = Intent(context, LocationReceiver::class.java)
val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

alarmManager.setRepeating(
    AlarmManager.ELAPSED_REALTIME_WAKEUP,
    SystemClock.elapsedRealtime() + 15 * 60 * 1000, // Каждые 15 минут
    15 * 60 * 1000,
    pendingIntent
)
```

> [!danger] Разрешения на локацию — главная боль современного Android
> • **Android 10 (API 29):** для фоновой локации нужно **отдельное** разрешение `ACCESS_BACKGROUND_LOCATION`, запрашивается отдельным шагом после foreground-разрешения.
> • **Android 11 (API 30):** background-разрешение нельзя запросить в том же диалоге — система ведёт пользователя в настройки («Разрешать всегда»).
> • **Android 12 (API 31):** появилась **приблизительная** локация (`ACCESS_COARSE_LOCATION`) — пользователь может дать только её, без `FINE`.
> • **Android 14 (API 34):** для Foreground Service с локацией обязателен `android:foregroundServiceType="location"` в манифесте **и** разрешение `FOREGROUND_SERVICE_LOCATION`.

> [!warning] `AlarmManager.setRepeating` ненадёжен и враждебен батарее
> • С API 19+ `setRepeating` стал **неточным** — система группирует будильники, реальный интервал «плавает».
> • В **Doze** (экран выключен, устройство неподвижно) такие будильники **откладываются** до окон обслуживания. Точные `setExactAndAllowWhileIdle` жёстко лимитированы (раз в ~9 минут на приложение).
> • Будить устройство каждые 15 минут ради локации — быстро сажает батарею и попадает под ограничения **App Standby Buckets**.
> Поэтому для периодических задач предпочтителен **WorkManager** (он сам учитывает Doze и ограничения), а для реального трекинга — **Foreground Service**. `AlarmManager` для геолокации сегодня почти не используют.

> [!warning] `PendingIntent` требует флаг мутабельности
> Начиная с **Android 12 (API 31)** при создании `PendingIntent` обязателен явный `FLAG_IMMUTABLE` или `FLAG_MUTABLE`, иначе — креш. В примере `FLAG_UPDATE_CURRENT` нужно дополнить: `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`.

Связано: [[Передача точной геолокации с интервалом времени]], [[Activity, Service,  Broadcast Receiver,  Content Provider]], [[Standby Bucket + Doze Mode]], [[Что такое Manifest]]