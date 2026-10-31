package com.tlenparty.algolingo.lesson.domain

// XP numbers are the single place to redefine in Phase 4 (D-A1, user confirmed 2026-10-21).
const val XP_CLEAN = 10
const val XP_ASSISTED = 5

fun xpFor(r: ExerciseResult): Int = when {
    !r.solved -> 0
    r.hintsShown >= 1 || r.attempts >= 2 -> XP_ASSISTED
    else -> XP_CLEAN
}

fun xpFor(r: LessonResult): Int = r.perExercise.sumOf { xpFor(it) }
