package com.tlenparty.algolingo.lesson.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.ui.components.ChunkyButton
import com.tlenparty.algolingo.ui.components.ChunkyStyle

/**
 * Intercepts system back / iOS edge swipe. Uses CMP common BackHandler; if the iOS swipe is not
 * intercepted inside NavHost (RESEARCH Pitfall 5), swap only this body to NavigationBackHandler.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExitBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
fun ExitDialog(onStay: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text("Выйти из урока?") },
        text = { Text("Прогресс этого урока не сохранится.") },
        confirmButton = {
            TextButton(onClick = onLeave) { Text("Выйти", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { ChunkyButton("Остаться", onStay, Modifier.width(140.dp), style = ChunkyStyle.Secondary) },
    )
}
