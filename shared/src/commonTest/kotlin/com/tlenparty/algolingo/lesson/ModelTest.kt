package com.tlenparty.algolingo.lesson

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.tlenparty.algolingo.lesson.domain.Accepted
import com.tlenparty.algolingo.lesson.domain.Chip
import com.tlenparty.algolingo.lesson.domain.Exercise
import com.tlenparty.algolingo.lesson.domain.Hole
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.LessonKind
import com.tlenparty.algolingo.lesson.domain.MAX_DESIGN_LINE
import com.tlenparty.algolingo.lesson.domain.Solution
import com.tlenparty.algolingo.lesson.domain.validate
import com.tlenparty.algolingo.lesson.data.ExerciseDto
import com.tlenparty.algolingo.lesson.data.LessonDto
import com.tlenparty.algolingo.lesson.data.toDomain

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
        assertErr(lessonOf(x1(), x2().copy(bank = listOf(Chip("X"), Chip("Y"), Chip("Z", " "))), code = same), "distractor 'Z'")
        // wrongWhy in one hole only: Z dropped into hole 1 would have no explanation
        val oneHole = bad.copy(holes = listOf(Hole(listOf("X"), wrongWhy = mapOf("Z" to "w")), Hole(listOf("Y"))))
        assertErr(lessonOf(x1(), oneHole, code = same), "distractor 'Z' has no why for hole 1")
        val everyHole = bad.copy(holes = listOf(Hole(listOf("X"), wrongWhy = mapOf("Z" to "w")), Hole(listOf("Y"), wrongWhy = mapOf("Z" to "w"))))
        val e1 = x1().copy(holes = listOf(Hole(listOf("Y"), wrongWhy = mapOf("Z" to "w"))), bank = listOf(Chip("Y"), Chip("Z")))
        assertEquals(emptyList(), validate(lessonOf(e1, everyHole, code = same)))
    }

    @Test fun wrongWhyKeys() {
        fun withWhy(vararg w: Pair<String, String>) =
            lessonOf(x1(), x2().copy(holes = listOf(Hole(listOf("X"), wrongWhy = mapOf(*w)), Hole(listOf("Y")))), code = same)
        assertEquals(emptyList(), validate(withWhy("Y" to "w")))
        assertErr(withWhy("Q" to "w"), "wrongWhy key 'Q' not in bank")
        assertErr(withWhy("X" to "w"), "wrongWhy key 'X' is a correct answer")
        assertErr(withWhy("Y" to ""), "wrongWhy for 'Y' is blank")
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
        val e = Json { ignoreUnknownKeys = true }.decodeFromString<ExerciseDto>(s).toDomain()
        assertEquals(1, e.holes.size)
        assertEquals("w", e.bank[1].why)
    }

    // --- design lessons ---
    private fun d(n: Int, code: String) = ex("l.e$n", listOf("", ""), listOf(Hole(listOf(code))), listOf(Chip(code), Chip("Z", "w")))
    private fun design(vararg ex: Exercise, code: String) = lessonOf(*ex, code = code).copy(kind = LessonKind.Design)

    @Test fun designStepRange() {
        assertEquals(emptyList(), validate(design(d(1, "a"), d(2, "b"), code = "b")))
        assertEquals(emptyList(), validate(design(d(1, "a"), d(2, "b"), d(3, "c"), d(4, "d"), code = "d")))
        assertErr(design(d(1, "a"), d(2, "b"), d(3, "c"), d(4, "d"), d(5, "e"), code = "e"), "expected 2..4")
        assertErr(design(d(1, "a"), code = "a"), "expected 2..4")
        val code4 = lessonOf(d(1, "a"), d(2, "a"), d(3, "a"), d(4, "a"), code = "a")
        assertErr(code4, "expected 2..3")
    }

    @Test fun designSkipsHoleGrowth() {
        val l = design(d(1, "a"), d(2, "b"), code = "b")
        assertTrue(validate(l).none { "strictly increasing" in it })
        assertErr(lessonOf(d(1, "a"), d(2, "a"), code = "a"), "strictly increasing")
    }

    @Test fun designOnlyLastStepMatchesSolution() {
        assertEquals(emptyList(), validate(design(d(1, "a"), d(2, "b"), code = "b")))
        assertErr(design(d(1, "a"), d(2, "b"), code = "a"), "l.e2: segments + ok[0] differ")
        assertErr(lessonOf(d(1, "a"), d(2, "b"), code = "b"), "l.e1: segments + ok[0] differ")
    }

    @Test fun designLineLength() {
        val ok = "x".repeat(MAX_DESIGN_LINE)
        val long = "y".repeat(MAX_DESIGN_LINE + 1)
        assertEquals(emptyList(), validate(design(d(1, "a\n$ok"), d(2, ok), code = ok)))
        assertErr(design(d(1, "a\n$long"), d(2, ok), code = ok), "l.e1: line 2 has ${MAX_DESIGN_LINE + 1} chars (max $MAX_DESIGN_LINE)")
        assertTrue(validate(lessonOf(d(1, long), d(2, long).copy(segments = listOf("", "", ""), holes = listOf(Hole(listOf("q")), Hole(listOf(long))), bank = listOf(Chip("q"), Chip(long))), code = long))
            .let { errs -> errs.none { "chars" in it } })
    }

    @Test fun defaultsAndKindDecode() {
        val j = Json { ignoreUnknownKeys = true }
        val base = """"id":"x","title":"t","statement":"s","exercises":[],"solution":{"code":"c"}"""
        val l = j.decodeFromString<LessonDto>("{$base}").toDomain()
        assertEquals(LessonKind.Code, l.kind)
        assertEquals("", l.solution.time)
        assertEquals("", l.solution.space)
        assertEquals(LessonKind.Design, j.decodeFromString<LessonDto>("{\"kind\":\"design\",$base}").toDomain().kind)
        val e = j.decodeFromString<ExerciseDto>("""{"id":"e","segments":[""],"holes":[],"bank":[]}""").toDomain()
        assertEquals(null, e.title)
    }
}
