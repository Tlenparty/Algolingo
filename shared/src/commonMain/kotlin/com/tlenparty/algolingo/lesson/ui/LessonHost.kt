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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tlenparty.algolingo.lesson.LessonViewModel
import com.tlenparty.algolingo.design.components.ChunkyButton
import com.tlenparty.algolingo.design.AlgoTheme
import kotlin.time.Clock

@Composable
fun LessonHost(id: String, onHome: () -> Unit) {
    val vm = viewModel(key = id) { LessonViewModel(id, seed = Clock.System.now().toEpochMilliseconds().toInt()) }
    val r by vm.state.collectAsStateWithLifecycle()
    val state = r?.getOrNull()
    when {
        r == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        state == null -> LessonErrorScreen(onHome)
        else -> LessonScreen(state, vm::on, onHome)
    }
}

@Composable
private fun LessonErrorScreen(onHome: () -> Unit) {
    val t = AlgoTheme.typography
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = AlgoTheme.sizes.contentMaxWidth)) {
            Column(
                Modifier.weight(1f).padding(AlgoTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.smd, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Не удалось открыть урок", style = t.heading, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                Text(
                    "Файл урока повреждён или отсутствует. Попробуй другой урок или переустанови приложение.",
                    style = t.body, color = AlgoTheme.colors.secondaryText, textAlign = TextAlign.Center,
                )
            }
            ChunkyButton("На главную", onHome, Modifier.padding(AlgoTheme.spacing.md))
        }
    }
}
