package com.tlenparty.algolingo.progress

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.progress.LessonStatus.Available
import com.tlenparty.algolingo.progress.LessonStatus.Done
import com.tlenparty.algolingo.progress.LessonStatus.Locked
import com.tlenparty.algolingo.progress.LessonStatus.Soon
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.*

class CatalogTest {
    private val d = LocalDate(2026, 10, 22)
    private fun day(n: Int) = if (n >= 0) d.plus(n, DateTimeUnit.DAY) else d.minus(-n, DateTimeUnit.DAY)
    private val ids = listOf("a", "b", "c", "d")
    private fun p(vararg l: Pair<String, LessonProgress>) = Progress(lessons = l.toMap())

    @Test
    fun noProgressOnlyFirstAvailable() =
        assertEquals(listOf(Available, Locked, Locked, Locked), lessonStatuses(ids, Progress()))

    @Test
    fun doneUnlocksNext() =
        assertEquals(listOf(Done, Available, Locked, Locked), lessonStatuses(ids, p("a" to LessonProgress(1, day(3)))))

    @Test
    fun failedPassCountsAsDoneAndUnlocks() =
        assertEquals(listOf(Done, Available, Locked, Locked), lessonStatuses(ids, p("a" to LessonProgress(0, day(1)))))

    @Test
    fun gapUnlocksAfterEachDone() = assertEquals(
        listOf(Done, Available, Done, Available),
        lessonStatuses(ids, p("a" to LessonProgress(), "c" to LessonProgress())),
    )

    @Test
    fun emptyIds() = assertEquals(emptyList(), lessonStatuses(emptyList(), Progress()))

    @Test
    fun dueTodayAndOverdueIncluded() = assertEquals(
        listOf("a", "b"),
        dueIds(ids, p("b" to LessonProgress(1, day(-1)), "a" to LessonProgress(0, d)), d),
    )

    @Test
    fun futureAndMasteredExcluded() = assertEquals(
        emptyList(),
        dueIds(ids, p("a" to LessonProgress(0, day(1)), "b" to LessonProgress(3, null)), d),
    )

    @Test
    fun unknownIdExcluded() = assertEquals(
        listOf("c"),
        dueIds(ids, p("zzz" to LessonProgress(0, day(-5)), "c" to LessonProgress(0, d)), d),
    )

    @Test
    fun homeModelBuildsCaptionsAndCounters() {
        val cat = Catalog(lessons = listOf(CatalogEntry("l0.4", "Бинарный поиск")))
        val prog = Progress(xp = 42, streak = 5, lastActiveDay = day(-2), lessons = mapOf("l0.4" to LessonProgress(0, d)))
        assertEquals(
            HomeModel(streak = 0, xp = 42, nodes = listOf(HomeNode("l0.4", "0.4 Бинарный поиск", Done)), due = listOf("l0.4")),
            homeModel(cat, prog, d),
        )
    }

    @Test
    fun homeModelKeepsStreakWhenActiveYesterday() =
        assertEquals(3, homeModel(Catalog(), Progress(streak = 3, lastActiveDay = day(-1)), d).streak)

    @Test
    fun homeModelNextDayShowsDueAndThenBrokenStreak() {
        // Finished on d: due d+1, streak 1. Home must change when only `today` moves (CR-01).
        val cat = Catalog(lessons = listOf(CatalogEntry("l0.4", "Б")))
        val prog = Progress(streak = 1, lastActiveDay = d, lessons = mapOf("l0.4" to LessonProgress(0, day(1))))
        assertEquals(emptyList(), homeModel(cat, prog, d).due)
        assertEquals(listOf("l0.4") to 1, homeModel(cat, prog, day(1)).let { it.due to it.streak })
        assertEquals(0, homeModel(cat, prog, day(2)).streak)
    }

    @Test
    fun parseValidWithUnknownKeys() = assertEquals(
        Catalog(1, listOf(CatalogEntry("l0.4", "Б"))),
        parseCatalog("""{"schema":1,"extra":true,"lessons":[{"id":"l0.4","title":"Б","x":1}]}"""),
    )

    @Test
    fun parseDropsDuplicateIds() = assertEquals(
        listOf(CatalogEntry("a", "1"), CatalogEntry("b", "2")),
        parseCatalog("""{"lessons":[{"id":"a","title":"1"},{"id":"b","title":"2"},{"id":"a","title":"3"}]}""")?.lessons,
    )

