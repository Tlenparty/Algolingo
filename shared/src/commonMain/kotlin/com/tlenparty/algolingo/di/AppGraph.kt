package com.tlenparty.algolingo.di

import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.home.domain.BuildHomeModelUseCase
import com.tlenparty.algolingo.home.presentation.HomeViewModel
import com.tlenparty.algolingo.kb.data.KbRepositoryImpl
import com.tlenparty.algolingo.kb.domain.KbRepository
import com.tlenparty.algolingo.kb.presentation.KbImages
import com.tlenparty.algolingo.kb.presentation.KbListViewModel
import com.tlenparty.algolingo.kb.presentation.NoteViewModel
import com.tlenparty.algolingo.lesson.data.LessonRepositoryImpl
import com.tlenparty.algolingo.lesson.domain.LoadLessonUseCase
import com.tlenparty.algolingo.lesson.presentation.LessonViewModel
import com.tlenparty.algolingo.progress.data.CatalogRepositoryImpl
import com.tlenparty.algolingo.progress.data.ProgressRepositoryImpl
import com.tlenparty.algolingo.progress.data.progressStore
import com.tlenparty.algolingo.progress.domain.CatalogRepository
import com.tlenparty.algolingo.progress.domain.ProgressRepository
import com.tlenparty.algolingo.progress.domain.RecordLessonResultUseCase
import com.tlenparty.algolingo.progress.domain.Track
import kotlin.time.Clock

/** The whole object graph, wired by hand: repositories live here, ViewModels are built per screen. */
class AppContainer(dataDir: String) {
    val progress: ProgressRepository = ProgressRepositoryImpl(progressStore("$dataDir/progress.json"))
    private val catalogs: CatalogRepository = CatalogRepositoryImpl()
    private val kb: KbRepository = KbRepositoryImpl()
    private val loadLesson = LoadLessonUseCase(LessonRepositoryImpl())
    private val recordResult = RecordLessonResultUseCase(progress)
    private val buildHome = BuildHomeModelUseCase()
    val kbImages = KbImages(kb)

    fun homeViewModel(track: Track) = HomeViewModel(track, progress, catalogs, buildHome)

    fun lessonViewModel(id: String, lang: CodeLang, review: Boolean) = LessonViewModel(
        id, seed = Clock.System.now().toEpochMilliseconds().toInt(), review = review, lang = lang,
        loadLesson = loadLesson, recordResult = recordResult,
    )

    fun kbListViewModel() = KbListViewModel(kb)

    fun noteViewModel(id: String) = NoteViewModel(id, kb)
}

/**
 * One DataStore per file (two on one file crash), surviving Activity recreation. The app always passes the same
 * dataDir; a different one (a second e2e @Test with its own fresh dir) gets its own container, kept for reuse.
 */
object AppGraph {
    // ponytail: main-thread only, no locking; App() is the sole caller.
    // ponytail: never evicts; one entry in the app, one per e2e @Test dir.
    private val containers = mutableMapOf<String, AppContainer>()

    /** Keyed by dir, so A -> B -> A reuses A's store instead of opening a second DataStore on A's file. */
    fun container(dataDir: String): AppContainer = containers.getOrPut(dataDir) { AppContainer(dataDir) }

    fun progress(dataDir: String): ProgressRepository = container(dataDir).progress
}
