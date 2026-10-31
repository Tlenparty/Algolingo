package com.tlenparty.algolingo.progress.data

import algolingo.shared.generated.resources.Res
import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.progress.domain.CatalogEntry
import com.tlenparty.algolingo.progress.domain.CatalogRepository
import com.tlenparty.algolingo.progress.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** files/content/index.json. The generator writes the same format. */
@Serializable
data class CatalogEntryDto(
    val id: String,
    val title: String,
    val caption: String? = null,
    /** CodeLang ids this lesson ships in; the generator adds "python" when a py sidecar exists. */
    val langs: List<String> = listOf("kotlin"),
)

@Serializable
data class CatalogDto(val schema: Int = 1, val lessons: List<CatalogEntryDto> = emptyList())

fun CatalogDto.toDomain() = Catalog(lessons.map { CatalogEntry(it.id, it.title, it.caption, it.langs) })

private val json = Json { ignoreUnknownKeys = true }

/** Null on any bad input; never throws. A repeated id keeps its first entry (Home's list keys must be unique). */
fun parseCatalog(text: String): Catalog? = try {
    json.decodeFromString(CatalogDto.serializer(), text).toDomain().let { it.copy(lessons = it.lessons.distinctBy(CatalogEntry::id)) }
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

/** Each catalog is read once per app run. */
class CatalogRepositoryImpl(private val load: suspend (String) -> Catalog = ::loadCatalog) : CatalogRepository {
    private val loaded = mutableMapOf<Track, Catalog>()

    override suspend fun catalog(track: Track): Catalog =
        loaded[track] ?: load(if (track == Track.Code) CATALOG_PATH else SD_CATALOG_PATH).also { loaded[track] = it }
}
