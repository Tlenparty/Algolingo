package com.tlenparty.algolingo.lesson

import java.io.File
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import com.tlenparty.algolingo.lesson.domain.Grade
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.presentation.LessonAction
import com.tlenparty.algolingo.lesson.presentation.LessonReducer
import com.tlenparty.algolingo.lesson.presentation.LessonState
import com.tlenparty.algolingo.lesson.presentation.Phase
import com.tlenparty.algolingo.lesson.presentation.ScreenKey
import com.tlenparty.algolingo.lesson.presentation.holeVisual
import com.tlenparty.algolingo.lesson.presentation.isLastExercise
import com.tlenparty.algolingo.lesson.presentation.panelModel
import com.tlenparty.algolingo.lesson.presentation.screenKey
import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.lesson.data.LessonDto
import com.tlenparty.algolingo.lesson.data.toDomain
import com.tlenparty.algolingo.progress.data.CatalogDto
import com.tlenparty.algolingo.progress.data.toDomain

/** Seeded random action sequences over every catalog lesson: the reducer must never throw or break its invariants. */
class LessonFuzzTest {
    private val dir = File("src/commonMain/composeResources/files/content")
    private val lessons: List<Lesson> = run {
        val ids = Json { ignoreUnknownKeys = true }.decodeFromString<CatalogDto>(File(dir, "index.json").readText()).toDomain().lessons.map { it.id }
        ids.map { Json { ignoreUnknownKeys = true }.decodeFromString<LessonDto>(File(dir, "lessons/$it.json").readText()).toDomain() }
    }

    private fun randomAction(s: LessonState, rnd: Random, stale: ScreenKey): LessonAction {
        val bank = s.current?.order?.size ?: 0
        val holes = s.current?.placed?.size ?: 0
        val r = rnd.nextInt(100)
        return when {
            r < 40 -> LessonAction.TapChip(rnd.nextInt(-1, bank + 2))
            r < 55 -> LessonAction.TapHole(rnd.nextInt(-1, holes + 2))
            r < 70 -> LessonAction.Check
            r < 78 -> LessonAction.Hint
            r < 93 -> LessonAction.Continue(screenKey(s))
            r < 96 -> LessonAction.Continue(stale)
            else -> LessonAction.NextTheory(rnd.nextInt(-1, s.lesson.theory.size + 2))
        }
    }

    private fun check(s: LessonState, where: String) {
        val ids = s.lesson.exercises.map { it.id }.toSet()
        fun ok(cond: Boolean, what: String) { if (!cond) fail("$where: $what; state=$s") }
        ok(s.queue.all { it in ids }, "queue ids")
        s.current?.let { c ->
            val placed = c.placed.filterNotNull()
            ok(placed.size == placed.toSet().size, "placed unique")
            ok(placed.all { it in c.order.indices }, "placed in bank")
            ok(c.selected == null || c.selected in c.placed.indices, "selected in holes")
            ok(c.order.sorted() == c.order.indices.toList(), "order permutation")
            c.placed.indices.forEach { holeVisual(it, c, null) }
        }
        ok(s.progress in 0f..1f, "progress ${s.progress}")
        if (s.phase is Phase.Done) ok(s.queue.isEmpty() && s.current == null, "done means empty")
        ok(s.done + s.queue.size <= s.lesson.exercises.size, "done+queue")
        ok(s.attempts.values.all { it >= 0 } && s.hintsShown.values.all { it >= 0 }, "counters")
        (s.phase as? Phase.Feedback)?.let { panelModel(it.check) }
        LessonReducer.result(s)
        isLastExercise(s)
        screenKey(s)
    }

    @Test
    fun randomActionsNeverBreakState() {
        assertTrue(lessons.isNotEmpty())
        lessons.forEachIndexed { li, lesson ->
            repeat(60) { seed ->
                val rnd = Random(seed * 7919 + li)
                var s = LessonReducer.start(lesson, seed)
                var stale = screenKey(s)
                check(s, "${lesson.id} seed=$seed step=start")
                repeat(600) { step ->
                    if (rnd.nextInt(10) == 0) stale = screenKey(s)
                    s = LessonReducer.reduce(s, randomAction(s, rnd, stale))
                    check(s, "${lesson.id} seed=$seed step=$step")
                }
            }
        }
    }

    /** Fills every hole with an unplaced chip whose text is ok[0], checks, continues: must finish the lesson. */
    @Test
    fun smartPlayerFinishesEveryLesson() {
        lessons.forEach { lesson ->
            var s = LessonReducer.start(lesson, 1)
            var steps = 0
            while (s.phase !is Phase.Done && steps++ < 500) {
                val p = s.phase
                s = when (p) {
                    is Phase.Theory -> LessonReducer.reduce(s, LessonAction.NextTheory(p.index))
                    Phase.Solving -> {
                        val ex = lesson.exercises.first { it.id == s.queue.first() }
                        ex.holes.forEachIndexed { h, hole ->
                            s = LessonReducer.reduce(s, LessonAction.TapHole(h))
                            val chip = ex.bank.indices.first { ex.bank[it].text == hole.ok[0] && !s.current!!.isPlaced(it) }
                            s = LessonReducer.reduce(s, LessonAction.TapChip(chip))
                        }
                        LessonReducer.reduce(s, LessonAction.Check)
                    }
                    else -> LessonReducer.reduce(s, LessonAction.Continue(screenKey(s)))
                }
                check(s, "${lesson.id} smart step=$steps")
            }
            assertEquals(Phase.Done, s.phase, "${lesson.id} did not finish")
            assertEquals(Grade.Clean, LessonReducer.result(s).grade, lesson.id)
        }
    }
}
