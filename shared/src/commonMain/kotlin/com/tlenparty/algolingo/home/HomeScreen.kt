package com.tlenparty.algolingo.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.tlenparty.algolingo.design.components.ChunkyBox
import com.tlenparty.algolingo.design.AlgoTheme

private data class HomeLesson(val id: String, val caption: String)

// Phase 3 slice: only the playable lesson. Phase 4 replaces this with index.json + statuses.
private val lessons = listOf(HomeLesson("l0.4", "0.4 Бинарный поиск"))

@Composable
fun HomeScreen(onStart: (String) -> Unit) {
    val c = AlgoTheme.colors
    val s = MaterialTheme.colorScheme
    val t = AlgoTheme.typography
    Box(Modifier.fillMaxSize().background(s.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = AlgoTheme.sizes.contentMaxWidth).padding(AlgoTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.xl),
        ) {
            lessons.forEach { l ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.sm)) {
                    ChunkyBox(s.primary, c.greenLedge, null, AlgoTheme.shapes.pill, AlgoTheme.sizes.pillFace, true, { onStart(l.id) }) {
                        Text("Начать", style = t.button, color = s.onPrimary, modifier = Modifier.padding(horizontal = AlgoTheme.spacing.pillX))
                    }
                    ChunkyBox(
                        s.primary, c.greenLedge, null, AlgoTheme.shapes.node, AlgoTheme.sizes.node, true, { onStart(l.id) },
                        Modifier.semantics { contentDescription = l.caption },
                    ) {
                        Box(Modifier.size(AlgoTheme.sizes.node), contentAlignment = Alignment.Center) {
                            val ink = s.onPrimary
                            Canvas(Modifier.size(AlgoTheme.sizes.nodeGlyph)) {
                                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height); close() }, ink)
                            }
                        }
                    }
                    Text(
                        l.caption, style = t.label, color = c.secondaryText, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(AlgoTheme.sizes.nodeCaption),
                    )
                }
            }
        }
    }
}
