package com.tlenparty.algolingo.home.domain

import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.datetime.LocalDate
import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.progress.domain.LessonStatus
import com.tlenparty.algolingo.progress.domain.Progress
import com.tlenparty.algolingo.progress.domain.dueIds
import com.tlenparty.algolingo.progress.domain.lessonStatuses
import com.tlenparty.algolingo.progress.domain.visibleStreak

data class HomeNode(val id: String, val caption: String, val status: LessonStatus)

data class HomeModel(val streak: Int, val xp: Int, val nodes: List<HomeNode>, val due: List<String>)

/** [lang] null = no language filter (SD tab). */
fun homeModel(catalog: Catalog, p: Progress, today: LocalDate, lang: CodeLang? = p.lang): HomeModel {
    val ids = catalog.lessons.map { it.id }
    val inLang = catalog.lessons.filter { it.has(lang) }.mapTo(HashSet()) { it.id }
    val available = { id: String -> id in inLang }
    val nodes = catalog.lessons.zip(lessonStatuses(ids, p, available)) { e, s -> HomeNode(e.id, e.caption ?: (e.id.removePrefix("l") + " " + e.title), s) }
    return HomeModel(visibleStreak(p, today), p.xp, nodes, dueIds(ids, p, today, available))
}
