package com.tlenparty.algolingo.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.coroutines.delay

/** [showLang] = false hides the language switch (SD tab). */
@Composable
fun HomeHost(vm: HomeViewModel, showLang: Boolean, onOpenLesson: (HomeEffect.OpenLesson) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onOpenLesson)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(vm) { vm.effects.collect { if (it is HomeEffect.OpenLesson) open(it) } }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // ponytail: 1-min poll, so a midnight rollover shows up to a minute late.
            while (true) {
                vm.on(HomeAction.RefreshDate)
                delay(60_000)
            }
        }
    }
    val content = state as? HomeUiState.Content
    HomeScreen(
        content?.model,
        onStart = { vm.on(HomeAction.StartLesson(it)) },
        onReview = { vm.on(HomeAction.StartReview) },
        saveFailed = content?.saveFailed == true,
        lang = content?.lang ?: CodeLang.Kotlin,
        onLang = if (showLang) { l -> vm.on(HomeAction.SelectLang(l)) } else null,
    )
}
