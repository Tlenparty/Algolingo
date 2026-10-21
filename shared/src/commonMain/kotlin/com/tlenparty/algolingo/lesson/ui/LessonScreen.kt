package com.tlenparty.algolingo.lesson.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.lesson.LessonAction
import com.tlenparty.algolingo.lesson.LessonReducer
import com.tlenparty.algolingo.lesson.LessonState
import com.tlenparty.algolingo.lesson.Phase
import com.tlenparty.algolingo.lesson.hintsRemaining
import com.tlenparty.algolingo.lesson.screenKey
import com.tlenparty.algolingo.ui.components.LessonTopBar

@Composable
fun LessonScreen(state: LessonState, on: (LessonAction) -> Unit, onExit: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    val done = state.phase == Phase.Done
    // On the summary back is not intercepted: NavHost pops straight to Home.
    ExitBackHandler(enabled = !done, onBack = { confirm = true })
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = AlgoTheme.sizes.contentMaxWidth)) {
            if (!done) {
                val id = state.queue.firstOrNull()
                val ex = state.lesson.exercises.firstOrNull { it.id == id }
                val exPhase = state.phase == Phase.Solving || state.phase is Phase.Feedback
                LessonTopBar(
                    progress = state.progress,
                    onClose = { confirm = true },
                    hintsRemaining = if (exPhase && ex != null && ex.hints.isNotEmpty()) hintsRemaining(ex, state.hintsShown[ex.id] ?: 0) else null,
                    hintEnabled = state.phase == Phase.Solving,
                    onHint = { on(LessonAction.Hint) },
                )
            }
            // Target is the whole state keyed by screenKey: the outgoing page keeps its own snapshot,
            // while Solving -> Feedback (same key) updates in place without a slide.
            AnimatedContent(
                targetState = state,
                contentKey = ::screenKey,
                transitionSpec = {
                    (slideInHorizontally(tween(200)) { it / 4 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f),
            ) { s ->
                when (val p = s.phase) {
                    is Phase.Theory -> s.lesson.theory.getOrNull(p.index)?.let { TheoryScreen(it, onNext = { on(LessonAction.NextTheory(p.index)) }) }
                    Phase.Solving, is Phase.Feedback -> ExerciseScreen(s, on)
                    is Phase.Reveal -> RevealScreen(s, p.solved, on)
                    Phase.Done -> SummaryScreen(LessonReducer.result(s), onDone = onExit)
                }
            }
        }
    }
    if (confirm) ExitDialog(onStay = { confirm = false }, onLeave = { confirm = false; onExit() })
}
