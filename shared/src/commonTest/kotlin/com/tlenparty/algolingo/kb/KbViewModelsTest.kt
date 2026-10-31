package com.tlenparty.algolingo.kb

import com.tlenparty.algolingo.kb.domain.KbRepository
import com.tlenparty.algolingo.kb.domain.Note
import com.tlenparty.algolingo.kb.domain.SearchHit
import com.tlenparty.algolingo.kb.domain.resolveNote
import com.tlenparty.algolingo.kb.presentation.KbListAction
import com.tlenparty.algolingo.kb.presentation.KbListUiState
import com.tlenparty.algolingo.kb.presentation.KbListViewModel
import com.tlenparty.algolingo.kb.presentation.NoteAction
import com.tlenparty.algolingo.kb.presentation.NoteEffect
import com.tlenparty.algolingo.kb.presentation.NoteUiState
import com.tlenparty.algolingo.kb.presentation.NoteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeKb(var fail: Boolean = false) : KbRepository {
    override val notes = listOf(Note("1", "Хеш", "S", "# Заголовок"))

    override suspend fun load() = if (fail) Result.failure(Exception("x")) else Result.success(notes)

    override fun byId(id: String) = notes.firstOrNull { it.id == id }

    override fun resolve(target: String) = resolveNote(notes, target)

    override fun search(q: String) = notes.map { SearchHit(it, "") }

    override suspend fun image(file: String) = ByteArray(0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class KbViewModelsTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test fun failedLoadIsRetriedOnNextOpen() {
        val kb = FakeKb(fail = true)
        val vm = KbListViewModel(kb)
        vm.on(KbListAction.Load)
        assertIs<KbListUiState.Error>(vm.state.value)
        kb.fail = false
        vm.on(KbListAction.Load)
        assertEquals(kb.notes, assertIs<KbListUiState.Content>(vm.state.value).notes)
    }

    @Test fun blankQueryClearsSearch() {
        val vm = KbListViewModel(FakeKb())
        vm.on(KbListAction.Load)
        vm.on(KbListAction.QueryChanged("  "))
        assertEquals(null, assertIs<KbListUiState.Content>(vm.state.value).search)
    }

    @Test fun noteLoadsWithBlocks() {
        val vm = NoteViewModel("1", FakeKb())
        vm.on(NoteAction.Load)
        assertEquals(1, assertIs<NoteUiState.Content>(vm.state.value).blocks.size)
    }

    @Test fun unknownNoteIsNotFound() {
        val vm = NoteViewModel("nope", FakeKb())
        vm.on(NoteAction.Load)
        assertIs<NoteUiState.NotFound>(vm.state.value)
    }

    @Test fun brokenKbShowsNoteError() {
        val vm = NoteViewModel("1", FakeKb(fail = true))
        vm.on(NoteAction.Load)
        assertIs<NoteUiState.Error>(vm.state.value)
    }

    @Test fun linkOpensResolvedNoteWithAnchor() = runTest {
        val vm = NoteViewModel("1", FakeKb())
        vm.on(NoteAction.FollowLink("хеш", "a"))
        assertEquals(NoteEffect.OpenNote("1", "a"), vm.effects.first())
    }
}
