package com.tlenparty.algolingo.lesson

import com.tlenparty.algolingo.lesson.LessonAction.*
import kotlin.test.*

class ReducerTest {
    // Each exercise: one hole, ok "r", acceptable "ra", bank 0 "r", 1 "x", 2 "ra", 3 "y", 4 "z".
    private fun ex(id: String, hints: List<String> = emptyList()) = Exercise(
        id = id,
        segments = listOf("a", "b"),
        holes = listOf(Hole(ok = listOf("r"), acceptable = listOf(Accepted("ra", "n")))),
        bank = listOf(Chip("r"), Chip("x", "w"), Chip("ra"), Chip("y", "w"), Chip("z", "w")),
        hints = hints,
    )

    private fun lessonOf(vararg ids: String, theory: Int = 0, hints: List<String> = emptyList()) = Lesson(
        id = "t", title = "t", statement = "s",
        theory = List(theory) { TheoryCard("c$it") },
        exercises = ids.map { ex(it, hints) },
        solution = Solution("", "", ""),
    )

    private fun run(s: LessonState, vararg a: LessonAction) = a.fold(s) { st, x -> LessonReducer.reduce(st, x) }
    private fun fill(s: LessonState, chip: Int) = run(s, TapChip(chip), Check)
    private fun failCurrent(s: LessonState) = run(fill(s, 1), Continue)
    private fun solveCurrent(s: LessonState, chip: Int = 0) = run(fill(s, chip), Continue, Continue)

    @Test fun theoryFlow() {
        var s = LessonReducer.start(lessonOf("a", theory = 2), 1)
        assertEquals(Phase.Theory(0), s.phase)
        s = run(s, NextTheory); assertEquals(Phase.Theory(1), s.phase)
        s = run(s, NextTheory); assertEquals(Phase.Exercise, s.phase)
        assertEquals(Phase.Exercise, LessonReducer.start(lessonOf("a"), 1).phase)
        assertNotNull(LessonReducer.start(lessonOf("a"), 1).current)
    }

    @Test fun theoryWithoutExercisesEndsInDone() {
        val s = LessonReducer.start(lessonOf(theory = 1), 1)
        assertEquals(Phase.Theory(0), s.phase)
        assertEquals(Phase.Done, run(s, NextTheory).phase)
    }

    @Test fun wrongThreeTimesFails() {
        var s = LessonReducer.start(lessonOf("a", "b"), 1)
        s = failCurrent(s)
        assertEquals(listOf("b", "a"), s.queue)
        s = solveCurrent(s) // b
        assertEquals(listOf("a"), s.queue)
        s = failCurrent(s)
        assertEquals(Phase.Exercise, s.phase)
        s = run(fill(s, 1))
        assertEquals(3, s.attempts["a"])
        s = run(s, Continue)
        assertEquals(Phase.Reveal(false), s.phase)
        // a 4th attempt is impossible: Check/TapChip do nothing in Reveal
        assertEquals(s, run(s, TapChip(0), Check))
        s = run(s, Continue)
        assertEquals(Phase.Done, s.phase)
        assertNull(s.current)
        assertEquals(3, s.attempts["a"])
        assertEquals(Grade.Failed, LessonReducer.result(s).grade)
        assertEquals(1f, s.progress)
    }

    @Test fun singleItemRequeueReshuffles() {
        val s0 = LessonReducer.start(lessonOf("x"), 7)
        val s1 = failCurrent(s0)
        assertEquals(listOf("x"), s1.queue)
        assertEquals(1, s1.attempts["x"])
        assertNotEquals(s0.current!!.order, s1.current!!.order)
    }

    @Test fun hints() {
        var s = LessonReducer.start(lessonOf("a", hints = listOf("h1", "h2")), 1)
        s = run(s, Hint, Hint, Hint)
        assertEquals(2, s.hintsShown["a"])
        assertEquals(Grade.Hinted, LessonReducer.result(solveCurrent(s)).grade)
        val none = LessonReducer.start(lessonOf("a"), 1)
        assertEquals(none, run(none, Hint))
        assertEquals(Grade.Clean, LessonReducer.result(solveCurrent(none)).grade)
    }

    @Test fun gradeClean_acceptableCounts() {
        val s = solveCurrent(LessonReducer.start(lessonOf("a", "b"), 1), chip = 2)
        val s2 = solveCurrent(s)
        assertEquals(Phase.Done, s2.phase)
        assertEquals(Grade.Clean, LessonReducer.result(s2).grade)
    }

    @Test fun gradeHintedAfterRetry() {
        var s = failCurrent(LessonReducer.start(lessonOf("a"), 1))
        s = solveCurrent(s)
        assertEquals(Phase.Done, s.phase)
        assertEquals(Grade.Hinted, LessonReducer.result(s).grade)
    }

    @Test fun progressCountsRequeued() {
        var s = LessonReducer.start(lessonOf("a", "b"), 1)
        assertEquals(0f, s.progress)
        s = failCurrent(s)
        assertEquals(0f, s.progress)
        s = solveCurrent(s)
        assertEquals(0.5f, s.progress)
    }

    @Test fun strayActionsUnchanged() {
        val s = LessonReducer.start(lessonOf("a", theory = 1), 1)
        assertEquals(s, run(s, TapChip(0), Check, Continue, Hint))
        var e = run(s, NextTheory)
        assertEquals(e, run(e, NextTheory, Continue, Check)) // Check with empty hole, Continue in Exercise
        e = fill(e, 1)
        assertTrue(e.phase is Phase.Feedback)
        assertEquals(e, run(e, TapChip(0), TapHole(0), Check, Hint, NextTheory))
        assertEquals(e, run(e, TapChip(99)))
        val done = solveCurrent(LessonReducer.start(lessonOf("a"), 1))
        assertEquals(done, run(done, Continue, Check, TapChip(0), Hint, NextTheory))
    }

    @Test fun deterministicAndSeedMixed() {
        val acts = arrayOf(TapChip(1), Check, Continue, TapChip(0), Check, Continue, Continue)
        val l = lessonOf("a", "b")
        assertEquals(run(LessonReducer.start(l, 5), *acts), run(LessonReducer.start(l, 5), *acts))
        val orders = (1..8).map { LessonReducer.start(l, it).current!!.order }.toSet()
        assertTrue(orders.size > 1)
    }
}
