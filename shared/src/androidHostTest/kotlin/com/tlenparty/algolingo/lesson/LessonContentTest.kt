package com.tlenparty.algolingo.lesson

import com.tlenparty.algolingo.design.CodeLang
import java.io.File
import kotlinx.serialization.json.Json
import com.tlenparty.algolingo.progress.Catalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonContentTest {
    private val file = File("src/commonMain/composeResources/files/content/lessons/l0.4.json")

    // strict: a typo in hand-written JSON must fail, not be ignored
    private val lesson = Json { ignoreUnknownKeys = false }.decodeFromString<Lesson>(file.readText())

    @Test
    fun l04IsValid() {
        assertEquals(emptyList(), validate(lesson))
        assertTrue(lesson.exercises.size in 2..3)
        assertEquals(listOf(1, 2, 4), lesson.exercises.map { it.holes.size })
    }

    @Test
    fun e3HasTwoSeparateOneChips() {
        assertEquals(2, lesson.exercises.last().bank.count { it.text == "1" })
    }

    @Test
    fun catalogIdsAreUniqueAndHaveLessonFiles() {
        val dir = File("src/commonMain/composeResources/files/content")
        // Lenient like the runtime (the generator may add fields), but no dedupe: duplicates in the shipped file must still fail here.
        val ids = Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(File(dir, "index.json").readText()).lessons.map { it.id }
        assertTrue(ids.isNotEmpty())
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue(File(dir, "lessons/$it.json").isFile, "missing lessons/$it.json") }
    }

    @Test
    fun everyCatalogLessonIsStrictAndValid() {
        val dir = File("src/commonMain/composeResources/files/content")
        val ids = Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(File(dir, "index.json").readText()).lessons.map { it.id }
        val strict = Json { ignoreUnknownKeys = false }
        ids.forEach { id ->
            val l = strict.decodeFromString<Lesson>(File(dir, "lessons/$id.json").readText())
            assertEquals(emptyList(), validate(l), id)
            assertEquals(id, l.id)
            val counts = l.exercises.map { it.holes.size }
            if (l.kind == LessonKind.Code) assertTrue(counts.zipWithNext().all { (a, b) -> b > a }, "$id hole counts $counts")
        }
    }

    /** Vacuous until 09-05 ships lessons/py; then every python lesson must parse + validate. */
    @Test
    fun everyPythonLessonParses() {
        File("src/commonMain/composeResources/files/content/lessons/py").listFiles { f -> f.extension == "json" }?.forEach { f ->
            assertTrue(parseLesson(f.readText()).isSuccess, "${f.name}: ${parseLesson(f.readText()).exceptionOrNull()}")
        }
    }

    /** Res.readBytes needs instrumentation on the host JVM, so read the same lessonPath from disk. */
    @Test
    fun pythonPilotLoadsBesideKotlin() {
        fun load(lang: CodeLang) = parseLesson(File("src/commonMain/composeResources/" + lessonPath("l1.4", lang)).readText()).getOrThrow()
        val py = load(CodeLang.Python)
        assertTrue("def two_sum" in py.solution.code)
        assertTrue(py.exercises.size in 2..3)
        assertTrue("fun twoSum" in load(CodeLang.Kotlin).solution.code)
    }

    @Test
    fun everySdLessonIsStrictValidDesign() {
        val dir = File("src/commonMain/composeResources/files/content")
        val entries = Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(File(dir, "sd/index.json").readText()).lessons
        val ids = entries.map { it.id }
        assertTrue(ids.isNotEmpty())
        assertEquals(ids.size, ids.toSet().size)
        entries.forEach { assertTrue(!it.caption.isNullOrBlank(), "${it.id}: caption") }
        val strict = Json { ignoreUnknownKeys = false }
        ids.forEach { id ->
            val f = File(dir, "sd/$id.json")
            assertTrue(f.isFile, "missing sd/$id.json")
            assertEquals("files/content/sd/$id.json", lessonPath(id, CodeLang.Python))
            val l = strict.decodeFromString<Lesson>(f.readText())
            assertEquals(LessonKind.Design, l.kind, id)
            assertEquals(emptyList(), validate(l), id)
            assertEquals(id, l.id)
        }
        val mainIds = Json { ignoreUnknownKeys = true }.decodeFromString<Catalog>(File(dir, "index.json").readText()).lessons.map { it.id }
        assertEquals(emptySet(), ids.toSet() intersect mainIds.toSet())
        assertTrue(ids.none { File(dir, "lessons/$it.json").exists() })
    }
}
