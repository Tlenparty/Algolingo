package com.tlenparty.algolingo.kb

import algolingo.shared.generated.resources.Res
import kotlinx.serialization.json.Json

class KbRepository {
    var notes: List<Note> = emptyList()
        private set

    suspend fun load() {
        val index = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<KbEntry>>(Res.readBytes("files/kb/index.json").decodeToString())
        notes = index.map {
            Note(it.id, it.title, it.section, Res.readBytes("files/kb/${it.file}").decodeToString())
        }
    }

    fun byId(id: String): Note? = notes.firstOrNull { it.id == id }

    /** Case-insensitive title lookup; null if absent (never throws). */
    fun resolve(target: String): Note? = resolveNote(notes, target)

    fun search(q: String): List<SearchHit> = searchNotes(notes, q)
}

data class SearchHit(val note: Note, val snippet: String)

/** Plain substring search (no regex from user input): title hits first, then body hits. */
fun searchNotes(notes: List<Note>, query: String): List<SearchHit> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val (titleHits, rest) = notes.partition { it.title.lowercase().contains(q) }
    val bodyHits = rest.mapNotNull { n ->
        val i = n.raw.lowercase().indexOf(q)
        if (i < 0) null else SearchHit(
            n, n.raw.substring((i - 30).coerceAtLeast(0), (i + q.length + 30).coerceAtMost(n.raw.length))
                .replace('\n', ' ').trim(),
        )
    }
    return titleHits.map { SearchHit(it, "") } + bodyHits
}

/** Strips `|alias` and `#heading`, then matches the title case-insensitively. */
fun resolveNote(notes: List<Note>, target: String): Note? {
    val t = target.substringBefore('|').substringBefore('#').trim().lowercase()
    return notes.firstOrNull { it.title.trim().lowercase() == t }
}

fun findAnchorIndex(blocks: List<Block>, anchor: String): Int? {
    val a = anchor.trim().lowercase()
    return blocks.indexOfFirst { it is Heading && it.text.trim().lowercase() == a }.takeIf { it >= 0 }
}
