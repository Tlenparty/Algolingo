package com.tlenparty.algolingo.progress.domain

fun interface CatalogRepository {
    /** A read or parse failure gives an empty catalog (Home empty state), never a crash. */
    suspend fun catalog(track: Track): Catalog
}
