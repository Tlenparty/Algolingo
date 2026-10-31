package com.tlenparty.algolingo.kb.domain

data class Note(val id: String, val title: String, val section: String, val raw: String) {
    /** Cached once so search per keystroke is a plain indexOf (not part of equals). */
    val lowerTitle by lazy { title.lowercase() }
    val lowerRaw by lazy { raw.lowercase() }
}
