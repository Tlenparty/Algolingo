package com.tlenparty.algolingo.lesson

import kotlin.test.*

class DuplicateChipTest {
    // bank: 0 mid, 1 mid, 2 "1", 3 "1"
    private val ex = synth(
        "mid", "mid", "1", "1",
        holes = listOf(Hole(ok = listOf("mid")), Hole(ok = listOf("mid")), Hole(ok = listOf("1")), Hole(ok = listOf("1"))),
    )

    @Test fun equalTextChipsAreIndependent() {
        var s = initial(ex, 3)
        s = tapChip(s, 2); s = tapChip(s, 3)
        assertEquals(listOf(2, 3, null, null), s.placed)
        assertTrue(s.isPlaced(2) && s.isPlaced(3))
        assertFalse(s.isPlaced(0))
        assertEquals(s, tapChip(s, 2)) // re-tap placed is a no-op
        s = tapHole(s, 0) // return first "1"; second stays
        assertFalse(s.isPlaced(2)); assertTrue(s.isPlaced(3))
    }

    @Test fun bothCheckOk() {
        val s = initial(ex, 3).copy(placed = listOf(0, 1, 2, 3), selected = null)
        assertEquals(Verdict.Ok, check(ex, s)!!.verdict)
        val swapped = s.copy(placed = listOf(1, 0, 3, 2))
        assertEquals(Verdict.Ok, check(ex, swapped)!!.verdict)
    }
}
