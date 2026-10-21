package com.tlenparty.algolingo.lesson

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val schema: Int = 1,
    val id: String,
    val level: Int = 0,
    val title: String,
    val statement: String,
    val trains: String? = null,
    val targetTimeSec: Int? = null,
    val theory: List<TheoryCard> = emptyList(),
    val exercises: List<Exercise>,
    val solution: Solution,
)

@Serializable
data class TheoryCard(
    val title: String,
    val body: String = "",
    val signals: List<String> = emptyList(),
    val skeleton: String? = null,
    val pitfalls: List<String> = emptyList(),
)

@Serializable
data class Exercise(
    val id: String,
    val segments: List<String>,
    val holes: List<Hole>,
    val bank: List<Chip>,
    val hints: List<String> = emptyList(),
)

@Serializable
data class Hole(
    val ok: List<String>,
    val acceptable: List<Accepted> = emptyList(),
    val wrongWhy: Map<String, String> = emptyMap(),
)

@Serializable
data class Accepted(val text: String, val note: String)

// Chip identity = index in Exercise.bank (EXER-05); no id field on purpose.
@Serializable
data class Chip(val text: String, val why: String? = null)

@Serializable
data class Solution(
    val code: String,
    val time: String,
    val space: String,
    val pitfalls: List<String> = emptyList(),
    val variations: List<String> = emptyList(),
)

const val MAX_BANK = 10

/** Returns human-readable problems; empty = valid. Never throws. */
fun validate(lesson: Lesson): List<String> {
    val errors = mutableListOf<String>()
    if (lesson.schema != 1) errors += "lesson: schema ${lesson.schema} != 1"
    if (lesson.exercises.size !in 2..3) errors += "lesson: ${lesson.exercises.size} exercises, expected 2..3"
    val ids = lesson.exercises.map { it.id }
    if (ids.size != ids.toSet().size) errors += "lesson: duplicate exercise ids"
    val counts = lesson.exercises.map { it.holes.size }
    if (counts.zipWithNext().any { (a, b) -> b <= a }) errors += "lesson: hole counts $counts not strictly increasing"

    for (ex in lesson.exercises) {
        val e = ex.id
        if (!ex.id.startsWith(lesson.id + ".")) errors += "$e: id is not prefixed with '${lesson.id}.'"
        val shapeOk = ex.segments.size == ex.holes.size + 1
        if (!shapeOk) errors += "$e: segments=${ex.segments.size} but holes=${ex.holes.size} (need holes+1)"
        ex.holes.forEachIndexed { h, hole ->
            if (hole.ok.isEmpty()) errors += "$e: hole $h has empty ok"
            hole.acceptable.firstOrNull { a -> a.text in hole.ok }
                ?.let { errors += "$e: hole $h acceptable '${it.text}' also in ok" }
        }
        val bankTexts = ex.bank.map { it.text }
        if (ex.bank.size > MAX_BANK) errors += "$e: bank has ${ex.bank.size} chips (max $MAX_BANK)"
        val used = mutableSetOf<String>()
        ex.holes.forEachIndexed { h, hole ->
            (hole.ok + hole.acceptable.map { it.text }).forEach { t ->
                used += t
                if (t !in bankTexts) errors += "$e: hole $h text '$t' missing from bank"
            }
        }
        // multiset assignability of reference answers
        val need = ex.holes.mapNotNull { it.ok.getOrNull(0) }.groupingBy { it }.eachCount()
        for ((t, n) in need) {
            val have = bankTexts.count { it == t }
            if (have < n && have > 0) errors += "$e: '$t' needed $n times, bank has $have"
        }
        for (c in ex.bank) {
            if (c.text !in used && c.why == null && ex.holes.none { it.wrongWhy.containsKey(c.text) }) {
                errors += "$e: distractor '${c.text}' has no why"
            }
        }
        if (shapeOk && ex.holes.all { it.ok.isNotEmpty() }) {
            val rebuilt = buildString {
                ex.segments.forEachIndexed { i, s ->
                    append(s)
                    ex.holes.getOrNull(i)?.let { append(it.ok[0]) }
                }
            }
            if (rebuilt != lesson.solution.code) errors += "$e: segments + ok[0] differ from solution.code"
        }
    }
    return errors
}
