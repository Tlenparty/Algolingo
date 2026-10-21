package com.tlenparty.algolingo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.design.AlgoTheme

/** [hintsRemaining] == null means the exercise has no hints: a spacer replaces the button. */
@Composable
fun LessonTopBar(
    progress: Float,
    onClose: () -> Unit,
    hintsRemaining: Int?,
    hintEnabled: Boolean,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AlgoTheme.colors
    val t = AlgoTheme.typography
    Row(modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).semantics { contentDescription = "Закрыть урок" }
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) { Text("✕", color = c.secondaryText, fontSize = AlgoTheme.typography.iconEmoji) }
        LessonProgressBar(progress, Modifier.weight(1f).padding(horizontal = 16.dp))
        if (hintsRemaining == null) {
            Box(Modifier.size(48.dp))
        } else {
            val active = hintEnabled && hintsRemaining > 0
            Box(
                Modifier.size(48.dp).semantics { contentDescription = "Подсказка, осталось $hintsRemaining" }
                    .clickable(enabled = active, role = Role.Button, onClick = onHint),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("💡", fontSize = AlgoTheme.typography.iconEmoji, color = if (active) androidx.compose.ui.graphics.Color.Unspecified else c.secondaryText)
                    if (hintsRemaining > 0) {
                        Text(hintsRemaining.toString(), style = t.label, color = if (active) c.focusText else c.secondaryText)
                    }
                }
            }
        }
    }
}
