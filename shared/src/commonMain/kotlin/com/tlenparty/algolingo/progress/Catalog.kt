package com.tlenparty.algolingo.progress

import algolingo.shared.generated.resources.Res
import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

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

private val json = Json { ignoreUnknownKeys = true }

/** Null on any bad input; never throws. A repeated id keeps its first entry (Home's list keys must be unique). */
fun parseCatalog(text: String): Catalog? = try {
    json.decodeFromString(Catalog.serializer(), text).let { it.copy(lessons = it.lessons.distinctBy(CatalogEntry::id)) }
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

const val CATALOG_PATH = "files/content/index.json"
const val SD_CATALOG_PATH = "files/content/sd/index.json"

/** Any read/parse failure gives an empty catalog (Home empty state), never a crash. */
suspend fun loadCatalog(path: String = CATALOG_PATH): Catalog = try {
    parseCatalog(Res.readBytes(path).decodeToString()) ?: Catalog()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    Catalog()
}

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

data class HomeNode(val id: String, val caption: String, val status: LessonStatus)

data class HomeModel(val streak: Int, val xp: Int, val nodes: List<HomeNode>, val due: List<String>)

/** [lang] null = no language filter (SD tab). */
fun homeModel(catalog: Catalog, p: Progress, today: LocalDate, lang: CodeLang? = p.lang): HomeModel {
    val ids = catalog.lessons.map { it.id }
    val inLang = catalog.lessons.filter { it.has(lang) }.mapTo(HashSet()) { it.id }
    val available = { id: String -> id in inLang }
    val nodes = catalog.lessons.zip(lessonStatuses(ids, p, available)) { e, s -> HomeNode(e.id, e.caption ?: (e.id.removePrefix("l") + " " + e.title), s) }
    return HomeModel(visibleStreak(p, today), p.xp, nodes, dueIds(ids, p, today, available))
}

/**
 * Language a lesson opens in: [lang] if the lesson ships in it, else Kotlin (always shipped).
 * So a Done lesson without the selected language replays in Kotlin, and SD ([lang] null) always routes Kotlin.
 */
fun Catalog.routeLang(id: String, lang: CodeLang?): CodeLang =
    if (lang != null && lessons.any { it.id == id && lang.id in it.langs }) lang else CodeLang.Kotlin
