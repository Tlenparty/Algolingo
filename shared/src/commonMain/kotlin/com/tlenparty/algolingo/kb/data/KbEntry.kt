package com.tlenparty.algolingo.kb.data

import kotlinx.serialization.Serializable

@Serializable
data class KbEntry(val id: String, val title: String, val section: String, val file: String)
