package com.tlenparty.algolingo.progress

import algolingo.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** files/content/index.json: lessons in unlock order. Phase 5's generator writes the same format. */
@Serializable
data class CatalogEntry(val id: String, val title: String, val caption: String? = null)

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

enum class LessonStatus { Done, Available, Locked }

/** Any pass (even ❌, A3) marks Done and unlocks the next id. */
fun lessonStatuses(ids: List<String>, p: Progress): List<LessonStatus> = ids.mapIndexed { i, id ->
    when {
        id in p.lessons -> LessonStatus.Done
        i == 0 || ids[i - 1] in p.lessons -> LessonStatus.Available
        else -> LessonStatus.Locked
    }
}

/** Catalog order; ids absent from the catalog are ignored. */
fun dueIds(ids: List<String>, p: Progress, today: LocalDate): List<String> =
    ids.filter { id -> p.lessons[id]?.due?.let { it <= today } == true }

data class HomeNode(val id: String, val caption: String, val status: LessonStatus)

data class HomeModel(val streak: Int, val xp: Int, val nodes: List<HomeNode>, val due: List<String>)

fun homeModel(catalog: Catalog, p: Progress, today: LocalDate): HomeModel {
    val ids = catalog.lessons.map { it.id }
    val nodes = catalog.lessons.zip(lessonStatuses(ids, p)) { e, s -> HomeNode(e.id, e.caption ?: (e.id.removePrefix("l") + " " + e.title), s) }
    return HomeModel(visibleStreak(p, today), p.xp, nodes, dueIds(ids, p, today))
}
