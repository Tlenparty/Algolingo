package com.tlenparty.algolingo.lesson

import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {
    @Test
    fun reviewOfLessonWithoutExercisesStillRecords() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val lesson = Lesson(id = "t", title = "t", statement = "s", theory = listOf(TheoryCard("c")), exercises = emptyList(), solution = Solution("", "", ""))
            val results = mutableListOf<LessonResult>()
            LessonViewModel("t", 1, review = true, onFinish = { results += it }, load = { _, _ -> Result.success(lesson) })
            assertEquals(1, results.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadsWithLangGivenAtCreation() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val calls = mutableListOf<Pair<String, CodeLang>>()
            LessonViewModel("l1.4", 1, lang = CodeLang.Python, load = { id, lang -> calls += id to lang; Result.failure(Exception("x")) })
            LessonViewModel("l1.4", 1, load = { id, lang -> calls += id to lang; Result.failure(Exception("x")) })
            assertEquals(listOf("l1.4" to CodeLang.Python, "l1.4" to CodeLang.Kotlin), calls)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
