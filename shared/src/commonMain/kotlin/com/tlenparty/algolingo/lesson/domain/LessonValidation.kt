package com.tlenparty.algolingo.lesson.domain

const val MAX_BANK = 10

/** Widest design line that fits the phone screen without horizontal scroll. */
const val MAX_DESIGN_LINE = 34

/** segments interleaved with each hole's reference answer (ok[0]). */
fun assembled(ex: Exercise): String = buildString {
    ex.segments.forEachIndexed { i, s ->
        append(s)
        ex.holes.getOrNull(i)?.ok?.firstOrNull()?.let { append(it) }
    }
}

/** Returns human-readable problems; empty = valid. Never throws. */
fun validate(lesson: Lesson): List<String> {
    val errors = mutableListOf<String>()
    if (lesson.schema != 1) errors += "lesson: schema ${lesson.schema} != 1"
    val design = lesson.kind == LessonKind.Design
    val steps = if (design) 2..4 else 2..3
    if (lesson.exercises.size !in steps) errors += "lesson: ${lesson.exercises.size} exercises, expected ${steps.first}..${steps.last}"
    val ids = lesson.exercises.map { it.id }
    if (ids.size != ids.toSet().size) errors += "lesson: duplicate exercise ids"
    val counts = lesson.exercises.map { it.holes.size }
    if (!design && counts.zipWithNext().any { (a, b) -> b <= a }) errors += "lesson: hole counts $counts not strictly increasing"

    for ((i, ex) in lesson.exercises.withIndex()) {
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
            // map keys escape strict decoding: catch typos and entries checkExercise() can never reach
            hole.wrongWhy.forEach { (k, v) ->
                if (k !in bankTexts) errors += "$e: hole $h wrongWhy key '$k' not in bank"
                if (k in hole.ok || hole.acceptable.any { it.text == k }) errors += "$e: hole $h wrongWhy key '$k' is a correct answer"
                if (v.isBlank()) errors += "$e: hole $h wrongWhy for '$k' is blank"
            }
        }
        // multiset assignability of reference answers
        val need = ex.holes.mapNotNull { it.ok.getOrNull(0) }.groupingBy { it }.eachCount()
        for ((t, n) in need) {
            val have = bankTexts.count { it == t }
            if (have < n && have > 0) errors += "$e: '$t' needed $n times, bank has $have"
        }
        // checkExercise() reads only the placed hole's wrongWhy, then chip.why: a distractor needs a why wherever it can land.
        for (c in ex.bank) if (c.text !in used && c.why.isNullOrBlank()) {
            ex.holes.forEachIndexed { h, hole ->
                if (hole.wrongWhy[c.text].isNullOrBlank()) errors += "$e: distractor '${c.text}' has no why for hole $h"
            }
        }
        if (shapeOk && ex.holes.all { it.ok.isNotEmpty() }) {
            val rebuilt = assembled(ex)
            // design: only the final step is the reference scheme; earlier steps are requirements/API/decisions
            if ((!design || i == lesson.exercises.lastIndex) && rebuilt != lesson.solution.code)
                errors += "$e: segments + ok[0] differ from solution.code"
            if (design) rebuilt.split('\n').forEachIndexed { n, line ->
                if (line.length > MAX_DESIGN_LINE) errors += "$e: line ${n + 1} has ${line.length} chars (max $MAX_DESIGN_LINE)"
            }
        }
    }
    return errors
}
