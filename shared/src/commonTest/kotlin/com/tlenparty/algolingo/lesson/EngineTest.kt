package com.tlenparty.algolingo.lesson

import kotlin.test.*
import com.tlenparty.algolingo.lesson.domain.Accepted
import com.tlenparty.algolingo.lesson.domain.Chip
import com.tlenparty.algolingo.lesson.domain.Exercise
import com.tlenparty.algolingo.lesson.domain.Hole
import com.tlenparty.algolingo.lesson.domain.HoleResult
import com.tlenparty.algolingo.lesson.domain.Verdict
import com.tlenparty.algolingo.lesson.domain.checkExercise
import com.tlenparty.algolingo.lesson.domain.initialExerciseState
import com.tlenparty.algolingo.lesson.domain.tapChip
import com.tlenparty.algolingo.lesson.domain.tapHole

internal fun synth(vararg chips: String, holes: List<Hole>) = Exercise(
    id = "t.e1",
    segments = List(holes.size + 1) { "s$it" },
    holes = holes,
    bank = chips.map { Chip(it, if (it.startsWith("bad")) "why-$it" else null) },
)

class EngineTest {
    // bank: 0 "a", 1 "b", 2 "c", 3 "ushr", 4 "bad"
    private val ex = synth(
        "a", "b", "c", "ushr", "bad",
        holes = listOf(
            Hole(ok = listOf("a"), acceptable = listOf(Accepted("ushr", "works"))),
            Hole(ok = listOf("b"), wrongWhy = mapOf("c" to "c is wrong here")),
            Hole(ok = listOf("c")),
        ),
    )

    @Test fun initialState() {
        val s = initialExerciseState(ex, 1)
        assertEquals(listOf(null, null, null), s.placed)
        assertEquals(0, s.selected)
        assertEquals(ex.bank.indices.toList(), s.order.sorted())
        assertNull(initialExerciseState(synth("a", holes = emptyList()), 1).selected)
    }

    @Test fun tapChipFillsAndAdvances() {
        var s = initialExerciseState(ex, 1)
        s = tapChip(s, 0); assertEquals(listOf(0, null, null), s.placed); assertEquals(1, s.selected)
        s = tapChip(s, 1); s = tapChip(s, 2)
        assertEquals(listOf(0, 1, 2), s.placed); assertNull(s.selected)
        assertEquals(s, tapChip(s, 4)) // no empty hole, no selection
    }

    @Test fun noOps() {
        val s = tapChip(initialExerciseState(ex, 1), 0)
        assertEquals(s, tapChip(s, 0))
        assertEquals(s, tapChip(s, 99))
        assertEquals(s, tapChip(s, -1))
        assertEquals(s, tapHole(s, 9))
        assertEquals(s, tapHole(s, -1))
    }

    @Test fun selectedFilledHoleIsOverwritten() {
        // hand-built: unreachable via the public API (tapHole clears a filled hole), kept as a guard
        var s = initialExerciseState(ex, 1).copy(placed = listOf(4, null, null), selected = 0)
        s = tapChip(s, 1)
        assertEquals(listOf(1, null, null), s.placed)
        assertFalse(s.isPlaced(4))
    }

    @Test fun cyclicNextEmpty() {
        var s = initialExerciseState(ex, 1).copy(selected = 2)
        s = tapChip(s, 0)
        assertEquals(listOf(null, null, 0), s.placed); assertEquals(0, s.selected)
    }

    @Test fun removeSelectsHoleThenRefillClearsSelection() {
        var s = initialExerciseState(ex, 1)
        s = tapChip(tapChip(tapChip(s, 0), 1), 2)
        s = tapHole(s, 1)
        assertEquals(listOf(0, null, 2), s.placed); assertEquals(1, s.selected)
        s = tapChip(s, 1)
        assertNull(s.selected)
        // empty hole: just selects
        val e = tapHole(initialExerciseState(ex, 1), 2)
        assertEquals(2, e.selected); assertEquals(listOf(null, null, null), e.placed)
    }

    @Test fun checkNullWhileEmpty() {
        assertNull(checkExercise(ex, initialExerciseState(ex, 1)))
        assertNull(checkExercise(ex, tapChip(initialExerciseState(ex, 1), 0)))
    }

    @Test fun checkThreeOutcomes() {
        fun run(vararg ids: Int) = checkExercise(ex, initialExerciseState(ex, 1).copy(placed = ids.toList(), selected = null))!!
        assertEquals(Verdict.Ok, run(0, 1, 2).verdict)
        val acc = run(3, 1, 2)
        assertEquals(Verdict.Acceptable, acc.verdict)
        assertEquals(HoleResult.Acceptable("ushr", "works"), acc.holes[0])
        val w = run(0, 2, 4)
        assertEquals(Verdict.Wrong, w.verdict)
        assertEquals(HoleResult.Wrong("c", "c is wrong here", "b"), w.holes[1]) // wrongWhy overrides
        assertEquals(HoleResult.Wrong("bad", "why-bad", "c"), w.holes[2])      // chip.why fallback
        assertEquals(Verdict.Wrong, run(3, 2, 2).verdict) // Wrong beats Acceptable
    }

    @Test fun perHoleIndependence() {
        val e = synth("mid + 1", "mid - 1", holes = listOf(Hole(ok = listOf("mid + 1")), Hole(ok = listOf("mid - 1"))))
        val r = checkExercise(e, initialExerciseState(e, 1).copy(placed = listOf(1, 0), selected = null))!!
        assertTrue(r.holes.all { it is HoleResult.Wrong })
    }
}
