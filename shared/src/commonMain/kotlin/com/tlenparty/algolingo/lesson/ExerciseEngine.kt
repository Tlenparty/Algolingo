package com.tlenparty.algolingo.lesson

// Hole-filling engine. State holds only Int bank indices (chip identity, EXER-05), never chip text.
// Phase 3 note: the "Filled + selected" visual is unreachable, because tapHole on a filled hole clears it.
// Functions never throw: out-of-range ids/holes return the state unchanged.

data class ExerciseState(val placed: List<Int?>, val selected: Int?, val order: List<Int>) {
    fun isPlaced(id: Int): Boolean = id in placed
}

fun initial(ex: Exercise, seed: Int) = ExerciseState(
    placed = List(ex.holes.size) { null },
    selected = if (ex.holes.isEmpty()) null else 0,
    order = seededShuffle(ex.bank.indices.toList(), seed),
)

fun tapChip(s: ExerciseState, id: Int): ExerciseState {
    if (id !in s.order || s.isPlaced(id)) return s
    val target = s.selected?.takeIf { it in s.placed.indices } ?: s.placed.indexOfFirst { it == null }
    if (target < 0) return s
    val placed = s.placed.toMutableList().also { it[target] = id }
    val n = placed.size
    val next = (1..n).map { (target + it) % n }.firstOrNull { placed[it] == null }
    return s.copy(placed = placed, selected = next)
}

fun tapHole(s: ExerciseState, h: Int): ExerciseState {
    if (h !in s.placed.indices) return s
    val placed = s.placed.toMutableList().also { it[h] = null }
    return s.copy(placed = placed, selected = h)
}

sealed interface HoleResult {
    data object Ok : HoleResult
    data class Acceptable(val text: String, val note: String) : HoleResult
    data class Wrong(val text: String, val why: String?, val correct: String) : HoleResult
}

enum class Verdict { Ok, Acceptable, Wrong }

data class CheckResult(val holes: List<HoleResult>) {
    val verdict: Verdict
        get() = when {
            holes.any { it is HoleResult.Wrong } -> Verdict.Wrong
            holes.any { it is HoleResult.Acceptable } -> Verdict.Acceptable
            else -> Verdict.Ok
        }
}

fun check(ex: Exercise, s: ExerciseState): CheckResult? {
    if (s.placed.size != ex.holes.size || s.placed.any { it == null }) return null
    val results = ex.holes.mapIndexed { h, hole ->
        val chip = ex.bank.getOrNull(s.placed[h]!!) ?: return null
        val text = chip.text
        val acc = hole.acceptable.firstOrNull { it.text == text }
        when {
            text in hole.ok -> HoleResult.Ok
            acc != null -> HoleResult.Acceptable(text, acc.note)
            else -> HoleResult.Wrong(text, hole.wrongWhy[text] ?: chip.why, hole.ok.firstOrNull() ?: "")
        }
    }
    return CheckResult(results)
}
