package com.tlenparty.algolingo.kb

import java.io.File
import java.text.Normalizer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KbContentTest {
    private val dir = File("src/commonMain/composeResources/files/kb")
    private val entries: List<KbEntry> =
        Json { ignoreUnknownKeys = true }.decodeFromString(File(dir, "index.json").readText())

    @Test
    fun indexIsConsistent() {
        assertEquals(23, entries.size)
        entries.forEach {
            assertTrue(File(dir, it.file).exists(), "missing ${it.file}")
            assertTrue(it.id.all { c -> c.code < 128 }, "non-ascii id ${it.id}")
            assertEquals(Normalizer.normalize(it.title, Normalizer.Form.NFC), it.title)
        }
    }

    @Test
    fun sectionsInOrder() {
        assertEquals(
            listOf("Навигация", "Тренажёры", "Задачи с собесов", "Темы"),
            entries.map { it.section }.distinct(),
        )
    }
}
