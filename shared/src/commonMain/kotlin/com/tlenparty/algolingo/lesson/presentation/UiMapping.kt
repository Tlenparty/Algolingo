package com.tlenparty.algolingo.lesson.presentation

import com.tlenparty.algolingo.lesson.domain.CheckResult
import com.tlenparty.algolingo.lesson.domain.Exercise
import com.tlenparty.algolingo.lesson.domain.ExerciseState
import com.tlenparty.algolingo.lesson.domain.HoleResult
import com.tlenparty.algolingo.lesson.domain.Verdict

// Pure UI-state mapping; screens only render these. Never throws on malformed exercises.

enum class HoleVisual { Empty, EmptySelected, Filled, Ok, Acceptable, Wrong }

fun holeVisual(h: Int, state: ExerciseState, check: CheckResult?): HoleVisual =
    when (check?.holes?.getOrNull(h)) {
        HoleResult.Ok -> HoleVisual.Ok
        is HoleResult.Acceptable -> HoleVisual.Acceptable
        is HoleResult.Wrong -> HoleVisual.Wrong
        null -> when {
            state.placed.getOrNull(h) != null -> HoleVisual.Filled
            state.selected == h -> HoleVisual.EmptySelected
            else -> HoleVisual.Empty
        }
    }

enum class PanelKind { Success, Amber, Danger }

data class PanelItem(val chip: String, val text: String, val correct: String? = null)

data class PanelModel(
    val kind: PanelKind,
    val title: String,
    val button: String,
    val wrong: List<PanelItem>,
    val acceptable: List<PanelItem>,
)

fun panelModel(check: CheckResult): PanelModel {
    val acc = check.holes.filterIsInstance<HoleResult.Acceptable>().map { PanelItem(it.text, it.note) }
    // correct goes through PanelItem.correct (rendered as code), never interpolated into markup.
    val wrong = check.holes.filterIsInstance<HoleResult.Wrong>().map {
        PanelItem(it.text, it.why ?: "Здесь нужно другое.", it.correct)
    }
    return when (check.verdict) {
        Verdict.Ok -> PanelModel(PanelKind.Success, "✅ Отлично!", "Дальше", emptyList(), emptyList())
        Verdict.Acceptable -> PanelModel(PanelKind.Amber, "👌 Принято, но…", "Дальше", emptyList(), acc)
        Verdict.Wrong -> PanelModel(PanelKind.Danger, "❌ Не совсем", "Понятно, дальше", wrong, acc)
    }
}

sealed interface Piece {
    data class Code(val s: String) : Piece
    data class HoleAt(val h: Int) : Piece
}

/** Lines of pieces; a hole follows segment i (except after the last) on the same line. */
fun templateLines(segments: List<String>): List<List<Piece>> {
    val lines = mutableListOf<MutableList<Piece>>(mutableListOf())
    segments.forEachIndexed { i, seg ->
        seg.split('\n').forEachIndexed { k, part ->
            if (k > 0) lines += mutableListOf<Piece>()
            if (part.isNotEmpty()) lines.last() += Piece.Code(part)
        }
        if (i < segments.lastIndex) lines.last() += Piece.HoleAt(i)
    }
    return lines
}

/** One range per hole into the reference code (segments + ok[0]); empty range when ok is missing. */
fun solutionSpans(ex: Exercise): List<IntRange> {
    var pos = 0
    return ex.holes.mapIndexed { i, hole ->
        pos += ex.segments.getOrNull(i)?.length ?: 0
        val len = hole.ok.firstOrNull()?.length ?: 0
        (pos until pos + len).also { pos += len }
    }
}

fun hintsRemaining(ex: Exercise, shown: Int): Int = (ex.hints.size - shown).coerceAtLeast(0)

// D-A2: Reveal gating lives here (last queue item).
fun isLastExercise(state: LessonState): Boolean = state.queue.size == 1

sealed interface ScreenKey {
    data class Theory(val index: Int) : ScreenKey
    data class Exercise(val id: String, val play: Int) : ScreenKey
    data class Reveal(val id: String) : ScreenKey
    data object Done : ScreenKey
}

/** Stable across Solving -> Feedback of one play (Check bumps attempts), changes on requeue. */
fun screenKey(state: LessonState): ScreenKey {
    val id = state.queue.firstOrNull() ?: ""
    val n = state.attempts[id] ?: 0
    return when (val p = state.phase) {
        is Phase.Theory -> ScreenKey.Theory(p.index)
        Phase.Solving -> ScreenKey.Exercise(id, n)
        is Phase.Feedback -> ScreenKey.Exercise(id, (n - 1).coerceAtLeast(0))
        is Phase.Reveal -> ScreenKey.Reveal(id)
        Phase.Done -> ScreenKey.Done
    }
}
