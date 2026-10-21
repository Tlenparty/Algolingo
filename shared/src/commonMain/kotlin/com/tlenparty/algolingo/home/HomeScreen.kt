package com.tlenparty.algolingo.home

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tlenparty.algolingo.ui.components.ChunkyBox
import com.tlenparty.algolingo.ui.theme.LocalAppColors
import com.tlenparty.algolingo.ui.theme.LocalAppText

private data class HomeLesson(val id: String, val caption: String)

// Phase 3 slice: only the playable lesson. Phase 4 replaces this with index.json + statuses.
private val lessons = listOf(HomeLesson("l0.4", "0.4 Бинарный поиск"))

@Composable
fun HomeScreen(onStart: (String) -> Unit) {
    val c = LocalAppColors.current
    val s = MaterialTheme.colorScheme
    val t = LocalAppText.current
    Box(Modifier.fillMaxSize().background(s.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 600.dp).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            lessons.forEach { l ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChunkyBox(s.primary, c.greenLedge, null, 20.dp, 40.dp, true, { onStart(l.id) }) {
                        Text("Начать", style = t.button, color = s.onPrimary, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                    ChunkyBox(
                        s.primary, c.greenLedge, null, 36.dp, 72.dp, true, { onStart(l.id) },
                        Modifier.semantics { contentDescription = l.caption },
                    ) {
                        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            Text("▶", color = s.onPrimary, fontSize = 28.sp)
                        }
                    }
                    Text(
                        l.caption, style = t.label, color = c.secondaryText, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(160.dp),
                    )
                }
            }
        }
    }
}
