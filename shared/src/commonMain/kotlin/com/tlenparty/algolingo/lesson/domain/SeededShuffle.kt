package com.tlenparty.algolingo.lesson.domain

import kotlin.random.Random

// Explicit hash: no String.hashCode reliance, identical on every target.
fun seedOf(exerciseId: String, attempt: Int): Int =
    exerciseId.fold(17) { h, c -> h * 31 + c.code } * 31 + attempt

// Hand-written Fisher-Yates over a seeded Random; never Random.Default.
fun <T> seededShuffle(items: List<T>, seed: Int): List<T> {
    val r = Random(seed)
    val a = items.toMutableList()
    for (i in a.lastIndex downTo 1) {
        val j = r.nextInt(i + 1)
        val t = a[i]; a[i] = a[j]; a[j] = t
    }
    return a
}
