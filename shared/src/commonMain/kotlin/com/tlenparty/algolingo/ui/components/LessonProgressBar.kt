package com.tlenparty.algolingo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LessonProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(300))
    val shape = RoundedCornerShape(50)
    Box(modifier.fillMaxWidth().height(16.dp).background(MaterialTheme.colorScheme.outline, shape)) {
        if (f > 0f) Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(MaterialTheme.colorScheme.primary, shape))
    }
}
