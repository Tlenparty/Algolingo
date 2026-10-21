package com.tlenparty.algolingo.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.tlenparty.algolingo.design.AlgoTheme

@Composable
fun LessonProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val target = fraction.coerceIn(0f, 1f)
    val f by animateFloatAsState(target, tween(300))
    Box(modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) }.fillMaxWidth().height(AlgoTheme.sizes.progressBar).background(MaterialTheme.colorScheme.outline, CircleShape)) {
        if (f > 0f) Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}
