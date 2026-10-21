package com.tlenparty.algolingo.lesson.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.lesson.TheoryCard
import com.tlenparty.algolingo.ui.components.ChunkyButton
import com.tlenparty.algolingo.ui.components.CodeBlock
import com.tlenparty.algolingo.ui.components.richText
import com.tlenparty.algolingo.design.AlgoTheme

@Composable
fun TheoryScreen(card: TheoryCard, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val t = AlgoTheme.typography
    val cap = AlgoTheme.colors.secondaryText
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(card.title, style = t.heading, color = MaterialTheme.colorScheme.onBackground)
                    if (card.signals.isNotEmpty()) {
                        Text("Сигналы", style = t.label, color = cap)
                        Bullets(card.signals)
                    }
                    if (card.skeleton != null) {
                        Text("Скелет", style = t.label, color = cap)
                        CodeBlock(card.skeleton)
                    }
                    if (card.body.isNotBlank()) {
                        Text(richText(card.body), style = t.body, color = MaterialTheme.colorScheme.onBackground)
                    }
                    if (card.pitfalls.isNotEmpty()) {
                        Text("⚠️ Грабли", style = t.label, color = cap)
                        Bullets(card.pitfalls)
                    }
                }
            }
        }
        ChunkyButton("Дальше", onNext, Modifier.padding(16.dp))
    }
}

@Composable
private fun Bullets(items: List<String>) {
    for (s in items) {
        Text(richText("• $s"), style = AlgoTheme.typography.body, color = MaterialTheme.colorScheme.onBackground)
    }
}
