package com.tlenparty.algolingo.progress.data

import algolingo.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.progress.domain.CatalogEntry

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
