package com.tlenparty.algolingo.kb.data

import algolingo.shared.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import com.tlenparty.algolingo.kb.domain.KbRepository
import com.tlenparty.algolingo.kb.domain.Note
import com.tlenparty.algolingo.kb.domain.SearchHit
import com.tlenparty.algolingo.kb.domain.resolveNote
import com.tlenparty.algolingo.kb.domain.searchNotes

class KbRepositoryImpl(
    private val read: suspend (String) -> ByteArray = { Res.readBytes(it) },
) : KbRepository {
    override var notes: List<Note> = emptyList()
        private set
    private var loaded = false
    private val mutex = Mutex()

    /** Loads once; a failure (missing/corrupt index, no readable notes) is not cached, so the next open retries. */
    override suspend fun load(): Result<List<Note>> = mutex.withLock {
        if (loaded) return@withLock Result.success(notes)
        try {
            val index = json.decodeFromString<List<KbEntry>>(read("files/kb/index.json").decodeToString())
            val loadedNotes = index.mapNotNull {
                try {
                    Note(it.id, it.title, it.section, read("files/kb/${it.file}").decodeToString())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null // one broken note must not hide the rest
                }
            }
            if (loadedNotes.isEmpty()) return@withLock Result.failure(IllegalStateException("kb empty"))
            notes = loadedNotes
            loaded = true
            Result.success(loadedNotes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun byId(id: String): Note? = notes.firstOrNull { it.id == id }

    override fun resolve(target: String): Note? = resolveNote(notes, target)

    override fun search(q: String): List<SearchHit> = searchNotes(notes, q)

    override suspend fun image(file: String): ByteArray = read("files/kb/img/$file")
}

private val json = Json { ignoreUnknownKeys = true }
