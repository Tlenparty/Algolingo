package com.tlenparty.algolingo.lesson

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonRepositoryTest {
    private fun ex(id: String, holes: Int) = Exercise(
        id = id,
        segments = if (holes == 1) listOf("", "a") else List(holes + 1) { "" }, // both rebuild to "aa"
        holes = List(holes) { Hole(ok = listOf("a")) },
        bank = List(holes) { Chip("a") },
    )

    private fun lesson(e1: Exercise = ex("x.1", 1)) = Lesson(
        id = "x", title = "t", statement = "s",
        exercises = listOf(e1, ex("x.2", 2)),
        solution = Solution("aa", "O(1)", "O(1)"),
    )

    @Test fun garbage() = assertTrue(parseLesson("not json").isFailure)

    @Test fun emptyObject() = assertTrue(parseLesson("{}").isFailure)

    @Test fun validateFailure() {
        val bad = lesson(ex("x.1", 1).copy(segments = listOf("only")))
        val r = parseLesson(Json.encodeToString(bad))
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("need holes+1"))
    }

    @Test fun validRoundtripIgnoresUnknownKeys() {
        val l = lesson()
        val text = Json.encodeToString(l).replaceFirst("{", "{\"extra\":1,")
        assertEquals(l, parseLesson(text).getOrThrow())
    }
}
