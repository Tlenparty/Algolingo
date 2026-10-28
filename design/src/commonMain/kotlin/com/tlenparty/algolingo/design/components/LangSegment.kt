package com.tlenparty.algolingo.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.design.CodeLang

/** Two-way language switch (one segment per CodeLang); labels are the enum names. testTag "lang-<id>". */
@Composable
fun LangSegment(selected: CodeLang, onSelect: (CodeLang) -> Unit, modifier: Modifier = Modifier) {
    val s = MaterialTheme.colorScheme
    val shape = AlgoTheme.shapes.medium
    Row(
        modifier.height(AlgoTheme.sizes.touchTarget).clip(shape)
            .border(AlgoTheme.sizes.border, s.outline, shape).selectableGroup(),
    ) {
        CodeLang.entries.forEach { lang ->
            val on = lang == selected
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (on) s.primary else s.background)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(lang) }
                    .testTag("lang-${lang.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(lang.name, style = AlgoTheme.typography.button, color = if (on) s.onPrimary else s.onBackground)
            }
        }
    }
}
