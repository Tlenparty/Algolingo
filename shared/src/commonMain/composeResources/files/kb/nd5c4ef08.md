
# Приоритеты процессов

Процессы делятся по приоритету на 4 вида, в порядке убывания:  
  
_1. Видимый активный процесс_ (Foreground process). В этом процессе работает приложение, с которым взаимодействует пользователь. Процесс находится в этом состоянии в следующих случаях:  
- `Activity`, у которой вызван `onResume()`, но еще не вызван `onPause()`  
- Во время выполнения `BroadcastReceiver.onReceive()`  
- Запущен `Service` и выполняется один из колбэков: `onCreate()`, `onStart()`, `onDestroy()`  
  
_2. Видимый процесс_. Пользователь знает о приложении, запущенном в этом процессе, но не взаимодействует с ним:  
- `Activity` у которой вызван `onPause()`, но еще не вызван `onStop()`.  
- `Service`, который запущен через [Service.startForeground()](https://developer.android.com/reference/android/app/Service.html#startForeground%28int,%20android.app.Notification%29). В этом случае пользователь видит нотификацию, связанную с сервисом.  
- `Service`, выполняющий функцию, о которой пользователь осведомлен. Например input method service.  
  
_3. Background Service_. Процесс с сервисом, запущенным командой [startService()](https://developer.android.com/reference/android/content/Context.html#startService%28android.content.Intent%29).  
  
_4. Закэшированный процесс_. К ним относятся пустые процессы, используемые при старте приложений, и процессы, в которых все активити в `onStop()` состоянии.  
  
> [!note] Канонически выделяют 5 уровней
> Часто перечисляют так (от высшего к низшему): **Foreground → Visible → Service → Cached/Background → Empty**. Здесь Service вынесен отдельно, а Cached и Empty объединены. Суть одна: чем выше приоритет, тем позже процесс убьют при нехватке памяти. Подробнее — [[Процессы и потоки]].
>
> Связано: [[Процессы и потоки]], [[Activity, Service,  Broadcast Receiver,  Content Provider]], [[Standby Bucket + Doze Mode]]

Система убивает процессы в порядке возрастания их приоритета для восстановления ресурсов. [Подробнее](https://developer.android.com/guide/components/activities/process-lifecycle)