package com.tlenparty.algolingo.kb

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.tlenparty.algolingo.kb.data.KbRepositoryImpl

class KbRepositoryTest {
    private val index = """[
        {"id":"a","title":"A","section":"S","file":"a.md"},
        {"id":"b","title":"B","section":"S","file":"b.md"}
    ]"""

    /** Fake reader: [broken] paths throw, [indexText] overrides the index. Counts index reads. */
    private class Reader(var indexText: String, var broken: Set<String> = emptySet()) {
        var indexReads = 0
        suspend fun read(path: String): ByteArray {
            if (path == "files/kb/index.json") indexReads++
            if (path in broken) error("missing $path")
            return if (path == "files/kb/index.json") indexText.encodeToByteArray() else "# $path".encodeToByteArray()
        }
    }

    @Test fun missingIndex() = runTest {
        val r = Reader(index, setOf("files/kb/index.json"))
        assertTrue(KbRepositoryImpl(r::read).load().isFailure)
    }

    @Test fun garbageIndex() = runTest {
        assertTrue(KbRepositoryImpl(Reader("not json")::read).load().isFailure)
    }

    @Test fun oneBrokenNoteKeepsOthers() = runTest {
        val repo = KbRepositoryImpl(Reader(index, setOf("files/kb/a.md"))::read)
        val res = repo.load()
        assertTrue(res.isSuccess)
        assertEquals(listOf("b"), repo.notes.map { it.id })
    }

    @Test fun allNotesBroken() = runTest {
        val r = Reader(index, setOf("files/kb/a.md", "files/kb/b.md"))
        assertTrue(KbRepositoryImpl(r::read).load().isFailure)
    }

    @Test fun idempotent() = runTest {
        val r = Reader(index)
        val repo = KbRepositoryImpl(r::read)
        assertTrue(repo.load().isSuccess)
        val second = repo.load()
        assertTrue(second.isSuccess)
        assertEquals(2, second.getOrThrow().size)
        assertEquals(1, r.indexReads)
    }

    @Test fun failureIsNotCached() = runTest {
        val r = Reader("not json")
        val repo = KbRepositoryImpl(r::read)
        assertTrue(repo.load().isFailure)
        r.indexText = index
        assertTrue(repo.load().isSuccess)
        assertEquals(2, repo.notes.size)
    }
}
