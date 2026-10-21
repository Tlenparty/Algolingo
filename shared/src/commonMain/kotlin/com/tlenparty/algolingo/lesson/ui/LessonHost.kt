package com.tlenparty.algolingo.lesson.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tlenparty.algolingo.lesson.Lesson
import com.tlenparty.algolingo.lesson.LessonViewModel
import com.tlenparty.algolingo.lesson.loadLesson
import com.tlenparty.algolingo.ui.components.ChunkyButton
import com.tlenparty.algolingo.design.AlgoTheme
import kotlin.time.Clock

@Composable
fun LessonHost(id: String, onHome: () -> Unit) {
    val result by produceState<Result<Lesson>?>(null, id) { value = loadLesson(id) }
    val r = result
    when {
        r == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        r.isFailure -> LessonErrorScreen(onHome)
        else -> {
            val lesson = r.getOrThrow()
            val vm = viewModel(key = id) { LessonViewModel(lesson, seed = Clock.System.now().toEpochMilliseconds().toInt()) }
            val state by vm.state.collectAsStateWithLifecycle()
            LessonScreen(state, vm::on, onHome)
        }
    }
}

@Composable
private fun LessonErrorScreen(onHome: () -> Unit) {
    val t = AlgoTheme.typography
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 600.dp)) {
            Column(
                Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Не удалось открыть урок", style = t.heading, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Text(
                    "Файл урока повреждён или отсутствует. Попробуй другой урок или переустанови приложение.",
                    style = t.body, color = AlgoTheme.colors.secondaryText, textAlign = TextAlign.Center,
                )
            }
            ChunkyButton("На главную", onHome, Modifier.padding(16.dp))
        }
    }
}
