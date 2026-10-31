package com.tlenparty.algolingo.lesson

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.LoadLessonUseCase
import com.tlenparty.algolingo.lesson.domain.Solution
import com.tlenparty.algolingo.lesson.domain.TheoryCard
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadLessonUseCaseTest {
    private val lesson = Lesson(id = "t", title = "t", statement = "s", theory = listOf(TheoryCard("c")), exercises = emptyList(), solution = Solution("x"))
    private val load = LoadLessonUseCase { _, _ -> Result.success(lesson) }

    @Test fun normalRunKeepsTheory() = runTest {
        assertEquals(1, load("t", CodeLang.Kotlin, review = false).getOrThrow().theory.size)
    }

    @Test fun reviewSkipsTheory() = runTest {
        assertEquals(0, load("t", CodeLang.Kotlin, review = true).getOrThrow().theory.size)
    }
}
