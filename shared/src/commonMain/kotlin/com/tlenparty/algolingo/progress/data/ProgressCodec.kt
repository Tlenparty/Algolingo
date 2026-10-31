package com.tlenparty.algolingo.progress.data

import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.tlenparty.algolingo.progress.domain.Progress

/** Stored as CodeLang.id; an unknown id or a non-string value (null, 3, {}) reads as Kotlin so progress is never reset over it. */
object CodeLangSerializer : KSerializer<CodeLang> {
    override val descriptor = PrimitiveSerialDescriptor("CodeLang", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: CodeLang) = encoder.encodeString(value.id)
    override fun deserialize(decoder: Decoder): CodeLang {
        val id = if (decoder is JsonDecoder) (decoder.decodeJsonElement() as? JsonPrimitive)?.contentOrNull else decoder.decodeString()
        return CodeLang.entries.firstOrNull { it.id == id } ?: CodeLang.Kotlin
    }
}

// encodeDefaults so schemaVersion is always written; ignoreUnknownKeys so newer files load in older builds.
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun encodeProgress(p: Progress): String = json.encodeToString(Progress.serializer(), p)

/** Upgrades a raw document written with schemaVersion [from] to [PROGRESS_SCHEMA] before typed decoding. v1 is current: nothing to do. */
fun migrate(doc: JsonObject, from: Int): JsonObject = when (from) {
    else -> doc
}

/** Null on any bad input; never throws. A missing schemaVersion means v1. */
fun decodeProgress(s: String): Progress? = try {
    val doc = json.parseToJsonElement(s).jsonObject
    json.decodeFromJsonElement(Progress.serializer(), migrate(doc, doc["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1))
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}
