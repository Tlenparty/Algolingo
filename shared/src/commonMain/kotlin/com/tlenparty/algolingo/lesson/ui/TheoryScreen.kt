package com.tlenparty.algolingo.lesson.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tlenparty.algolingo.lesson.TheoryCard
import com.tlenparty.algolingo.design.components.AlgoCard
import com.tlenparty.algolingo.design.components.ChunkyButton
import com.tlenparty.algolingo.design.components.CodeBlock
import com.tlenparty.algolingo.ui.components.richText
import com.tlenparty.algolingo.design.AlgoTheme

@Composable
fun TheoryScreen(card: TheoryCard, onNext: () -> Unit, modifier: Modifier = Modifier, syntax: Boolean = true) {
    val t = AlgoTheme.typography
    val cap = AlgoTheme.colors.secondaryText
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(AlgoTheme.spacing.md)) {
            AlgoCard(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = MaterialTheme.colorScheme.outline,
                shape = AlgoTheme.shapes.large,
                padding = AlgoTheme.spacing.lg,
                modifier = Modifier.fillMaxWidth(),
                spacing = AlgoTheme.spacing.smd,
            ) {
                Text(card.title, style = t.heading, color = MaterialTheme.colorScheme.onBackground)
                if (card.signals.isNotEmpty()) {
                    Text("Сигналы", style = t.label, color = cap)
                    Bullets(card.signals)
                }
                if (card.skeleton != null) {
                    Text("Скелет", style = t.label, color = cap)
                    CodeBlock(card.skeleton, syntax = syntax)
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
        ChunkyButton("Дальше", onNext, Modifier.padding(AlgoTheme.spacing.md))
    }
}

@Composable
private fun Bullets(items: List<String>) {
    for (s in items) {
        Text(richText("• $s"), style = AlgoTheme.typography.body, color = MaterialTheme.colorScheme.onBackground)
    }
}
