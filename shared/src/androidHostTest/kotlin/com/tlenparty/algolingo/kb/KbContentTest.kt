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
        assertTrue(entries.isNotEmpty())
        assertEquals(entries.size, entries.map { it.id }.toSet().size, "duplicate ids")
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

    private fun inlines(bs: List<Block>): List<Inline> = bs.flatMap {
        when (it) {
            is Paragraph -> parseInline(it.text)
            is ListItem -> parseInline(it.text)
            is Heading -> parseInline(it.text)
            is Table -> (listOf(it.header) + it.rows).flatten().flatMap(::parseInline)
            is Callout -> parseInline(it.title) + inlines(it.blocks)
            is Quote -> inlines(it.blocks)
            is Code, Rule, is ImageBlock -> emptyList()
        }
    }.flatMap { if (it is Mark) parseInline(it.v) else listOf(it) }

    // Wikilinks to vault notes that are not part of the bundle; they render as plain text.
    private val knownMissing = setOf(
        "coil", "hashmap vs hashset", "heap & stack", "performance. память и утечки", "как работает hashmap", "структуры данных",
    )

    @Test
    fun allNotesParseAndLinksResolve() {
        val notes = entries.map { Note(it.id, it.title, it.section, File(dir, it.file).readText()) }
        val parsed = notes.associate { it.id to parseBlocks(it.raw) }
        var callouts = 0
        val broken = ArrayList<String>()
        notes.forEach { n ->
            val blocks = parsed.getValue(n.id)
            assertTrue(blocks.isNotEmpty(), "empty ${n.id}")
            callouts += countCallouts(blocks)
            inlines(blocks).filterIsInstance<Link>().forEach { l ->
                val dest = if (l.target.startsWith("#")) n else resolveNote(notes, l.target)
                when {
                    dest == null -> if (l.target.trim().lowercase() !in knownMissing) broken += "${n.title}: [[${l.target}]]"
                    l.anchor != null && findAnchorIndex(parsed.getValue(dest.id), l.anchor!!) == null ->
                        broken += "${n.title}: ${l.target}#${l.anchor}"
                }
            }
        }
        assertTrue(broken.isEmpty(), "unresolved links: $broken")
        assertTrue(callouts > 100, "callouts=$callouts")
    }
}
