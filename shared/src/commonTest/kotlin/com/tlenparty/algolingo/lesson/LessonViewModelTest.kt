package com.tlenparty.algolingo.lesson

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
            LessonViewModel("t", 1, review = true, onFinish = { results += it }, load = { Result.success(lesson) })
            assertEquals(1, results.size)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
