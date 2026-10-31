package com.tlenparty.algolingo.lesson.presentation

import com.tlenparty.algolingo.lesson.domain.CheckResult
import com.tlenparty.algolingo.lesson.domain.Exercise
import com.tlenparty.algolingo.lesson.domain.ExerciseResult
import com.tlenparty.algolingo.lesson.domain.ExerciseState
import com.tlenparty.algolingo.lesson.domain.Grade
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.LessonResult
import com.tlenparty.algolingo.lesson.domain.Verdict
import com.tlenparty.algolingo.lesson.domain.checkExercise
import com.tlenparty.algolingo.lesson.domain.initialExerciseState
import com.tlenparty.algolingo.lesson.domain.seedOf
import com.tlenparty.algolingo.lesson.domain.tapChip
import com.tlenparty.algolingo.lesson.domain.tapHole

// Pure lesson state machine. No Random.Default, never throws; stray actions return the state unchanged.
// NextTheory/Continue carry the screen they were issued from, so a double tap (or a tap on the outgoing
// AnimatedContent page) that arrives after the screen changed is stale and ignored (CR-01).
// No XP here (Phase 4). No ShowAnswer action: Reveal is entered automatically after a solve or the 3rd miss.

const val MAX_REPEATS = 2

sealed interface Phase {
    data class Theory(val index: Int) : Phase
    data object Solving : Phase
    data class Feedback(val check: CheckResult) : Phase
    data class Reveal(val solved: Boolean) : Phase
    data object Done : Phase
}

sealed interface LessonAction {
    data class NextTheory(val index: Int) : LessonAction
    data class TapChip(val id: Int) : LessonAction
    data class TapHole(val h: Int) : LessonAction
    data object Check : LessonAction
    data object Hint : LessonAction
    data class Continue(val from: ScreenKey) : LessonAction
}

data class LessonState(
    val lesson: Lesson,
    val phase: Phase,
    val queue: List<String>,
    val attempts: Map<String, Int>,
    val hintsShown: Map<String, Int>,
    val solved: Map<String, Boolean>,
    val current: ExerciseState?,
    val done: Int,
    val seed: Int,
) {
    val progress: Float
        get() = if (done + queue.size == 0) 1f else done.toFloat() / (done + queue.size)
}

object LessonReducer {
    private fun exerciseOf(s: LessonState, id: String?): Exercise? = s.lesson.exercises.firstOrNull { it.id == id }

    // Fresh state for the head of the queue; seed mixes exercise id, attempts so far and the lesson seed.
    private fun enter(s: LessonState, queue: List<String>, attempts: Map<String, Int>): ExerciseState? {
        val id = queue.firstOrNull() ?: return null
        val ex = exerciseOf(s, id) ?: return null
        return initialExerciseState(ex, seedOf(id, attempts[id] ?: 0) xor s.seed)
    }

    fun start(lesson: Lesson, seed: Int): LessonState {
        val base = LessonState(
            lesson, Phase.Solving, lesson.exercises.map { it.id },
            emptyMap(), emptyMap(), emptyMap(), null, 0, seed,
        )
        val withCur = base.copy(current = enter(base, base.queue, emptyMap()))
        return when {
            lesson.theory.isNotEmpty() -> withCur.copy(phase = Phase.Theory(0))
            withCur.current == null -> withCur.copy(phase = Phase.Done)
            else -> withCur
        }
    }

    fun reduce(s: LessonState, a: LessonAction): LessonState {
        val p = s.phase
        val id = s.queue.firstOrNull()
        val ex = exerciseOf(s, id)
        val cur = s.current
        return when {
            a is LessonAction.Continue && a.from != screenKey(s) -> s

            a is LessonAction.NextTheory && p is Phase.Theory && a.index == p.index -> s.copy(
                phase = when {
                    p.index + 1 < s.lesson.theory.size -> Phase.Theory(p.index + 1)
                    s.current == null -> Phase.Done // no playable exercise: never strand the UI in Exercise
                    else -> Phase.Solving
                }
            )

            a is LessonAction.TapChip && p is Phase.Solving && cur != null -> s.copy(current = tapChip(cur, a.id))
            a is LessonAction.TapHole && p is Phase.Solving && cur != null -> s.copy(current = tapHole(cur, a.h))

            a is LessonAction.Hint && p is Phase.Solving && ex != null && id != null && ex.hints.isNotEmpty() ->
                s.copy(hintsShown = s.hintsShown + (id to minOf((s.hintsShown[id] ?: 0) + 1, ex.hints.size)))

            a is LessonAction.Check && p is Phase.Solving && cur != null && ex != null && id != null -> {
                val r = checkExercise(ex, cur) ?: return s
                s.copy(
                    phase = Phase.Feedback(r),
                    attempts = s.attempts + (id to (s.attempts[id] ?: 0) + 1),
                    solved = if (r.verdict != Verdict.Wrong) s.solved + (id to true) else s.solved,
                )
            }

            a is LessonAction.Continue && p is Phase.Feedback && id != null -> when {
                s.solved[id] == true -> s.copy(phase = Phase.Reveal(true))
                (s.attempts[id] ?: 0) <= MAX_REPEATS -> {
                    val q = s.queue.drop(1) + id
                    s.copy(phase = Phase.Solving, queue = q, current = enter(s, q, s.attempts))
                }
                else -> s.copy(phase = Phase.Reveal(false))
            }

            a is LessonAction.Continue && p is Phase.Reveal && id != null -> {
                val q = s.queue.drop(1)
                val cr = enter(s, q, s.attempts)
                s.copy(
                    phase = if (cr == null) Phase.Done else Phase.Solving,
                    queue = q, done = s.done + 1, current = cr,
                )
            }

            else -> s
        }
    }

    fun result(s: LessonState): LessonResult {
        val per = s.lesson.exercises.map {
            ExerciseResult(it.id, s.solved[it.id] == true, s.attempts[it.id] ?: 0, s.hintsShown[it.id] ?: 0)
        }
        val grade = when {
            per.any { !it.solved } -> Grade.Failed
            per.any { it.attempts > 1 || it.hintsShown > 0 } -> Grade.Hinted
            else -> Grade.Clean
        }
        return LessonResult(grade, per)
    }
}
