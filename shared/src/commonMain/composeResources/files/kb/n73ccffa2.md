**ExoPlayer** — это мощный и гибкий медиаплеер от Google, который позволяет воспроизводить аудио и видео в Android-приложениях. Он является более продвинутой альтернативой MediaPlayer и VideoView и поддерживает адаптивное потоковое воспроизведение.

**1. Архитектура ExoPlayer**

ExoPlayer состоит из нескольких ключевых компонентов:

1. ExoPlayer – главный класс, управляющий воспроизведением.
2. MediaItem – объект, описывающий мультимедийный контент (URL, заголовки, DRM).
3. MediaSource – источник медиаконтента (локальный файл, HLS, DASH).
4. TrackSelector – выбор подходящей дорожки (например, видео с нужным разрешением или аудиотрек).
5. LoadControl – управление буферизацией.
6. Renderers – обработка видео, аудио и субтитров.
7. PlayerView – UI-элемент для отображения видео.

**2. Как использовать ExoPlayer?**

**Шаг 1: Добавить зависимости**

В build.gradle (Module) добавляем:
```kotlin
dependencies {
    implementation 'androidx.media3:media3-exoplayer:1.2.0'
    implementation 'androidx.media3:media3-ui:1.2.0'
}
```

**Шаг 2: Добавить PlayerView в XML**
```kotlin
<androidx.media3.ui.PlayerView
    android:id="@+id/playerView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:show_controller="true"/>
```

**Шаг 3: Создать и настроить ExoPlayer**
```kotlin
class VideoPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.playerView)

        // Создаем и настраиваем ExoPlayer
        player = ExoPlayer.Builder(this).build().apply {
            playerView.player = this // Привязываем плеер к PlayerView

            // Создаем источник медиа (локальный файл или ссылка)
            val mediaItem = MediaItem.fromUri("https://www.example.com/video.mp4")
            setMediaItem(mediaItem)

            prepare()  // Подготовка плеера
            playWhenReady = true  // Автоматический старт
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()  // Останавливаем при уходе с экрана
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()  // Освобождаем ресурсы
        player = null
    }
}
```

**3. Продвинутые возможности ExoPlayer**

**1️⃣ Кеширование видео**

Если вам нужно кешировать видео, например, для оффлайн-проигрывания или экономии трафика, можно использовать SimpleCache:
```kotlin
val cache = SimpleCache(File(context.cacheDir, "media"), LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024))
val dataSourceFactory = CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())

val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
    .createMediaSource(MediaItem.fromUri(videoUri))

```

**2️⃣ Потоковое воспроизведение (HLS, DASH)**

ExoPlayer поддерживает HTTP Live Streaming (HLS):
```kotlin
val mediaItem = MediaItem.Builder()
    .setUri("https://www.example.com/stream.m3u8") // HLS поток
    .setMimeType(MimeTypes.APPLICATION_M3U8)
    .build()
player?.setMediaItem(mediaItem)
player?.prepare()
```

**3️⃣ Включение кнопок управления**

По умолчанию PlayerView скрывает кнопки управления. Можно включить их явно

```kotlin
<androidx.media3.ui.PlayerView
    android:id="@+id/playerView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:use_controller="true"/>
```

**4️⃣ Обработка событий воспроизведения**

Можно слушать события плеера, например, когда видео началось, остановилось или произошла ошибка:
```kotlin
player?.addListener(object : Player.Listener {
    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_BUFFERING -> Log.d("ExoPlayer", "Buffering...")
            Player.STATE_READY -> Log.d("ExoPlayer", "Ready to play")
            Player.STATE_ENDED -> Log.d("ExoPlayer", "Playback ended")
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e("ExoPlayer", "Error: ${error.message}")
    }
})
```

> [!important] ExoPlayer переехал в Media3
> Старый артефакт `com.google.android.exoplayer2.*` **больше не развивается** — актуальная версия живёт в **`androidx.media3`** (`androidx.media3.exoplayer.ExoPlayer`, `androidx.media3.ui.PlayerView`). В примерах выше зависимости уже media3, поэтому и пакеты в коде/XML должны быть `androidx.media3.*` (исправлено). Не смешивай два пакета — это частая ошибка при миграции.

> [!tip] Что важно про ExoPlayer
> - **Жизненный цикл:** обязательно `player.release()` в `onStop`/`onDestroy` (или в `DisposableEffect` в Compose) — иначе утечка и удержание аудио-фокуса. Для современного подхода — привязка к Lifecycle.
> - **Зачем вместо `MediaPlayer`:** адаптивный стриминг (**HLS/DASH/SmoothStreaming**), DRM, кэширование, гибкая архитектура (`MediaSource`/`TrackSelector`), кастомизация.
> - **Фоновое воспроизведение** — через `MediaSessionService` + `MediaSession` (media3), а не просто держать плеер в Activity.

Связано: [[Choreographer, SurfaceFlinger]], [[Custom view]]