    @Test
    fun explicitCaptionWins() = assertEquals(
        "Свой", homeModel(Catalog(lessons = listOf(CatalogEntry("x", "t", "Свой"))), Progress(), d).nodes.single().caption,
    )

    @Test
    fun parseGarbageIsNull() = assertNull(parseCatalog("garbage"))

    @Test
    fun parseEmptyObjectIsEmpty() = assertEquals(emptyList(), parseCatalog("{}")?.lessons)

    // Language (09-02)

    private val py = listOf("kotlin", "python")
    private fun cat(vararg e: Pair<String, List<String>>) = Catalog(lessons = e.map { (id, l) -> CatalogEntry(id, id, langs = l) })
    private val kt = listOf("kotlin")

    @Test
    fun entryWithoutLangsIsKotlinOnly() =
        assertEquals(listOf("kotlin"), parseCatalog("""{"lessons":[{"id":"a","title":"t"}]}""")!!.lessons.single().langs)

    @Test
    fun pilotFreshPythonOnlyPythonLessonAvailable() {
        val c = cat("l1.1" to kt, "l1.2" to kt, "l1.4" to py, "l1.5" to kt)
        assertEquals(listOf(Soon, Soon, Available, Soon), homeModel(c, Progress(lang = CodeLang.Python), d).nodes.map { it.status })
    }

    @Test
    fun sharedProgressUnlocksNextAvailableOnPython() {
        // a passed on Kotlin counts; b is not on python and drops out; c follows a; d follows c (not passed).
        val c = cat("a" to py, "b" to kt, "c" to py, "d" to py)
        val prog = Progress(lang = CodeLang.Python, lessons = mapOf("a" to LessonProgress()))
        assertEquals(listOf(Done, Soon, Available, Locked), homeModel(c, prog, d).nodes.map { it.status })
    }

    @Test
    fun doneLessonWithoutPythonStaysDone() {
        val c = cat("a" to kt, "b" to py)
        val prog = Progress(lang = CodeLang.Python, lessons = mapOf("a" to LessonProgress()))
        // a is Done but off the python chain, so b is the first available lesson.
        assertEquals(listOf(Done, Available), homeModel(c, prog, d).nodes.map { it.status })
    }

    @Test
    fun dueSkipsLessonsWithoutPythonKeepingDue() {
        val c = cat("a" to kt, "b" to py)
        val prog = Progress(lang = CodeLang.Python, lessons = mapOf("a" to LessonProgress(0, d), "b" to LessonProgress(0, d)))
        assertEquals(listOf("b"), homeModel(c, prog, d).due)
        assertEquals(listOf("a", "b"), homeModel(c, prog.copy(lang = CodeLang.Kotlin), d).due)
        assertEquals(d, prog.lessons["a"]!!.due)
    }

    @Test
    fun sdTabIgnoresLanguage() {
        val sd = Catalog(lessons = listOf(CatalogEntry("sd1", "x"), CatalogEntry("sd2", "y"), CatalogEntry("sd3", "z")))
        val prog = Progress(lang = CodeLang.Python, lessons = mapOf("sd1" to LessonProgress(0, d)))
        val m = homeModel(sd, prog, d, lang = null)
        assertEquals(listOf(Done, Available, Locked), m.nodes.map { it.status })
        assertEquals(listOf("sd1"), m.due)
        assertEquals(homeModel(sd, prog.copy(lang = CodeLang.Kotlin), d), m)
    }

    @Test
    fun routeLangFallsBackToKotlinWhenLessonLacksLanguage() {
        val c = cat("l1.1" to kt, "l1.4" to py)
        // Kotlin-completed l1.1 is Done on Python Home; tapping it must not open a missing py file (CR-01).
        assertEquals(CodeLang.Kotlin, c.routeLang("l1.1", CodeLang.Python))
        assertEquals(CodeLang.Python, c.routeLang("l1.4", CodeLang.Python))
        assertEquals(CodeLang.Kotlin, c.routeLang("l1.4", CodeLang.Kotlin))
        // SD tab (no filter) always routes Kotlin (WR-05).
        assertEquals(CodeLang.Kotlin, Catalog(lessons = listOf(CatalogEntry("sd1", "x", langs = py))).routeLang("sd1", null))
    }
}
