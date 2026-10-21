package com.tlenparty.algolingo.lesson

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelTest {
    private fun ex(id: String, segs: List<String>, holes: List<Hole>, bank: List<Chip>) =
        Exercise(id, segs, holes, bank)

    private fun lessonOf(vararg ex: Exercise, code: String, id: String = "l", schema: Int = 1) = Lesson(
        schema = schema, id = id, title = "t", statement = "s", exercises = ex.toList(),
        solution = Solution(code, "O(1)", "O(1)"),
    )

    // helper: 1-hole and 2-hole exercises reproducing the same code "a(X)b(Y)c"
    private val same = "a(X)b(Y)c"
    private fun x1() = ex("l.e1", listOf("a(X)b(", ")c"), listOf(Hole(listOf("Y"))), listOf(Chip("Y"), Chip("Z", "w")))
    private fun x2() = ex(
        "l.e2", listOf("a(", ")b(", ")c"), listOf(Hole(listOf("X")), Hole(listOf("Y"))),
        listOf(Chip("X"), Chip("Y"), Chip("Z", "w")),
    )
    private fun good() = lessonOf(x1(), x2(), code = same)

    private fun assertErr(l: Lesson, part: String) {
        val errs = validate(l)
        assertTrue(errs.any { part in it }, "expected '$part' in $errs")
    }

    @Test fun validLessonHasNoErrors() = assertEquals(emptyList(), validate(good()))

    @Test fun segmentHoleMismatch() {
        val bad = x2().copy(segments = listOf("a(", ")b("))
        assertErr(lessonOf(x1(), bad, code = same), "l.e2: segments")
    }

    @Test fun emptyOk() {
        val bad = x2().copy(holes = listOf(Hole(emptyList()), Hole(listOf("Y"))))
        assertErr(lessonOf(x1(), bad, code = same), "empty ok")
    }

    @Test fun textMissingFromBank() {
        val bad = x2().copy(bank = listOf(Chip("X"), Chip("Z", "w")))
        assertErr(lessonOf(x1(), bad, code = same), "missing from bank")
        val bad2 = x2().copy(holes = listOf(Hole(listOf("X"), listOf(Accepted("Q", "n"))), Hole(listOf("Y"))))
        assertErr(lessonOf(x1(), bad2, code = same), "'Q' missing")
    }

    @Test fun assignability() {
        val twice = ex(
            "l.e2", listOf("a(", ")b(", ")c"), listOf(Hole(listOf("X")), Hole(listOf("X"))),
            listOf(Chip("X"), Chip("Z", "w")),
        )
        val code = "a(X)b(X)c"
        assertErr(lessonOf(x1().copy(segments = listOf("a(X)b(", ")c"), holes = listOf(Hole(listOf("X")))), twice, code = code), "needed 2 times")
        val dup = twice.copy(bank = listOf(Chip("X"), Chip("X"), Chip("Z", "w")))
        val e1x = ex("l.e1", listOf("a(X)b(", ")c"), listOf(Hole(listOf("X"))), listOf(Chip("X"), Chip("Z", "w")))
        assertEquals(emptyList(), validate(lessonOf(e1x, dup, code = code)))
    }

    @Test fun distractorNeedsWhy() {
        val bad = x2().copy(bank = listOf(Chip("X"), Chip("Y"), Chip("Z")))
        assertErr(lessonOf(x1(), bad, code = same), "distractor 'Z'")
        val viaWrongWhy = bad.copy(holes = listOf(Hole(listOf("X"), wrongWhy = mapOf("Z" to "w")), Hole(listOf("Y"))))
        assertEquals(emptyList(), validate(lessonOf(x1(), viaWrongWhy, code = same)))
    }

    @Test fun bankTooBig() {
        val bad = x2().copy(bank = x2().bank + List(8) { Chip("D$it", "w") })
        assertErr(lessonOf(x1(), bad, code = same), "max 10")
    }

    @Test fun referenceDrift() = assertErr(lessonOf(x1(), x2(), code = "other"), "differ from solution.code")

    @Test fun ladderAndIds() {
        assertErr(lessonOf(x2(), x2().copy(id = "l.e3"), code = same), "strictly increasing")
        assertErr(lessonOf(x1(), code = same), "expected 2..3")
        assertErr(lessonOf(x1(), x2().copy(id = "zzz"), code = same), "not prefixed")
        assertErr(lessonOf(x1(), x1(), code = same), "duplicate")
        assertErr(lessonOf(x1(), x2(), code = same, schema = 2), "schema")
    }

    @Test fun acceptableAlsoInOk() {
        val bad = x2().copy(holes = listOf(Hole(listOf("X"), listOf(Accepted("X", "n"))), Hole(listOf("Y"))))
        assertErr(lessonOf(x1(), bad, code = same), "also in ok")
    }

    @Test fun neverThrowsOnEmpty() {
        val empty = Lesson(id = "l", title = "", statement = "", exercises = emptyList(), solution = Solution("", "", ""))
        assertTrue(validate(empty).isNotEmpty())
        val hollow = Exercise("l.e1", emptyList(), emptyList(), emptyList())
        validate(lessonOf(hollow, hollow.copy(id = "l.e2"), code = ""))
    }

    @Test fun inlineJsonDecodes() {
        val s = """{"id":"l.e1","segments":["a(",")"],"holes":[{"ok":["X"]}],"bank":[{"text":"X"},{"text":"Z","why":"w"}]}"""
        val e = Json { ignoreUnknownKeys = true }.decodeFromString<Exercise>(s)
        assertEquals(1, e.holes.size)
        assertEquals("w", e.bank[1].why)
    }
}
