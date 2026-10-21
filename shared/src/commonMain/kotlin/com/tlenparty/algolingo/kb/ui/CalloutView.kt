package com.tlenparty.algolingo.kb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.kb.Block
import com.tlenparty.algolingo.kb.Callout
import com.tlenparty.algolingo.design.AlgoTheme

private fun typeWord(type: String) = when (type.lowercase()) {
    "question" -> "Вопрос"
    "success" -> "Решение"
    "warning" -> "Внимание"
    "important" -> "Важно"
    "tip" -> "Совет"
    "info" -> "Инфо"
    else -> "Заметка"
}

@Composable
fun CalloutView(c: Callout, noteId: String, index: Int, content: @Composable (Block) -> Unit) {
    val a = AlgoTheme.colors
    val (tint, accent) = when (c.type.lowercase()) {
        "question", "info" -> a.focusTint to a.focusText
        "success", "tip" -> a.successTint to a.successText
        "warning" -> a.acceptableTint to a.acceptableText
        "important" -> a.wrongTint to a.wrongText
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurface
    }
    var open by rememberSaveable(noteId, index) { mutableStateOf(!c.collapsed) }
    val isOpen = open || !c.foldable
    val title = c.title.ifBlank { typeWord(c.type) }
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = tint,
        border = BorderStroke(2.dp, accent),
    ) {
        Column(Modifier.padding(12.dp)) {
            val chevron = if (!c.foldable) "" else if (isOpen) "▾ " else "▸ "
            Text(
                chevron + title,
                Modifier.fillMaxWidth().then(if (c.foldable) Modifier.clickable { open = !open } else Modifier),
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            if (isOpen) c.blocks.forEach { content(it) }
        }
    }
}
