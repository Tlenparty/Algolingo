package com.tlenparty.algolingo.progress

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.tlenparty.algolingo.di.AppGraph
import com.tlenparty.algolingo.lesson.domain.ExerciseResult
import com.tlenparty.algolingo.lesson.domain.Grade
import com.tlenparty.algolingo.lesson.domain.LessonResult
import com.tlenparty.algolingo.progress.data.ProgressRepositoryImpl
import com.tlenparty.algolingo.progress.domain.ProgressRepository
import com.tlenparty.algolingo.progress.domain.RecordLessonResultUseCase
import com.tlenparty.algolingo.progress.data.ProgressSerializer
import com.tlenparty.algolingo.progress.data.decodeProgress
import com.tlenparty.algolingo.progress.data.encodeProgress
import com.tlenparty.algolingo.progress.data.progressStore
import com.tlenparty.algolingo.progress.domain.LessonProgress
import com.tlenparty.algolingo.progress.domain.Progress

private class FakeStore : DataStore<Progress> {
    val state = MutableStateFlow(Progress())
    override val data: Flow<Progress> = state
    override suspend fun updateData(transform: suspend (Progress) -> Progress): Progress =
        transform(state.value).also { state.value = it }
}

class ProgressRepositoryTest {
    private val d = LocalDate(2026, 10, 22)
    private val clean = LessonResult(
        Grade.Clean,
        listOf(ExerciseResult("e1", true, 1, 0), ExerciseResult("e2", true, 1, 0)),
    )

    @Test
    fun recordWritesXpStreakAndSrs() = runTest {
        val store = FakeStore()
        RecordLessonResultUseCase(ProgressRepositoryImpl(store, { d }, backgroundScope))("l0.4", clean).join()
        val p = ProgressRepositoryImpl(store, { d }, backgroundScope).progress.filterNotNull().first()
        assertEquals(20, p.xp)
        assertEquals(1, p.streak)
        assertEquals(LessonProgress(0, LocalDate(2026, 10, 23)), p.lessons["l0.4"])
    }

    @Test
    fun todayIsReadAtWriteTime() = runTest {
        var day = d
        val store = FakeStore()
        val repo = ProgressRepositoryImpl(store, { day }, backgroundScope)
        day = LocalDate(2026, 10, 23)
        RecordLessonResultUseCase(repo)("l0.4", clean).join()
        assertEquals(LocalDate(2026, 10, 23), store.state.value.lastActiveDay)
    }

    @Test
    fun twoRecordsSameDaySumXpKeepStreak() = runTest {
        val store = FakeStore()
        val repo = ProgressRepositoryImpl(store, { d }, backgroundScope)
        RecordLessonResultUseCase(repo)("l0.4", clean).join()
        RecordLessonResultUseCase(repo)("l0.4", clean).join()
        assertEquals(40, store.state.value.xp)
        assertEquals(1, store.state.value.streak)
    }

    @Test
    fun failedWriteIsRetriedThenFlagged() = runTest {
        var calls = 0
        val failing = object : DataStore<Progress> {
            override val data: Flow<Progress> = MutableStateFlow(Progress())
            override suspend fun updateData(transform: suspend (Progress) -> Progress): Progress {
                calls++
                throw okio.IOException("disk full")
            }
        }
        val repo = ProgressRepositoryImpl(failing, { d }, backgroundScope)
        RecordLessonResultUseCase(repo)("l0.4", clean).join()
        assertEquals(2, calls)
        assertTrue(repo.saveFailed.value)
    }

    @Test
    fun setLangPersists() = runTest {
        val store = FakeStore()
        ProgressRepositoryImpl(store, { d }, backgroundScope).setLang(CodeLang.Python).join()
        val p = ProgressRepositoryImpl(store, { d }, backgroundScope).progress.filterNotNull().first()
        assertEquals(CodeLang.Python, p.lang)
    }

    @Test
    fun setLangFailedWriteIsRetriedThenFlagged() = runTest {
        var calls = 0
        val failing = object : DataStore<Progress> {
            override val data: Flow<Progress> = MutableStateFlow(Progress())
            override suspend fun updateData(transform: suspend (Progress) -> Progress): Progress {
                calls++
                throw okio.IOException("disk full")
            }
        }
        val repo = ProgressRepositoryImpl(failing, { d }, backgroundScope)
        repo.setLang(CodeLang.Python).join()
        assertEquals(2, calls)
        assertTrue(repo.saveFailed.value)
    }

    @Test
    fun serializerRejectsGarbage() = runTest {
        assertFailsWith<CorruptionException> { ProgressSerializer().readFrom(Buffer().writeUtf8("garbage")) }
    }

    @Test
    fun serializerRoundTrips() = runTest {
        val p = Progress(xp = 7, streak = 2, lastActiveDay = d, lessons = mapOf("l0.4" to LessonProgress(1, d)))
        assertEquals(p, ProgressSerializer().readFrom(Buffer().writeUtf8(encodeProgress(p))))
        val buf = Buffer()
        ProgressSerializer().writeTo(p, buf)
        assertEquals(p, ProgressSerializer().readFrom(buf))
    }

    @Test
    fun missingSchemaVersionDecodesAsV1() =
        assertEquals(Progress(xp = 5), decodeProgress("""{"xp":5}"""))

    @Test
    fun appGraphReusesRepositoryWhenDirReturns() {
        val a = tempPath().toString()
        val b = tempPath().toString()
        val repoA = AppGraph.progress(a)
        assertTrue(repoA !== AppGraph.progress(b))
        assertTrue(repoA === AppGraph.progress(a)) // A -> B -> A: one DataStore per file
    }

    private fun tempPath() = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "progress-${Random.nextLong().toULong()}.json"

    @Test
    fun realFileSurvivesRestart() = runTest {
        val path = tempPath()
        try {
            val scopeA = CoroutineScope(Dispatchers.Default + SupervisorJob())
            progressStore(path.toString(), scopeA).updateData { it.copy(xp = 30) }
            scopeA.coroutineContext.job.cancelAndJoin()
            val scopeB = CoroutineScope(Dispatchers.Default + SupervisorJob())
            assertEquals(30, progressStore(path.toString(), scopeB).data.first().xp)
            scopeB.coroutineContext.job.cancelAndJoin()
        } finally {
            FileSystem.SYSTEM.delete(path)
        }
    }

    @Test
    fun corruptRealFileResetsToZero() = runTest {
        val path = tempPath()
        try {
            FileSystem.SYSTEM.write(path) { writeUtf8("garbage") }
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            assertEquals(Progress(), progressStore(path.toString(), scope).data.first())
            scope.coroutineContext.job.cancelAndJoin()
            assertEquals("garbage", FileSystem.SYSTEM.read("$path.bak".toPath()) { readUtf8() })
        } finally {
            FileSystem.SYSTEM.delete(path)
            FileSystem.SYSTEM.delete("$path.bak".toPath())
        }
    }
}
