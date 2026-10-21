package com.tlenparty.algolingo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.kb.parseInline
import com.tlenparty.algolingo.kb.ui.inlineText
import com.tlenparty.algolingo.lesson.PanelItem
import com.tlenparty.algolingo.lesson.PanelKind
import com.tlenparty.algolingo.lesson.PanelModel
import com.tlenparty.algolingo.ui.theme.LocalAppColors
import com.tlenparty.algolingo.ui.theme.LocalAppText
import com.tlenparty.algolingo.ui.theme.highlight

/** `code` and **bold** markup from lesson content. */
@Composable
internal fun richText(s: String): AnnotatedString = inlineText(parseInline(s))

@Composable
fun ResultPanel(model: PanelModel, maxBodyHeight: Dp, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val t = LocalAppText.current
    val style = when (model.kind) {
        PanelKind.Success -> ChunkyStyle.Primary
        PanelKind.Amber -> ChunkyStyle.Amber
        PanelKind.Danger -> ChunkyStyle.Danger
    }
    val tintC = when (model.kind) { PanelKind.Success -> c.successTint; PanelKind.Amber -> c.acceptableTint; PanelKind.Danger -> c.wrongTint }
    val accentC = when (model.kind) { PanelKind.Success -> MaterialTheme.colorScheme.primary; PanelKind.Amber -> c.amber; PanelKind.Danger -> c.redFill }
    val inkC = when (model.kind) { PanelKind.Success -> c.successText; PanelKind.Amber -> c.acceptableText; PanelKind.Danger -> c.wrongText }
    Column(modifier.fillMaxWidth().background(tintC)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(accentC))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)) {
            Text(
                model.title, style = t.heading, color = inkC,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (model.wrong.isNotEmpty() || model.acceptable.isNotEmpty()) {
                Column(
                    Modifier.padding(top = 12.dp).heightIn(max = maxBodyHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    for (w in model.wrong) Item(w, inkC)
                    if (model.wrong.isNotEmpty() && model.acceptable.isNotEmpty()) {
                        Text("Принято, но…", style = t.label, color = c.acceptableText)
                    }
                    for (a in model.acceptable) Item(a, inkC)
                }
            }
            ChunkyButton(model.button, onNext, Modifier.padding(top = 16.dp), style = style)
        }
    }
}

@Composable
private fun Item(item: PanelItem, ink: androidx.compose.ui.graphics.Color) {
    val c = LocalAppColors.current
    val t = LocalAppText.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(highlight(item.chip, c), style = t.codeBold, color = ink, softWrap = false)
        Text(richText(item.text), style = t.body, color = MaterialTheme.colorScheme.onBackground)
        if (item.correct != null) {
            Text("Верно:", style = t.label, color = c.secondaryText)
            Text(highlight(item.correct, c), style = t.codeBold, color = MaterialTheme.colorScheme.onBackground, softWrap = false)
        }
    }
}
