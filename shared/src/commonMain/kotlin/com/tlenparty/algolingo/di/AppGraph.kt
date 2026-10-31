package com.tlenparty.algolingo.di

import com.tlenparty.algolingo.progress.data.ProgressRepository
import com.tlenparty.algolingo.progress.data.progressStore

/**
 * One DataStore per file (two on one file crash), surviving Activity recreation. The app always passes the same
 * dataDir; a different one (a second e2e @Test with its own fresh dir) gets its own repository, kept for reuse.
 */
object AppGraph {
    // ponytail: main-thread only, no locking; App() is the sole caller.
    // ponytail: never evicts; one entry in the app, one per e2e @Test dir.
    private val progress = mutableMapOf<String, ProgressRepository>()

    /** Keyed by dir, so A -> B -> A reuses A's store instead of opening a second DataStore on A's file. */
    fun progress(dataDir: String): ProgressRepository =
        progress.getOrPut(dataDir) { ProgressRepository(progressStore("$dataDir/progress.json")) }
}
