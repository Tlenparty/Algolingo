package com.tlenparty.algolingo.kb.domain

interface KbRepository {
    val notes: List<Note>

    /** Loads once; a failure (missing/corrupt index, no readable notes) is not cached, so the next open retries. */
    suspend fun load(): Result<List<Note>>

    fun byId(id: String): Note?

    /** Case-insensitive title lookup; null if absent (never throws). */
    fun resolve(target: String): Note?

    fun search(q: String): List<SearchHit>

    /** Bytes of a bundled note image; throws if the file is missing. */
    suspend fun image(file: String): ByteArray
}
