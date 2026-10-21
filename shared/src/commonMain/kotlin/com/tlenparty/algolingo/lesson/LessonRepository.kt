package com.tlenparty.algolingo.lesson

import algolingo.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.MissingResourceException

private val json = Json { ignoreUnknownKeys = true }

/** Malformed-content boundary: decode + validate, never throws on bad input. */
fun parseLesson(text: String): Result<Lesson> {
    val lesson = try {
        json.decodeFromString<Lesson>(text)
    } catch (e: SerializationException) {
        return Result.failure(e)
    } catch (e: IllegalArgumentException) {
        return Result.failure(e)
    }
    val errors = validate(lesson)
    return if (errors.isEmpty()) Result.success(lesson)
    else Result.failure(IllegalArgumentException(errors.joinToString("; ")))
}

suspend fun loadLesson(id: String): Result<Lesson> {
    val text = try {
        Res.readBytes("files/content/lessons/$id.json").decodeToString()
    } catch (e: CancellationException) {
        throw e
    } catch (e: MissingResourceException) {
        return Result.failure(e)
    }
    return parseLesson(text)
}
