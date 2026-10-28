package com.tlenparty.algolingo.lesson.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tlenparty.algolingo.lesson.ExerciseResult
import com.tlenparty.algolingo.lesson.LessonAction
import com.tlenparty.algolingo.lesson.LessonKind
import com.tlenparty.algolingo.lesson.assembled
import com.tlenparty.algolingo.lesson.LessonState
import com.tlenparty.algolingo.lesson.isLastExercise
import com.tlenparty.algolingo.lesson.screenKey
import com.tlenparty.algolingo.lesson.solutionSpans
import com.tlenparty.algolingo.lesson.xpFor
import com.tlenparty.algolingo.design.components.ChunkyButton
import com.tlenparty.algolingo.design.components.CodeBlock
import com.tlenparty.algolingo.ui.components.richText
import com.tlenparty.algolingo.design.AlgoTheme

@Composable
fun RevealScreen(state: LessonState, solved: Boolean, on: (LessonAction) -> Unit, modifier: Modifier = Modifier) {
    val t = AlgoTheme.typography
    val c = AlgoTheme.colors
    val ink = MaterialTheme.colorScheme.onBackground
    val id = state.queue.firstOrNull()
    val ex = state.lesson.exercises.firstOrNull { it.id == id }
    val sol = state.lesson.solution
    val design = state.lesson.kind == LessonKind.Design
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = AlgoTheme.spacing.md, vertical = AlgoTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.lg),
        ) {
            if (ex != null) {
                val head = if (solved) {
                    val xp = xpFor(ExerciseResult(ex.id, true, state.attempts[ex.id] ?: 0, state.hintsShown[ex.id] ?: 0))
                    buildAnnotatedString {
                        append("✅ Решено! ")
                        withStyle(SpanStyle(color = c.xpText)) { append("+$xp XP") }
                    }
                } else buildAnnotatedString { append("❌ Разберём решение") }
                Text(head, style = t.display, color = ink)
                Section("Эталон") {
                    // design: each step is its own reference (requirements/API/scheme), not the final solution.code
                    CodeBlock(if (design) assembled(ex) else sol.code, highlightRanges = solutionSpans(ex), syntax = !design)
                }
                // design: the outro belongs to the final scheme step, even if a failed step was requeued after it
                val showOutro = if (design) ex.id == state.lesson.exercises.last().id else isLastExercise(state)
                if (showOutro) {
                    if (sol.time.isNotBlank() || sol.space.isNotBlank()) Section("Сложность") {
                        Text("Время ${sol.time} · Память ${sol.space}", style = t.body, color = ink)
                    }
                    if (sol.pitfalls.isNotEmpty()) Section("⚠️ Грабли") { Bullets(sol.pitfalls) }
                    if (sol.variations.isNotEmpty()) Section(if (design) "🔁 Что спросят дальше" else "🔁 Вариации") { Bullets(sol.variations) }
                }
            }
        }
        ChunkyButton("Продолжить", { on(LessonAction.Continue(screenKey(state))) }, Modifier.padding(AlgoTheme.spacing.md))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.sm)) {
        Text(title, style = AlgoTheme.typography.heading, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.xs)) {
        for (s in items) Text(richText("• $s"), style = AlgoTheme.typography.body, color = MaterialTheme.colorScheme.onBackground)
    }
}
