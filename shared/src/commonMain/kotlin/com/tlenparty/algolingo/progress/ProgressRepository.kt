package com.tlenparty.algolingo.progress

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import com.tlenparty.algolingo.lesson.LessonResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.time.Clock

object ProgressSerializer : OkioSerializer<Progress> {
    override val defaultValue = Progress()
    override suspend fun readFrom(source: BufferedSource): Progress =
        decodeProgress(source.readUtf8()) ?: throw CorruptionException("progress.json unreadable")
    override suspend fun writeTo(t: Progress, sink: BufferedSink) {
        sink.writeUtf8(encodeProgress(t))
    }
}

/** [path] must be absolute (OkioStorage checks it). A corrupt file is reset to zero progress. */
fun progressStore(
    path: String,
    // ponytail: Default is fine for a few-KB file; Dispatchers.IO if the file ever grows.
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
): DataStore<Progress> = DataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, ProgressSerializer) { path.toPath() },
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
    /** An IO error shows zeros instead of crashing Home. */
    val progress: Flow<Progress> = store.data.catch { emit(Progress()) }

    /** today() is read inside the transform: the day of the write, not of the lesson start. */
    fun record(id: String, r: LessonResult): Job =
        scope.launch { store.updateData { applyLessonResult(it, id, r, today()) } }
}

/** One DataStore per process (two on one file crash), surviving Activity recreation. */
object AppGraph {
    // ponytail: main-thread only, no locking; App() is the sole caller.
    private var progress: ProgressRepository? = null

    fun progress(dataDir: String): ProgressRepository =
        progress ?: ProgressRepository(progressStore("$dataDir/progress.json")).also { progress = it }
}
