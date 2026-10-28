package com.tlenparty.algolingo.e2e

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.tlenparty.algolingo.App
import com.tlenparty.algolingo.kb.KbRepository
import com.tlenparty.algolingo.kb.Note
import com.tlenparty.algolingo.lesson.Lesson
import com.tlenparty.algolingo.lesson.LessonAction
import com.tlenparty.algolingo.lesson.LessonReducer
import com.tlenparty.algolingo.lesson.LessonState
import com.tlenparty.algolingo.lesson.Phase
import com.tlenparty.algolingo.lesson.Verdict
import com.tlenparty.algolingo.lesson.loadLesson
import com.tlenparty.algolingo.lesson.panelModel
import com.tlenparty.algolingo.lesson.screenKey
import com.tlenparty.algolingo.lesson.xpFor
import com.tlenparty.algolingo.progress.CATALOG_PATH
import com.tlenparty.algolingo.progress.HomeNode
import com.tlenparty.algolingo.progress.Progress
import com.tlenparty.algolingo.progress.SD_CATALOG_PATH
import com.tlenparty.algolingo.progress.homeModel
import com.tlenparty.algolingo.progress.loadCatalog
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

// UI e2e scenarios, shared by the Android device test and the iOS simulator test. No test annotations here:
// commonTest also compiles into androidHostTest, where runComposeUiTest cannot run.
// Everything expected is derived from the bundled JSON and the app's own functions, never from lesson literals.

private const val WAIT = 15_000L

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.launchApp(dataDir: String) = setContent { App(dataDir) }

/** Home nodes in catalog order (fresh progress), via the app's own homeModel. */
private fun nodes(path: String = CATALOG_PATH): List<HomeNode> {
    val catalog = runBlocking { loadCatalog(path) }
    check(catalog.lessons.isNotEmpty()) { "catalog is empty: Res.readBytes found no content" }
    return homeModel(catalog, Progress(), LocalDate(2000, 1, 1)).nodes
}

