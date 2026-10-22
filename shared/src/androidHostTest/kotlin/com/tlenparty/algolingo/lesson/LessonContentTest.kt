package com.tlenparty.algolingo.lesson

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
            assertTrue(counts.zipWithNext().all { (a, b) -> b > a }, "$id hole counts $counts")
        }
    }
}
