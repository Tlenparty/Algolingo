package com.tlenparty.algolingo.lesson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ShuffleTest {
    // captured on JVM, asserted on iOS
    private val golden = listOf(5, 0, 4, 9, 2, 8, 1, 7, 6, 3)

    @Test fun golden() = assertEquals(golden, seededShuffle((0..9).toList(), 42))

    @Test fun deterministicPermutationNoMutation() {
        val input = (0..9).toList()
        val a = seededShuffle(input, 7)
        assertEquals(a, seededShuffle(input, 7))
        assertEquals(input, a.sorted())
        assertEquals((0..9).toList(), input)
    }

    @Test fun seedOfDiffersByAttemptAndIsPure() {
        assertNotEquals(seedOf("l0.4.e1", 0), seedOf("l0.4.e1", 1))
        assertEquals(seedOf("l0.4.e1", 2), seedOf("l0.4.e1", 2))
    }

    @Test fun smallLists() {
        assertEquals(emptyList(), seededShuffle(emptyList<Int>(), 1))
        assertEquals(listOf(5), seededShuffle(listOf(5), 1))
    }
}
