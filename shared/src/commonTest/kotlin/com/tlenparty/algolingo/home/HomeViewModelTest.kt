package com.tlenparty.algolingo.home

import com.tlenparty.algolingo.FakeProgressRepository
import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.home.domain.BuildHomeModelUseCase
import com.tlenparty.algolingo.home.presentation.HomeAction
import com.tlenparty.algolingo.home.presentation.HomeEffect
import com.tlenparty.algolingo.home.presentation.HomeUiState
import com.tlenparty.algolingo.home.presentation.HomeViewModel
import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.progress.domain.CatalogEntry
import com.tlenparty.algolingo.progress.domain.LessonProgress
import com.tlenparty.algolingo.progress.domain.LessonStatus
import com.tlenparty.algolingo.progress.domain.Progress
import com.tlenparty.algolingo.progress.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val py = listOf("kotlin", "python")
    private val catalog = Catalog(listOf(CatalogEntry("a", "A", langs = py), CatalogEntry("b", "B")))
    private val day = LocalDate(2026, 10, 31)

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(progress: FakeProgressRepository, track: Track = Track.Code) =
        HomeViewModel(track, progress, { catalog }, BuildHomeModelUseCase())

    private fun content(vm: HomeViewModel) = assertIs<HomeUiState.Content>(vm.state.value)

    @Test fun loadingUntilProgressArrives() {
        val progress = FakeProgressRepository(start = null)
        val vm = vm(progress)
        assertIs<HomeUiState.Loading>(vm.state.value)
        progress.progress.value = Progress()
        assertEquals(listOf(LessonStatus.Available, LessonStatus.Locked), content(vm).model.nodes.map { it.status })
    }

    @Test fun startLessonOpensInSelectedLanguage() = runTest {
        val vm = vm(FakeProgressRepository(Progress(lang = CodeLang.Python)))
        vm.on(HomeAction.StartLesson("a"))
        assertEquals(HomeEffect.OpenLesson("a", false, CodeLang.Python), vm.effects.first())
    }

    @Test fun lessonWithoutSelectedLanguageOpensInKotlin() = runTest {
        val vm = vm(FakeProgressRepository(Progress(lang = CodeLang.Python)))
        vm.on(HomeAction.StartLesson("b"))
        assertEquals(HomeEffect.OpenLesson("b", false, CodeLang.Kotlin), vm.effects.first())
    }

    @Test fun systemDesignIgnoresLanguage() = runTest {
        val vm = vm(FakeProgressRepository(Progress(lang = CodeLang.Python)), Track.SystemDesign)
        vm.on(HomeAction.StartLesson("a"))
        assertEquals(HomeEffect.OpenLesson("a", false, CodeLang.Kotlin), vm.effects.first())
    }

    @Test fun reviewOpensFirstDueLesson() = runTest {
        val vm = vm(FakeProgressRepository(Progress(lessons = mapOf("a" to LessonProgress(0, day)))))
        vm.on(HomeAction.StartReview)
        assertEquals(HomeEffect.OpenLesson("a", true, CodeLang.Kotlin), vm.effects.first())
    }

    @Test fun refreshDateRebuildsDueList() {
        val progress = FakeProgressRepository(Progress(lessons = mapOf("a" to LessonProgress(0, LocalDate(2026, 11, 1)))))
        val vm = vm(progress)
        assertEquals(emptyList(), content(vm).model.due)
        progress.day = LocalDate(2026, 11, 1)
        vm.on(HomeAction.RefreshDate)
        assertEquals(listOf("a"), content(vm).model.due)
    }

    @Test fun selectLangWritesProgressOnCodeTrackOnly() {
        val progress = FakeProgressRepository()
        vm(progress, Track.SystemDesign).on(HomeAction.SelectLang(CodeLang.Python))
        assertEquals(0, progress.writes)
        val vm = vm(progress)
        vm.on(HomeAction.SelectLang(CodeLang.Python))
        assertEquals(CodeLang.Python, content(vm).lang)
    }
}
