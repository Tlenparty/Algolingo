package com.tlenparty.algolingo.lesson

import com.tlenparty.algolingo.FakeProgressRepository
import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.LessonRepository
import com.tlenparty.algolingo.lesson.domain.LoadLessonUseCase
import com.tlenparty.algolingo.lesson.domain.Solution
import com.tlenparty.algolingo.lesson.domain.TheoryCard
import com.tlenparty.algolingo.lesson.presentation.LessonUiState
import com.tlenparty.algolingo.lesson.presentation.LessonViewModel
import com.tlenparty.algolingo.progress.domain.RecordLessonResultUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {
    private val progress = FakeProgressRepository()
    private val record = RecordLessonResultUseCase(progress)

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun reviewOfLessonWithoutExercisesStillRecords() {
        val lesson = Lesson(id = "t", title = "t", statement = "s", theory = listOf(TheoryCard("c")), exercises = emptyList(), solution = Solution("x"))
        LessonViewModel("t", 1, review = true, loadLesson = LoadLessonUseCase { _, _ -> Result.success(lesson) }, recordResult = record)
        assertEquals(1, progress.writes)
    }

    @Test
    fun loadsWithLangGivenAtCreation() {
        val calls = mutableListOf<Pair<String, CodeLang>>()
        val repo = LessonRepository { id, lang -> calls += id to lang; Result.failure(Exception("x")) }
        LessonViewModel("l1.4", 1, lang = CodeLang.Python, loadLesson = LoadLessonUseCase(repo), recordResult = record)
        LessonViewModel("l1.4", 1, loadLesson = LoadLessonUseCase(repo), recordResult = record)
        assertEquals(listOf("l1.4" to CodeLang.Python, "l1.4" to CodeLang.Kotlin), calls)
    }

    @Test
    fun failedLoadShowsError() {
        val vm = LessonViewModel("x", 1, loadLesson = LoadLessonUseCase { _, _ -> Result.failure(Exception("x")) }, recordResult = record)
        assertIs<LessonUiState.Error>(vm.state.value)
        assertEquals(0, progress.writes)
    }
}