private fun lesson(id: String): Lesson = runBlocking { loadLesson(id) }.getOrElse { fail("lesson $id failed to load: $it") }

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.click(m: SemanticsMatcher, where: String) {
    val nodes = onAllNodes(m) // fetch waits for idle first
    if (nodes.fetchSemanticsNodes().isEmpty()) fail("$where: no node ${m.description}")
    nodes.onFirst().performClick()
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.waitFor(m: SemanticsMatcher, where: String) {
    try {
        waitUntilAtLeastOneExists(m, WAIT)
    } catch (e: Throwable) {
        fail("$where: timed out waiting for ${m.description} ($e)")
    }
}

/** Home path is a LazyColumn: bring the node into composition first (stats row is outside it: use waitFor). */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.onHome(m: SemanticsMatcher, where: String) {
    waitFor(hasScrollAction(), "$where: Home list")
    onAllNodes(hasScrollAction()).onFirst().performScrollToNode(m)
    waitForIdle()
    waitFor(m, where)
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.openLesson(n: HomeNode) {
    println("e2e: open ${n.id}")
    val m = hasContentDescription("Начать: ${n.caption}")
    onHome(m, "Home ${n.id}")
    click(m, "Home ${n.id}")
    waitFor(hasText("Дальше") or hasText("Проверить"), "${n.id}: first screen")
}

/** Put [text] into hole [hole] instead of ok[0] on the first play of [exId]. */
data class WrongAt(val exId: String, val hole: Int, val text: String)

/**
 * Plays [lesson] through the UI with a headless LessonReducer in lockstep (same actions as LessonFuzzTest's
 * smart player). With [wrongAt]: asserts the wrong-chip feedback, then returns once that exercise is solved.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.playLesson(lesson: Lesson, wrongAt: WrongAt? = null): LessonState {
    var s = LessonReducer.start(lesson, 0)
    var wrongPlayed = false
    var wrongSeen = false
    var step = 0
    while (true) {
        val exId = s.queue.firstOrNull()
        val where = "${lesson.id} ex=$exId step=${step++} phase=${s.phase::class.simpleName}"
        if (step > 200) fail("$where: lesson did not finish")
        fun act(m: SemanticsMatcher, a: LessonAction) { click(m, where); s = LessonReducer.reduce(s, a) }
        when (val p = s.phase) {
            is Phase.Theory -> act(hasText("Дальше"), LessonAction.NextTheory(p.index))
            Phase.Solving -> {
                val ex = lesson.exercises.first { it.id == exId }
                val wrong = wrongAt != null && !wrongPlayed && ex.id == wrongAt.exId
                ex.holes.forEachIndexed { h, hole ->
                    val wanted = if (wrong && h == wrongAt!!.hole) wrongAt.text else hole.ok[0]
                    val holeM = hasContentDescription("Дырка ${h + 1}:", substring = true)
                    onAllNodes(holeM).onFirst().performScrollTo()
                    act(holeM, LessonAction.TapHole(h))
                    val idx = ex.bank.indices.firstOrNull { ex.bank[it].text == wanted && !s.current!!.isPlaced(it) }
                        ?: fail("$where: no free bank chip '$wanted' for hole $h")
                    act(hasTestTag("chip") and hasText(wanted) and hasClickAction(), LessonAction.TapChip(idx))
                }
                if (wrong) wrongPlayed = true
                act(hasText("Проверить"), LessonAction.Check)
            }
            is Phase.Feedback -> {
                val m = panelModel(p.check)
                waitFor(hasText(m.title), where)
                onAllNodes(hasText(m.title)).onFirst().assertIsDisplayed()
                if (exId != wrongAt?.exId && p.check.verdict == Verdict.Wrong) fail("$where: ok[0] chips gave ❌ (${m.wrong.map { it.chip }})")
                if (wrongAt != null && exId == wrongAt.exId) {
                    if (!wrongPlayed) fail("$where: feedback before the wrong chip was played")
                    if (p.check.verdict == Verdict.Wrong) {
                        wrongSeen = true
                        assertEquals("❌ Не совсем", m.title, where)
                        // richText strips the markup; match a plain prefix of the reason.
                        val reason = m.wrong.first().text.replace("`", "").replace("**", "").takeWhile { it !in "*=[" }.take(40).trim()
                        assertTrue(reason.isNotEmpty(), "$where: empty reason")
                        waitFor(hasText(reason, substring = true), "$where: reason")
                        onAllNodes(hasText(reason, substring = true)).onFirst().assertIsDisplayed()
                        waitFor(hasContentDescription("Дырка ${wrongAt.hole + 1}:", substring = true) and
                            hasContentDescription(": ошибка", substring = true), "$where: wrong hole")
                    } else {
                        // the first check of this exercise had the wrong chip: ✅ there means it was accepted
                        assertTrue(wrongSeen, "$where: wrong chip '${wrongAt.text}' in hole ${wrongAt.hole + 1} was accepted")
                        assertEquals("✅ Отлично!", m.title, where)
                        return s
                    }
                }
                act(hasText(m.button), LessonAction.Continue(screenKey(s)))
            }
            is Phase.Reveal -> act(hasText("Продолжить"), LessonAction.Continue(screenKey(s)))
            Phase.Done -> return s
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioLaunch() {
    waitFor(hasContentDescription("Начать: ${nodes().first().caption}"), "launch")
}

/** Wrong chip -> «❌ Не совсем» + reason -> same exercise solved -> exit without saving. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioWrongChip() {
    val node = nodes().first()
    val l = lesson(node.id)
    val at = l.exercises.firstNotNullOfOrNull { ex ->
        ex.holes.withIndex().firstNotNullOfOrNull { (h, hole) ->
            hole.wrongWhy.keys.firstOrNull { w ->
                val others = ex.holes.withIndex().count { (j, o) -> j != h && o.ok[0] == w }
                ex.bank.count { it.text == w } > others
            }?.let { WrongAt(ex.id, h, it) }
        }
    } ?: fail("${l.id}: no wrongWhy chip that can be placed without starving another hole")
    openLesson(node)
    playLesson(l, at)
    click(hasContentDescription("Закрыть урок"), "${l.id}: close")
    click(hasText("Выйти"), "${l.id}: exit dialog")
    waitFor(hasContentDescription("Опыт: 0 XP"), "after exit: nothing saved")
    onHome(hasContentDescription("Начать: ${node.caption}"), "after exit: lesson still open")
}

/** KB: section expands, a note opens, search shows a hit. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioKb() {
    val repo = KbRepository()
    val notes: List<Note> = runBlocking { repo.load() }.getOrElse { fail("kb failed to load: $it") }
    val (section, items) = notes.groupBy { it.section }.entries.first()
    click(hasText("База знаний"), "kb tab")
    waitFor(hasText("▸ ", substring = true), "kb sections")
    click(hasText("▸ $section (${items.size})"), "kb section $section")
    waitFor(hasText("▾ $section (${items.size})"), "kb section open")
    val note = items.first()
    click(hasText(note.title) and hasClickAction(), "kb note ${note.id}")
    waitFor(hasText("Назад"), "note screen")
    waitFor(hasText(note.title, substring = true), "note title ${note.id}")
    click(hasText("Назад"), "note back")
    val query = note.title.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("").split(' ').firstOrNull { it.length >= 4 && repo.search(it).isNotEmpty() }
        ?: fail("no searchable word in '${note.title}'")
    val hit = repo.search(query).first().note.title
    waitFor(hasSetTextAction(), "search field")
    onAllNodes(hasSetTextAction()).onFirst().performTextInput(query)
    waitForIdle()
    waitFor(hasText(hit), "search '$query'")
    onAllNodes(hasText(hit)).onFirst().assertIsDisplayed()
    assertEquals(0, onAllNodes(hasText("Ничего не найдено")).fetchSemanticsNodes().size, "search '$query' shows no-hits")
    click(hasText("Главная"), "home tab")
}

/** Every code-catalog lesson from clean progress: grade «Чисто!», XP adds up, streak 1, next lesson unlocks. Returns total XP. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioAllLessons(): Int = playCatalog(nodes(), 0)

/** System Design tab: every SD lesson, same checks; XP continues from [startXp] (progress is shared with code lessons). */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioSystemDesign(startXp: Int): Int {
    click(hasText("System Design") and hasClickAction(), "sd tab")
    return playCatalog(nodes(SD_CATALOG_PATH), startXp)
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.playCatalog(nodes: List<HomeNode>, startXp: Int): Int {
    var xp = startXp
    nodes.forEachIndexed { i, n ->
        openLesson(n)
        val s = playLesson(lesson(n.id))
        val gained = xpFor(LessonReducer.result(s))
        xp += gained
        waitFor(hasText("Чисто!"), "${n.id}: summary")
        waitFor(hasText("+$gained XP"), "${n.id}: summary xp")
        click(hasText("Продолжить"), "${n.id}: summary")
        waitFor(hasContentDescription("Опыт: $xp XP"), "${n.id}: home xp")
        waitFor(hasContentDescription("Серия дней: 1"), "${n.id}: streak")
        onHome(hasContentDescription("Пройден, повторить: ${n.caption}"), "${n.id}: done")
        nodes.getOrNull(i + 1)?.let { next ->
            onHome(hasContentDescription("Начать: ${next.caption}"), "${next.id}: unlocked")
            assertEquals(0, onAllNodes(hasContentDescription("Закрыт: ${next.caption}")).fetchSemanticsNodes().size, "${next.id}: still locked")
        }
    }
    println("e2e: played ${nodes.size} lessons through the UI, $xp XP")
    return xp
}
