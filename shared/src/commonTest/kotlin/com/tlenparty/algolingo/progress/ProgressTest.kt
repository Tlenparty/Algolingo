package com.tlenparty.algolingo.progress

import com.tlenparty.algolingo.lesson.ExerciseResult
import com.tlenparty.algolingo.lesson.Grade
import com.tlenparty.algolingo.lesson.LessonResult
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.*

class ProgressTest {
    private val d = LocalDate(2026, 10, 21)
    private fun day(n: Int) = if (n >= 0) d.plus(n, DateTimeUnit.DAY) else d.minus(-n, DateTimeUnit.DAY)

    private fun res(grade: Grade) = LessonResult(
        grade,
        when (grade) {
            Grade.Clean -> listOf(ExerciseResult("a", true, 1, 0), ExerciseResult("b", true, 1, 0))
            Grade.Hinted -> listOf(ExerciseResult("a", true, 1, 1))
            Grade.Failed -> listOf(ExerciseResult("a", false, 3, 0))
        },
    )

    private fun apply(p: Progress, grade: Grade, today: LocalDate, id: String = "l0.4") =
        applyLessonResult(p, id, res(grade), today)

    // XP

    @Test fun xpAddsLessonXp() = assertEquals(20, apply(Progress(), Grade.Clean, d).xp)

    @Test fun xpSumsAcrossLessons() {
        val p = apply(apply(Progress(), Grade.Clean, d), Grade.Hinted, d, "l0.3")
        assertEquals(25, p.xp)
    }

    @Test fun failedAllUnsolvedAddsZero() = assertEquals(0, apply(Progress(), Grade.Failed, d).xp)

    // Streak

    @Test fun streakAcrossMidnight() {
        val p = apply(apply(Progress(), Grade.Clean, d), Grade.Clean, day(1))
        assertEquals(2, p.streak)
        assertEquals(day(1), p.lastActiveDay)
    }

    @Test fun twoLessonsOneDayCountOnce() {
        val p = apply(apply(Progress(), Grade.Clean, d), Grade.Clean, d, "l0.3")
        assertEquals(1, p.streak)
        assertEquals(40, p.xp)
    }

    @Test fun missedDayResets() = assertEquals(1, apply(apply(Progress(), Grade.Clean, d), Grade.Clean, day(2)).streak)

    @Test fun visibleStreak() {
        val p = apply(Progress(), Grade.Clean, d)
        assertEquals(0, visibleStreak(p, day(2)))
        assertEquals(1, visibleStreak(p, day(1)))
        assertEquals(1, visibleStreak(p, d))
        assertEquals(0, visibleStreak(Progress(), d))
    }

    @Test fun clockBackKeepsStreakAndLastDay() {
        val p = apply(apply(Progress(), Grade.Clean, d), Grade.Clean, day(-1))
        assertEquals(1, p.streak)
        assertEquals(d, p.lastActiveDay)
    }

    @Test fun clockBackKeepsLongStreak() {
        val p = apply(Progress(streak = 3, lastActiveDay = d), Grade.Clean, day(-2))
        assertEquals(3, p.streak)
        assertEquals(d, p.lastActiveDay)
    }

    // SRS

    @Test fun firstPassAnyGradeStartsLadder() {
        for (g in Grade.entries) {
            assertEquals(LessonProgress(0, day(1)), apply(Progress(), g, d).lessons["l0.4"], "$g")
        }
    }

    @Test fun cleanLadderToMastered() {
        var p = apply(Progress(), Grade.Clean, d)
        p = apply(p, Grade.Clean, day(1))
        assertEquals(LessonProgress(1, day(4)), p.lessons["l0.4"])
        p = apply(p, Grade.Clean, day(4))
        assertEquals(LessonProgress(2, day(11)), p.lessons["l0.4"])
        p = apply(p, Grade.Clean, day(11))
        assertEquals(LessonProgress(3, null), p.lessons["l0.4"])
    }

    @Test fun hintedKeepsInterval() =
        assertEquals(LessonProgress(1, day(7)), schedule(LessonProgress(1, day(4)), Grade.Hinted, day(4)))

    @Test fun failedResets() =
        assertEquals(LessonProgress(0, day(12)), schedule(LessonProgress(2, day(11)), Grade.Failed, day(11)))

    @Test fun overdueCountsAsDue() =
        assertEquals(LessonProgress(2, day(13)), schedule(LessonProgress(1, day(4)), Grade.Clean, day(6)))

    @Test fun notDueReplayKeepsSrsButCountsXpAndStreak() {
        val lp = LessonProgress(1, day(4))
        val start = Progress(xp = 5, streak = 1, lastActiveDay = day(1), lessons = mapOf("l0.4" to lp))
        for (g in Grade.entries) {
            val p = apply(start, g, day(2))
            assertEquals(lp, p.lessons["l0.4"], "$g")
            assertEquals(2, p.streak, "$g")
        }
        assertEquals(25, apply(start, Grade.Clean, day(2)).xp)
    }

    @Test fun masteredStaysMastered() {
        for (g in Grade.entries) {
            assertEquals(LessonProgress(3, null), schedule(LessonProgress(3, null), g, day(30)), "$g")
        }
    }

    // Codec

    @Test fun encodeWritesSchemaVersion() = assertTrue("\"schemaVersion\":1" in encodeProgress(Progress()))

    @Test fun decodeEmptyObjectIsDefault() = assertEquals(Progress(), decodeProgress("{}"))

    @Test fun decodeIgnoresUnknownKeys() = assertEquals(Progress(xp = 7), decodeProgress("{\"xp\":7,\"future\":true}"))

    @Test fun decodeGarbageIsNull() {
        assertNull(decodeProgress("garbage"))
        assertNull(decodeProgress("{\"xp\":\"abc\"}"))
    }

    @Test fun roundTrip() {
        val p = Progress(xp = 30, streak = 2, lastActiveDay = day(1), lessons = mapOf("l0.4" to LessonProgress(1, day(4))))
        val s = encodeProgress(p)
        assertTrue("\"2026-10-22\"" in s, s)
        assertEquals(p, decodeProgress(s))
    }
}
