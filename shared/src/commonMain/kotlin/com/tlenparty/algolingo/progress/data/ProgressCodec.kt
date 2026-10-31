package com.tlenparty.algolingo.progress.data

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.progress.domain.LessonProgress
import com.tlenparty.algolingo.progress.domain.PROGRESS_SCHEMA
import com.tlenparty.algolingo.progress.domain.Progress
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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

/** The progress.json format: same field names and defaults as every build so far. */
@Serializable
data class ProgressDto(
    val schemaVersion: Int = PROGRESS_SCHEMA,
    val xp: Int = 0,
    val streak: Int = 0,
    val lastActiveDay: LocalDate? = null,
    val lessons: Map<String, LessonProgressDto> = emptyMap(),
    @Serializable(with = CodeLangSerializer::class) val lang: CodeLang = CodeLang.Kotlin,
)

@Serializable
data class LessonProgressDto(val step: Int = 0, val due: LocalDate? = null)

private fun Progress.toDto() =
    ProgressDto(schemaVersion, xp, streak, lastActiveDay, lessons.mapValues { (_, l) -> LessonProgressDto(l.step, l.due) }, lang)

private fun ProgressDto.toDomain() =
    Progress(schemaVersion, xp, streak, lastActiveDay, lessons.mapValues { (_, l) -> LessonProgress(l.step, l.due) }, lang)

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

fun encodeProgress(p: Progress): String = json.encodeToString(ProgressDto.serializer(), p.toDto())

/** Upgrades a raw document written with schemaVersion [from] to [PROGRESS_SCHEMA] before typed decoding. v1 is current: nothing to do. */
fun migrate(doc: JsonObject, from: Int): JsonObject = when (from) {
    else -> doc
}

/** Null on any bad input; never throws. A missing schemaVersion means v1. */
fun decodeProgress(s: String): Progress? = try {
    val doc = json.parseToJsonElement(s).jsonObject
    json.decodeFromJsonElement(ProgressDto.serializer(), migrate(doc, doc["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 1)).toDomain()
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}
