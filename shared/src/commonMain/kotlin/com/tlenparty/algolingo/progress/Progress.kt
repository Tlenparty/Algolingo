package com.tlenparty.algolingo.progress

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.lesson.Grade
import com.tlenparty.algolingo.lesson.LessonResult
import com.tlenparty.algolingo.lesson.xpFor
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Pure progress engine: no IO, no clock reads. `today` is always injected.

/** One versioned document. A key in [lessons] means the lesson was completed at least once. */
@Serializable
data class Progress(
    val schemaVersion: Int = PROGRESS_SCHEMA,
    val xp: Int = 0,
    val streak: Int = 0,
    val lastActiveDay: LocalDate? = null,
    val lessons: Map<String, LessonProgress> = emptyMap(),
    @Serializable(with = CodeLangSerializer::class) val lang: CodeLang = CodeLang.Kotlin,
)

/** Stored as CodeLang.id; an unknown id or a non-string value (null, 3, {}) reads as Kotlin so progress is never reset over it. */
object CodeLangSerializer : KSerializer<CodeLang> {
    override val descriptor = PrimitiveSerialDescriptor("CodeLang", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: CodeLang) = encoder.encodeString(value.id)
    override fun deserialize(decoder: Decoder): CodeLang {
        val id = if (decoder is JsonDecoder) (decoder.decodeJsonElement() as? JsonPrimitive)?.contentOrNull else decoder.decodeString()
        return CodeLang.entries.firstOrNull { it.id == id } ?: CodeLang.Kotlin
    }
}

/** Leitner position. due == null means mastered. */
@Serializable
data class LessonProgress(val step: Int = 0, val due: LocalDate? = null)

// encodeDefaults so schemaVersion is always written; ignoreUnknownKeys so newer files load in older builds.
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun encodeProgress(p: Progress): String = json.encodeToString(Progress.serializer(), p)

const val PROGRESS_SCHEMA = 1

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

val INTERVALS = listOf(1, 3, 7)

private fun LocalDate.plusDays(n: Int) = plus(n, DateTimeUnit.DAY)

/** First pass (any grade) starts the ladder at +1 day. Reviews only count when due (A2). */
fun schedule(prev: LessonProgress?, grade: Grade, today: LocalDate): LessonProgress = when {
    prev == null -> LessonProgress(0, today.plusDays(1))
    prev.due == null || prev.due > today -> prev // mastered, replay before due, or clock moved back
    grade == Grade.Clean ->
        if (prev.step >= INTERVALS.lastIndex) LessonProgress(INTERVALS.size, null)
        else LessonProgress(prev.step + 1, today.plusDays(INTERVALS[prev.step + 1]))
    grade == Grade.Hinted -> prev.copy(due = today.plusDays(INTERVALS[prev.step.coerceIn(INTERVALS.indices)]))
    else -> LessonProgress(0, today.plusDays(1))
}

// ponytail: a clock set ahead during a lesson stores a future lastActiveDay/due that freezes the streak and the review until that date; clamp stored dates > today+2 back to today on read if this ever bites.
fun bumpStreak(p: Progress, today: LocalDate): Progress {
    val last = p.lastActiveDay
    val s = when {
        last == null -> 1
        last >= today -> maxOf(p.streak, 1) // same day, or clock moved back: no change
        last.plusDays(1) == today -> p.streak + 1
        else -> 1
    }
    return p.copy(streak = s, lastActiveDay = if (last != null && last > today) last else today)
}

/** What Home shows: a streak whose last day is before yesterday is already broken. */
fun visibleStreak(p: Progress, today: LocalDate): Int =
    p.lastActiveDay?.takeIf { it >= today.minus(1, DateTimeUnit.DAY) }?.let { p.streak } ?: 0

fun applyLessonResult(p: Progress, id: String, r: LessonResult, today: LocalDate): Progress =
    bumpStreak(p, today).let {
        it.copy(xp = it.xp + xpFor(r), lessons = it.lessons + (id to schedule(it.lessons[id], r.grade, today)))
    }
