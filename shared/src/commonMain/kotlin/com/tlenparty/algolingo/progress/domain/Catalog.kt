package com.tlenparty.algolingo.progress.domain

import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** files/content/index.json: lessons in unlock order. Phase 5's generator writes the same format. */
@Serializable
data class CatalogEntry(
    val id: String,
    val title: String,
    val caption: String? = null,
    /** CodeLang ids this lesson ships in; the generator adds "python" when a py sidecar exists. */
    val langs: List<String> = listOf("kotlin"),
) {
    /** [lang] null = no language filter (SD tab). */
    fun has(lang: CodeLang?) = lang == null || lang.id in langs
}

@Serializable
data class Catalog(val schema: Int = 1, val lessons: List<CatalogEntry> = emptyList())

/** Soon = not shipped in the selected language yet. */
enum class LessonStatus { Done, Available, Locked, Soon }

/**
 * Any pass (even ❌, A3), in any language, marks Done and unlocks the next id.
 * Lessons not [available] in the selected language drop out of the chain: Soon unless Done, and they unlock nothing.
 */
fun lessonStatuses(ids: List<String>, p: Progress, available: (String) -> Boolean = { true }): List<LessonStatus> {
    var prev: String? = null // previous available lesson
    return ids.map { id ->
        val avail = available(id)
        when {
            id in p.lessons -> LessonStatus.Done
            !avail -> LessonStatus.Soon
            prev == null || prev in p.lessons -> LessonStatus.Available
            else -> LessonStatus.Locked
        }.also { if (avail) prev = id }
    }
}

/** Catalog order; ids absent from the catalog or not [available] are ignored (their due date is untouched). */
fun dueIds(ids: List<String>, p: Progress, today: LocalDate, available: (String) -> Boolean = { true }): List<String> =
    ids.filter { id -> available(id) && p.lessons[id]?.due?.let { it <= today } == true }

/**
 * Language a lesson opens in: [lang] if the lesson ships in it, else Kotlin (always shipped).
 * So a Done lesson without the selected language replays in Kotlin, and SD ([lang] null) always routes Kotlin.
 */
fun Catalog.routeLang(id: String, lang: CodeLang?): CodeLang =
    if (lang != null && lessons.any { it.id == id && lang.id in it.langs }) lang else CodeLang.Kotlin
