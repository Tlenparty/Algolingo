package com.tlenparty.algolingo.lesson

import java.io.File
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonContentTest {
    private val file = File("src/commonMain/composeResources/files/content/lessons/l0.4.json")

    // strict: a typo in hand-written JSON must fail, not be ignored
    private val lesson = Json { ignoreUnknownKeys = false }.decodeFromString<Lesson>(file.readText())

    @Test
    fun l04IsValid() {
        assertEquals(emptyList(), validate(lesson))
        assertTrue(lesson.exercises.size in 2..3)
        assertEquals(listOf(1, 2, 4), lesson.exercises.map { it.holes.size })
    }

    @Test
    fun e3HasTwoSeparateOneChips() {
        assertEquals(2, lesson.exercises.last().bank.count { it.text == "1" })
    }
}
