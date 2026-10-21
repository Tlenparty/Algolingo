package com.tlenparty.algolingo.kb

import kotlinx.serialization.Serializable

@Serializable
data class KbEntry(val id: String, val title: String, val section: String, val file: String)

data class Note(val id: String, val title: String, val section: String, val raw: String)
