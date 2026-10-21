package com.tlenparty.algolingo.lesson

import com.tlenparty.algolingo.lesson.LessonAction.*
import kotlin.test.*

class UiMappingTest {
    private val e3 = Exercise(
        id = "l.e3",
        segments = listOf(
            "fun f(): Int {\n    var lo = 0\n    var hi = 1\n    while (",
            ") {\n        val mid = 0\n        when {\n            a -> return ",
            "\n            b -> lo = mid + ",
            "\n            else -> hi = mid - ",
            "\n        }\n    }\n    return -1\n}",
        ),
        holes = listOf("lo <= hi", "mid", "1", "1").map { Hole(ok = listOf(it)) },
        bank = listOf(Chip("lo <= hi"), Chip("mid"), Chip("1"), Chip("1"), Chip("0", "w")),
        hints = listOf("h1", "h2"),
    )
    private val code = buildString {
        e3.segments.forEachIndexed { i, s -> append(s); e3.holes.getOrNull(i)?.let { append(it.ok[0]) } }
    }
    private val lesson = Lesson(
        id = "l", title = "t", statement = "s",
        exercises = listOf(e3, e3.copy(id = "l.e4")), solution = Solution(code, "", ""),
    )
    private fun run(s: LessonState, vararg a: LessonAction) = a.fold(s) { st, x -> LessonReducer.reduce(st, x) }
    private val state0 = ExerciseState(listOf(null, 2, null), 0, listOf(0, 1, 2))

    @Test fun holeVisualWithoutCheck() {
        assertEquals(HoleVisual.EmptySelected, holeVisual(0, state0, null))
        assertEquals(HoleVisual.Filled, holeVisual(1, state0, null))
        assertEquals(HoleVisual.Empty, holeVisual(2, state0, null))
    }

    @Test fun holeVisualWithCheck() {
        val c = CheckResult(listOf(HoleResult.Ok, HoleResult.Acceptable("a", "n"), HoleResult.Wrong("w", null, "c")))
        assertEquals(
            listOf(HoleVisual.Ok, HoleVisual.Acceptable, HoleVisual.Wrong),
            (0..2).map { holeVisual(it, state0, c) },
        )
    }

    @Test fun panelSuccess() {
        val p = panelModel(CheckResult(listOf(HoleResult.Ok)))
        assertEquals(PanelKind.Success, p.kind)
        assertEquals("✅ Отлично!", p.title)
        assertEquals("Дальше", p.button)
        assertTrue(p.wrong.isEmpty() && p.acceptable.isEmpty())
    }

    @Test fun panelAmber() {
        val p = panelModel(CheckResult(listOf(HoleResult.Ok, HoleResult.Acceptable("x", "note"))))
        assertEquals(PanelKind.Amber, p.kind)
        assertEquals("👌 Принято, но…", p.title)
        assertEquals("Дальше", p.button)
        assertEquals(listOf(PanelItem("x", "note")), p.acceptable)
        assertTrue(p.wrong.isEmpty())
    }

    @Test fun panelDangerWithWhyAndAcceptable() {
        val p = panelModel(
            CheckResult(listOf(HoleResult.Wrong("w1", "why1", "c1"), HoleResult.Acceptable("a", "n"), HoleResult.Wrong("w2", "why2", "c2"))),
        )
        assertEquals(PanelKind.Danger, p.kind)
        assertEquals("❌ Не совсем", p.title)
        assertEquals("Понятно, дальше", p.button)
        assertEquals(listOf(PanelItem("w1", "why1", "c1"), PanelItem("w2", "why2", "c2")), p.wrong)
        assertEquals(listOf(PanelItem("a", "n")), p.acceptable)
    }

    @Test fun panelDangerWithoutWhy() {
        val p = panelModel(CheckResult(listOf(HoleResult.Wrong("w", null, "nums[0]"))))
        assertEquals(PanelItem("w", "Здесь нужно другое. Верно: `nums[0]`", null), p.wrong.single())
    }

    @Test fun templateLinesShape() {
        val lines = templateLines(e3.segments)
        assertEquals(13, lines.size)
        assertEquals(listOf(0, 1, 2, 3), lines.flatten().filterIsInstance<Piece.HoleAt>().map { it.h })
        assertTrue(lines.flatten().none { it is Piece.Code && it.s.isEmpty() })
        // hole 0 sits at the end of the "while (" line, followed by ") {"
        assertEquals(listOf(Piece.Code("    while ("), Piece.HoleAt(0), Piece.Code(") {")), lines[3])
    }

    @Test fun templateLinesEmptyInput() {
        assertEquals(listOf(emptyList()), templateLines(emptyList()))
        assertEquals(listOf(listOf(Piece.Code("a"))), templateLines(listOf("a")))
    }

    @Test fun solutionSpansSliceToOk() {
        val spans = solutionSpans(e3)
        assertEquals(listOf("lo <= hi", "mid", "1", "1"), spans.map { code.substring(it) })
        assertEquals(code, lesson.solution.code)
    }

    @Test fun solutionSpansMalformed() {
        val bad = Exercise("x", emptyList(), listOf(Hole(ok = emptyList())), emptyList())
        assertEquals(listOf(0 until 0), solutionSpans(bad))
    }

    @Test fun hintsRemainingClamps() {
        assertEquals(2, hintsRemaining(e3, 0))
        assertEquals(1, hintsRemaining(e3, 1))
        assertEquals(0, hintsRemaining(e3, 5))
    }

    @Test fun isLastExerciseByQueue() {
        val s = LessonReducer.start(lesson, 1)
        assertFalse(isLastExercise(s))
        assertTrue(isLastExercise(s.copy(queue = listOf("l.e4"))))
    }

    @Test fun screenKeyStableAcrossCheckAndChangesOnRequeue() {
        val solving = LessonReducer.start(lesson, 1)
        val feedback = run(solving, TapChip(4), TapChip(1), TapChip(2), TapChip(3), Check) // hole 0 gets wrong chip
        assertTrue(feedback.phase is Phase.Feedback)
        assertEquals(screenKey(solving), screenKey(feedback))
        val requeued = run(feedback, Continue(screenKey(feedback)))
        assertEquals(Phase.Solving, requeued.phase)
        assertNotEquals(screenKey(solving), screenKey(requeued))
    }

    @Test fun screenKeyPhases() {
        val s = LessonReducer.start(lesson.copy(theory = listOf(TheoryCard("c"))), 1)
        assertEquals(ScreenKey.Theory(0), screenKey(s))
        assertEquals(ScreenKey.Exercise("l.e3", 0), screenKey(run(s, NextTheory(0))))
        assertEquals(ScreenKey.Reveal("l.e3"), screenKey(s.copy(phase = Phase.Reveal(true))))
        assertEquals(ScreenKey.Done, screenKey(s.copy(phase = Phase.Done)))
    }
}
