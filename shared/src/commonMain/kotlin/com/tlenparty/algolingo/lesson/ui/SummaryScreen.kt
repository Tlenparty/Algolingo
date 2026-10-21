package com.tlenparty.algolingo.lesson.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tlenparty.algolingo.lesson.Grade
import com.tlenparty.algolingo.lesson.LessonResult
import com.tlenparty.algolingo.lesson.xpFor
import com.tlenparty.algolingo.ui.components.ChunkyButton
import com.tlenparty.algolingo.design.AlgoTheme

@Composable
fun SummaryScreen(result: LessonResult, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val t = AlgoTheme.typography
    val (emoji, title, sub) = when (result.grade) {
        Grade.Clean -> Triple("✅", "Чисто!", "Без подсказок и ошибок. Так держать.")
        Grade.Hinted -> Triple("💡", "Решено с подсказкой", "Задача решена, но с подсказками или повтором. Вернись к ней завтра.")
        Grade.Failed -> Triple("❌", "Пока не вышло", "Разобрали решение — попробуй ещё раз позже.")
    }
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).padding(start = 16.dp, end = 16.dp, top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, fontSize = AlgoTheme.typography.heroEmoji) // decorative: title carries the meaning
            Text(title, style = t.display, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            Text(sub, style = t.body, color = AlgoTheme.colors.secondaryText, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("+${xpFor(result)} XP", style = t.heading, color = AlgoTheme.colors.xpText)
        }
        ChunkyButton("Продолжить", onDone, Modifier.padding(16.dp))
    }
}
