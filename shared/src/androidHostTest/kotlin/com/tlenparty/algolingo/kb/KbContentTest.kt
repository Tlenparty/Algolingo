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

    private fun countCallouts(bs: List<Block>): Int = bs.sumOf {
        when (it) {
            is Callout -> 1 + countCallouts(it.blocks)
            is Quote -> countCallouts(it.blocks)
            else -> 0
        }
    }

    @Test
    fun allNotesParse() {
        var callouts = 0
        entries.forEach { e ->
            val blocks = parseBlocks(File(dir, e.file).readText())
            assertTrue(blocks.isNotEmpty(), "empty ${e.id}")
            callouts += countCallouts(blocks)
            blocks.forEach {
                when (it) {
                    is Paragraph -> parseInline(it.text)
                    is ListItem -> parseInline(it.text)
                    is Heading -> parseInline(it.text)
                    is Table -> (listOf(it.header) + it.rows).flatten().forEach(::parseInline)
                    else -> {}
                }
            }
        }
        assertTrue(callouts > 100, "callouts=$callouts")
    }
}
