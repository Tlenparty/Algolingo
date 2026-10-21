package com.tlenparty.algolingo.lesson.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tlenparty.algolingo.lesson.LessonAction
import com.tlenparty.algolingo.lesson.LessonState
import com.tlenparty.algolingo.lesson.Phase
import com.tlenparty.algolingo.lesson.Verdict
import com.tlenparty.algolingo.lesson.holeVisual
import com.tlenparty.algolingo.lesson.panelModel
import com.tlenparty.algolingo.design.components.AlgoCard
import com.tlenparty.algolingo.design.components.ChipState
import com.tlenparty.algolingo.design.components.ChunkyButton
import com.tlenparty.algolingo.ui.components.CodeTemplate
import com.tlenparty.algolingo.ui.components.ResultPanel
import com.tlenparty.algolingo.design.components.TokenChip
import com.tlenparty.algolingo.ui.components.richText
import com.tlenparty.algolingo.design.AlgoTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseScreen(state: LessonState, on: (LessonAction) -> Unit, modifier: Modifier = Modifier) {
    val ex = state.lesson.exercises.firstOrNull { it.id == state.queue.firstOrNull() }
    val cur = state.current
    if (ex == null || cur == null) return // T-03-10: never crash on inconsistent state
    val phase = state.phase
    val check = (phase as? Phase.Feedback)?.check
    val solving = phase is Phase.Solving
    val t = AlgoTheme.typography
    val c = AlgoTheme.colors
    val outline = MaterialTheme.colorScheme.outline
    val shown = (state.hintsShown[ex.id] ?: 0).coerceIn(0, ex.hints.size)
    val scroll = rememberScrollState()

    LaunchedEffect(shown) { if (shown > 0) scroll.animateScrollTo(0) } // new card is appended near the top
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(check) {
        if (check != null) {
            haptic.performHapticFeedback(
                if (check.verdict == Verdict.Wrong) HapticFeedbackType.Reject else HapticFeedbackType.Confirm,
            )
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxBody = maxHeight / 2
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = AlgoTheme.spacing.md)) {
                Text(state.lesson.title, style = t.heading, color = MaterialTheme.colorScheme.onBackground)
                Column(Modifier.padding(top = AlgoTheme.spacing.sm)) {
                    for (line in state.lesson.statement.lines()) {
                        Text(
                            richText(line), style = t.body,
                            color = if (line.trimStart().startsWith("🎯")) c.secondaryText else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                Column(Modifier.padding(top = AlgoTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.sm)) {
                    for (i in 0 until shown) key(i) {
                        AlgoCard(
                            color = c.focusTint, border = c.focus, shape = AlgoTheme.shapes.medium,
                            padding = AlgoTheme.spacing.md, modifier = Modifier.fillMaxWidth(), spacing = AlgoTheme.spacing.xs,
                        ) {
                            Text("Подсказка ${i + 1} из ${ex.hints.size}", style = t.label, color = c.focusText)
                            Text(richText(ex.hints[i]), style = t.body, color = MaterialTheme.colorScheme.onBackground)
                            if (i == 0) Text("Подсказка снижает оценку до 💡", style = t.label, color = c.secondaryText)
                        }
                    }
                    CodeTemplate(
                        segments = ex.segments,
                        placedText = cur.placed.map { it?.let { i -> ex.bank.getOrNull(i)?.text } },
                        visuals = ex.holes.indices.map { holeVisual(it, cur, check) },
                        onHoleTap = { on(LessonAction.TapHole(it)) },
                        enabled = solving,
                    )
                }
                Column(Modifier.height(AlgoTheme.spacing.md)) {}
            }
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                Column(Modifier.fillMaxWidth().height(AlgoTheme.sizes.border).background(outline)) {}
                if (check != null) {
                    // Keep the last check during exit animation so the panel does not blank out.
                    ResultPanelHost(check, maxBody) { on(LessonAction.Continue) }
                } else {
                    Column(Modifier.padding(AlgoTheme.spacing.md)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.sm)) {
                            for (id in cur.order) key(id) {
                                val placed = cur.isPlaced(id)
                                TokenChip(
                                    ex.bank.getOrNull(id)?.text ?: "",
                                    if (placed) ChipState.Ghost else ChipState.Normal,
                                    if (placed || !solving) null else ({ on(LessonAction.TapChip(id)) }),
                                )
                            }
                        }
                        ChunkyButton(
                            "Проверить", { on(LessonAction.Check) }, Modifier.padding(top = AlgoTheme.spacing.md),
                            enabled = solving && cur.placed.all { it != null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultPanelHost(check: com.tlenparty.algolingo.lesson.CheckResult, maxBody: androidx.compose.ui.unit.Dp, onNext: () -> Unit) {
    // Enter animation: visible flips to true on first composition.
    val visible = androidx.compose.runtime.remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
    ) { ResultPanel(panelModel(check), maxBody, onNext) }
}
