package com.tlenparty.algolingo.lesson

import kotlin.test.*
import com.tlenparty.algolingo.lesson.domain.ExerciseResult
import com.tlenparty.algolingo.lesson.domain.Grade
import com.tlenparty.algolingo.lesson.domain.LessonResult
import com.tlenparty.algolingo.lesson.domain.XP_ASSISTED
import com.tlenparty.algolingo.lesson.domain.XP_CLEAN
import com.tlenparty.algolingo.lesson.domain.xpFor

class XpTest {
    private fun r(solved: Boolean, attempts: Int, hints: Int) = ExerciseResult("x", solved, attempts, hints)

    @Test fun clean() = assertEquals(XP_CLEAN, xpFor(r(true, 1, 0)))
    @Test fun hinted() = assertEquals(XP_ASSISTED, xpFor(r(true, 1, 1)))
    @Test fun retried() = assertEquals(XP_ASSISTED, xpFor(r(true, 2, 0)))
    @Test fun unsolved() = assertEquals(0, xpFor(r(false, 3, 2)))
    @Test fun lessonSum() {
        val clean = List(3) { r(true, 1, 0) }
        assertEquals(30, xpFor(LessonResult(Grade.Clean, clean)))
        assertEquals(0, xpFor(LessonResult(Grade.Clean, emptyList())))
        assertEquals(15, xpFor(LessonResult(Grade.Failed, listOf(r(true, 1, 0), r(true, 2, 0), r(false, 3, 0)))))
    }
}
