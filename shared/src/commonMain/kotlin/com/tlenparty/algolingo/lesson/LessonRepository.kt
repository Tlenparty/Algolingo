package com.tlenparty.algolingo.lesson

import algolingo.shared.generated.resources.Res
import com.tlenparty.algolingo.design.CodeLang
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

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

/** Only fixed folders; the id comes from a route, never a free path. SD lessons ignore the language. The single language branch for content. */
internal fun lessonPath(id: String, lang: CodeLang): String = when {
    id.startsWith("sd") -> "files/content/sd/$id.json"
    else -> when (lang) {
        CodeLang.Kotlin -> "files/content/lessons/$id.json"
        CodeLang.Python -> "files/content/lessons/py/$id.json"
    }
}

suspend fun loadLesson(id: String, lang: CodeLang): Result<Lesson> {
    val text = try {
        Res.readBytes(lessonPath(id, lang)).decodeToString()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) { // missing file, platform IO, resource reader state: all go to the error screen
        return Result.failure(e)
    }
    return parseLesson(text)
}
