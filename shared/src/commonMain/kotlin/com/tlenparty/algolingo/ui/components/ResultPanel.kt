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
import com.tlenparty.algolingo.kb.parseInline
import com.tlenparty.algolingo.kb.ui.inlineText
import com.tlenparty.algolingo.lesson.PanelItem
import com.tlenparty.algolingo.lesson.PanelKind
import com.tlenparty.algolingo.lesson.PanelModel
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.LocalCodeLang
import com.tlenparty.algolingo.design.components.ChunkyButton
import com.tlenparty.algolingo.design.components.ChunkyStyle
import com.tlenparty.algolingo.design.highlight

/** `code` and **bold** markup from lesson content. */
@Composable
internal fun richText(s: String): AnnotatedString = inlineText(parseInline(s))

@Composable
fun ResultPanel(model: PanelModel, maxBodyHeight: Dp, onNext: () -> Unit, modifier: Modifier = Modifier, syntax: Boolean = true) {
    val c = AlgoTheme.colors
    val t = AlgoTheme.typography
    val style = when (model.kind) {
        PanelKind.Success -> ChunkyStyle.Primary
        PanelKind.Amber -> ChunkyStyle.Amber
        PanelKind.Danger -> ChunkyStyle.Danger
    }
    val tintC = when (model.kind) { PanelKind.Success -> c.successTint; PanelKind.Amber -> c.acceptableTint; PanelKind.Danger -> c.wrongTint }
    val accentC = when (model.kind) { PanelKind.Success -> MaterialTheme.colorScheme.primary; PanelKind.Amber -> c.amber; PanelKind.Danger -> c.redFill }
    val inkC = when (model.kind) { PanelKind.Success -> c.successText; PanelKind.Amber -> c.acceptableText; PanelKind.Danger -> c.wrongText }
    Column(modifier.fillMaxWidth().background(tintC)) {
        Box(Modifier.fillMaxWidth().height(AlgoTheme.sizes.border).background(accentC))
        Column(Modifier.padding(start = AlgoTheme.spacing.md, end = AlgoTheme.spacing.md, top = AlgoTheme.spacing.lg, bottom = AlgoTheme.spacing.md)) {
            Text(
                model.title, style = t.heading, color = inkC,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (model.wrong.isNotEmpty() || model.acceptable.isNotEmpty()) {
                Column(
                    Modifier.padding(top = AlgoTheme.spacing.smd).heightIn(max = maxBodyHeight).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.md),
                ) {
                    for (w in model.wrong) Item(w, inkC, syntax)
                    if (model.wrong.isNotEmpty() && model.acceptable.isNotEmpty()) {
                        Text("Принято, но…", style = t.label, color = c.acceptableText)
                    }
                    for (a in model.acceptable) Item(a, inkC, syntax)
                }
            }
            ChunkyButton(model.button, onNext, Modifier.padding(top = AlgoTheme.spacing.md), style = style)
        }
    }
}

@Composable
private fun Item(item: PanelItem, ink: androidx.compose.ui.graphics.Color, syntax: Boolean) {
    val c = AlgoTheme.colors
    val lang = LocalCodeLang.current
    fun code(s: String) = if (syntax) highlight(s, c, lang) else AnnotatedString(s)
    val t = AlgoTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(AlgoTheme.spacing.xs)) {
        Text(code(item.chip), style = t.codeBold, color = ink, softWrap = false)
        Text(richText(item.text), style = t.body, color = MaterialTheme.colorScheme.onBackground)
        if (item.correct != null) {
            Text("Верно:", style = t.label, color = c.secondaryText)
            Text(code(item.correct), style = t.codeBold, color = MaterialTheme.colorScheme.onBackground, softWrap = false)
        }
    }
}
