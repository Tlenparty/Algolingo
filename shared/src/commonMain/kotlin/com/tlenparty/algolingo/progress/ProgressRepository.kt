package com.tlenparty.algolingo.progress

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.tlenparty.algolingo.design.CodeLang
import com.tlenparty.algolingo.lesson.LessonResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.time.Clock

/** An unreadable file is copied to [backup] (if set) before the corruption handler resets it, so progress can still be recovered by hand. */
class ProgressSerializer(private val backup: Path? = null) : OkioSerializer<Progress> {
    override val defaultValue = Progress()
    override suspend fun readFrom(source: BufferedSource): Progress {
        val raw = source.readUtf8()
        return decodeProgress(raw) ?: run {
            backup?.let { FileSystem.SYSTEM.write(it) { writeUtf8(raw) } }
            throw CorruptionException("progress.json unreadable")
        }
    }
    override suspend fun writeTo(t: Progress, sink: BufferedSink) {
        sink.writeUtf8(encodeProgress(t))
    }
}

/** [path] must be absolute (OkioStorage checks it). A corrupt file is saved as `<path>.bak`, then reset to zero progress. */
fun progressStore(
    path: String,
    // ponytail: Default is fine for a few-KB file; Dispatchers.IO if the file ever grows.
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
): DataStore<Progress> = DataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, ProgressSerializer("$path.bak".toPath())) { path.toPath() },
    corruptionHandler = ReplaceFileCorruptionHandler { Progress() },
    scope = scope,
)

fun localToday(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** Writes run in this app-lifetime scope, never a ViewModel scope: leaving the screen must not cancel them. */
class ProgressRepository(
    private val store: DataStore<Progress>,
    val today: () -> LocalDate = ::localToday,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> println("progress write failed: $e") },
    ),
) {
    /** Hot and app-scoped, so Home has the value on its first frame after the first load. null = not loaded yet. An IO error shows zeros instead of crashing Home. */
    val progress: StateFlow<Progress?> = store.data.catch { emit(Progress()) }.stateIn(scope, SharingStarted.Eagerly, null)

    private val _saveFailed = MutableStateFlow(false)

    /** True once a write failed even after a retry, so Home can say the lesson was not saved. The next good write clears it. */
    val saveFailed: StateFlow<Boolean> = _saveFailed

    /** today() is read inside the transform: the day of the write, not of the lesson start. */
    fun record(id: String, r: LessonResult): Job = write { applyLessonResult(it, id, r, today()) }

    fun setLang(lang: CodeLang): Job = write { it.copy(lang = lang) }

    /** One retry, then [saveFailed]. */
    private fun write(transform: (Progress) -> Progress): Job = scope.launch {
        repeat(2) { attempt ->
            try {
                store.updateData { transform(it) }
                _saveFailed.value = false
                return@launch
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt == 1) _saveFailed.value = true
            }
        }
    }
}

/** One DataStore per process (two on one file crash), surviving Activity recreation. The first dataDir wins; later ones are ignored. */
object AppGraph {
    // ponytail: main-thread only, no locking; App() is the sole caller.
    private var progress: ProgressRepository? = null

    fun progress(dataDir: String): ProgressRepository =
        progress ?: ProgressRepository(progressStore("$dataDir/progress.json")).also { progress = it }
}
