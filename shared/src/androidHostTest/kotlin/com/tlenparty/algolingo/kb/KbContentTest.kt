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
            listOf(
                "Обзор", "Алгоритмы", "Kotlin / Java", "Многопоточность", "Корутины и Flow", "Compose", "Android",
                "Архитектура", "Performance", "KMP", "System Design", "Исключения", "Вопросы с собесов", "Общие вопросы",
            ),
            entries.map { it.section }.distinct(),
        )
    }

    @Test
    fun oldIdsStable() {
        assertTrue(entries.size >= 217, "entries=${entries.size}")
        val byId = entries.associateBy { it.id }
        // Phase 01.1 ids: gen_content.py reads kb/n07ed966c.md, links and nav state may hold the others.
        """n054ccc1e n056e6b1c n07ed966c n0875a2bf n08afb307 n17006052 n20407d85 n3aa07ae9 n3cf4f903 n3f4be5ed n3f836927
           n4e5e7ef4 n6f3f2844 n87785994 n87d0fd88 n9e62f72d n9e814012 nb2db8dca nd0ce2057 nde479513 ne8e12a31 ned5215b7
           nef21c1e1""".split(Regex("\\s+")).filter { it.isNotEmpty() }.also { assertEquals(23, it.size) }.forEach {
            assertEquals("Алгоритмы", byId[it]?.section, "id $it")
        }
    }

    private fun images(bs: List<Block>): List<ImageBlock> = bs.flatMap {
        when (it) {
            is ImageBlock -> listOf(it)
            is Callout -> images(it.blocks)
            is Quote -> images(it.blocks)
            else -> emptyList()
        }
    }

    @Test
    fun imagesBundled() {
        val used = entries.flatMap { images(parseBlocks(File(dir, it.file).readText())) }.map { it.file }.toSet()
        assertTrue(used.size >= 25, "images=${used.size}")
        used.forEach { assertTrue(File(dir, "img/$it").exists(), "missing img/$it") }
        val bundled = File(dir, "img").list()!!.toSet()
        assertEquals(emptySet(), bundled - used, "orphan images")
    }

    @Test
    fun searchSpansFolders() {
        val notes = entries.map { Note(it.id, it.title, it.section, File(dir, it.file).readText()) }
        val sections = searchNotes(notes, "корутин").map { it.note.section }.toSet()
        assertTrue(sections.size >= 2, "sections=$sections")
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
        "kpi (2)", "extension для list", "spec", "00 remote worldwide — чеклист", "01 linkedin — итоговый профиль",
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
