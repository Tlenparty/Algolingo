package com.tlenparty.algolingo.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.components.ChunkyBox
import com.tlenparty.algolingo.design.components.ChunkyButton
import com.tlenparty.algolingo.design.components.ChunkyStyle
import com.tlenparty.algolingo.progress.HomeModel
import com.tlenparty.algolingo.progress.HomeNode
import com.tlenparty.algolingo.progress.LessonStatus

/** model == null while catalog/progress load: plain background, no flicker of wrong statuses. */
@Composable
fun HomeScreen(model: HomeModel?, onStart: (String) -> Unit, onReview: () -> Unit) {
    val s = MaterialTheme.colorScheme
    val t = AlgoTheme.typography
    val sp = AlgoTheme.spacing
    Box(Modifier.fillMaxSize().background(s.background), contentAlignment = Alignment.TopCenter) {
        if (model == null) return@Box
        Column(Modifier.fillMaxSize().widthIn(max = AlgoTheme.sizes.contentMaxWidth)) {
            Row(
                Modifier.fillMaxWidth().height(AlgoTheme.sizes.topBar).padding(horizontal = sp.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔥 ${model.streak}", style = t.label, color = s.onBackground, modifier = Modifier.semantics { contentDescription = "Серия дней: ${model.streak}" })
                Text("⭐ ${model.xp}", style = t.label, color = s.onBackground, modifier = Modifier.semantics { contentDescription = "Опыт: ${model.xp} XP" })
            }
            if (model.due.isNotEmpty()) {
                ChunkyButton("Повтор сегодня (${model.due.size})", onReview, Modifier.padding(horizontal = sp.md), style = ChunkyStyle.Secondary)
            }
            if (model.nodes.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).padding(sp.md),
                    verticalArrangement = Arrangement.spacedBy(sp.smd, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Уроков пока нет", style = t.heading, color = s.onBackground, textAlign = TextAlign.Center)
                    Text(
                        "Контент не найден. Пересобери приложение с папкой content.",
                        style = t.body, color = AlgoTheme.colors.secondaryText, textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(top = sp.lg, bottom = AlgoTheme.spacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(sp.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    itemsIndexed(model.nodes, key = { _, n -> n.id }) { i, n ->
                        // Zig-zag 0, +48, 0, -48 (03-UI-SPEC §1).
                        val zig = when (i % 4) {
                            1 -> Modifier.offset(x = AlgoTheme.spacing.xxl)
                            3 -> Modifier.offset(x = -AlgoTheme.spacing.xxl)
                            else -> Modifier
                        }
                        PathNode(n, zig, onStart)
                    }
                }
            }
        }
    }
}

@Composable
private fun PathNode(n: HomeNode, modifier: Modifier, onStart: (String) -> Unit) {
    val c = AlgoTheme.colors
    val s = MaterialTheme.colorScheme
    val t = AlgoTheme.typography
    val start = { onStart(n.id) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.sm)) {
        when (n.status) {
            LessonStatus.Available -> {
                ChunkyBox(
                    face = s.primary, ledge = c.greenLedge, border = null, shape = AlgoTheme.shapes.pill,
                    faceMinHeight = AlgoTheme.sizes.pillFace, enabled = true, onClick = start,
                    // The one focus stop per lesson: node and caption below are hidden from accessibility.
                    modifier = Modifier.semantics { contentDescription = "Начать: ${n.caption}" },
                ) {
                    Text("Начать", style = t.button, color = s.onPrimary, modifier = Modifier.padding(horizontal = AlgoTheme.spacing.pillX))
                }
                Node(s.primary, c.greenLedge, true, start, Modifier.clearAndSetSemantics {}) {
                    val ink = s.onPrimary
                    // Canvas triangle: the ▶ emoji renders badly on iOS.
                    Canvas(Modifier.size(AlgoTheme.sizes.nodeGlyph)) {
                        drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height); close() }, ink)
                    }
                }
            }
            // Replay plays the full lesson; SRS only moves when due (A2).
            LessonStatus.Done -> Node(
                s.primary, c.greenLedge, true, start,
                Modifier.clearAndSetSemantics { contentDescription = "Пройден, повторить: ${n.caption}" },
            ) { Text("✅", fontSize = t.nodeEmoji) }
            LessonStatus.Locked -> Node(
                s.outline, null, false, null,
                Modifier.clearAndSetSemantics { contentDescription = "Закрыт: ${n.caption}" },
            ) { Text("🔒", fontSize = t.nodeEmoji) }
        }
        Text(
            n.caption, style = t.label, color = c.secondaryText, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(AlgoTheme.sizes.nodeCaption).clearAndSetSemantics {},
        )
    }
}

@Composable
private fun Node(
    face: Color,
    ledge: Color?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    glyph: @Composable () -> Unit,
) = ChunkyBox(
    face = face, ledge = ledge, border = null, shape = AlgoTheme.shapes.node,
    faceMinHeight = AlgoTheme.sizes.node, enabled = enabled, onClick = onClick, modifier = modifier,
) {
    Box(Modifier.size(AlgoTheme.sizes.node), contentAlignment = Alignment.Center) { glyph() }
}
