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
    fun resolve(target: String): Note? {
        val t = target.trim().lowercase()
        return notes.firstOrNull { it.title.lowercase() == t }
    }
}
