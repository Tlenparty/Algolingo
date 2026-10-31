package com.tlenparty.algolingo.kb.domain

import com.tlenparty.algolingo.core.markdown.Block
import com.tlenparty.algolingo.core.markdown.Heading

data class SearchHit(val note: Note, val snippet: String)

/** Plain substring search (no regex from user input): title hits first, then body hits. */
fun searchNotes(notes: List<Note>, query: String): List<SearchHit> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val (titleHits, rest) = notes.partition { it.lowerTitle.contains(q) }
    val bodyHits = rest.mapNotNull { n ->
        val i = n.lowerRaw.indexOf(q)
        // lowercase() can lengthen the string («İ» -> 2 chars), so clamp both ends to raw.
        val start = (i - 30).coerceIn(0, n.raw.length)
        val end = (i + q.length + 30).coerceIn(start, n.raw.length)
        if (i < 0) null else SearchHit(n, n.raw.substring(start, end).replace('\n', ' ').trim())
    }
    return titleHits.map { SearchHit(it, "") } + bodyHits
}

/** Strips `|alias` and `#heading`, then matches the title case-insensitively. */
fun resolveNote(notes: List<Note>, target: String): Note? {
    val t = target.substringBefore('|').substringBefore('#').trim().lowercase()
    return notes.firstOrNull { it.title.trim().lowercase() == t }
}

/** Matches an Obsidian anchor (heading text) or a GitHub-style slug (`#1-hash-map--частоты`). */
fun findAnchorIndex(blocks: List<Block>, anchor: String): Int? {
    val a = anchor.trim().lowercase()
    return blocks.indexOfFirst {
        it is Heading && it.text.trim().lowercase().let { h -> h == a || slug(h) == a }
    }.takeIf { it >= 0 }
}

/** GitHub slug: drop everything except letters, digits, `-`, `_`, spaces; spaces -> `-`. */
fun slug(heading: String): String =
    heading.trim().lowercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == ' ' }.replace(' ', '-')
