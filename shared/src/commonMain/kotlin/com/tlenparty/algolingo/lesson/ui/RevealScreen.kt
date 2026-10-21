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
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.lesson.ExerciseResult
import com.tlenparty.algolingo.lesson.LessonAction
import com.tlenparty.algolingo.lesson.LessonState
import com.tlenparty.algolingo.lesson.isLastExercise
import com.tlenparty.algolingo.lesson.solutionSpans
import com.tlenparty.algolingo.lesson.xpFor
import com.tlenparty.algolingo.ui.components.ChunkyButton
import com.tlenparty.algolingo.ui.components.CodeBlock
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
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
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
                Section("Эталон") { CodeBlock(sol.code, highlightRanges = solutionSpans(ex)) }
                if (isLastExercise(state)) {
                    Section("Сложность") {
                        Text("Время ${sol.time} · Память ${sol.space}", style = t.body, color = ink)
                    }
                    if (sol.pitfalls.isNotEmpty()) Section("⚠️ Грабли") { Bullets(sol.pitfalls) }
                    if (sol.variations.isNotEmpty()) Section("🔁 Вариации") { Bullets(sol.variations) }
                }
            }
        }
        ChunkyButton("Продолжить", { on(LessonAction.Continue) }, Modifier.padding(16.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = AlgoTheme.typography.heading, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (s in items) Text(richText("• $s"), style = AlgoTheme.typography.body, color = MaterialTheme.colorScheme.onBackground)
    }
}
