package com.tlenparty.algolingo.e2e

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.tlenparty.algolingo.App
import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import com.tlenparty.algolingo.di.AppGraph
import com.tlenparty.algolingo.home.domain.HomeNode
import com.tlenparty.algolingo.home.domain.homeModel
import com.tlenparty.algolingo.kb.data.KbRepositoryImpl
import com.tlenparty.algolingo.kb.domain.Note
import com.tlenparty.algolingo.lesson.data.loadLesson
import com.tlenparty.algolingo.lesson.domain.Lesson
import com.tlenparty.algolingo.lesson.domain.Verdict
import com.tlenparty.algolingo.lesson.domain.xpFor
import com.tlenparty.algolingo.lesson.presentation.LessonAction
import com.tlenparty.algolingo.lesson.presentation.LessonReducer
import com.tlenparty.algolingo.lesson.presentation.LessonState
import com.tlenparty.algolingo.lesson.presentation.Phase
import com.tlenparty.algolingo.lesson.presentation.panelModel
import com.tlenparty.algolingo.lesson.presentation.screenKey
import com.tlenparty.algolingo.progress.data.CATALOG_PATH
import com.tlenparty.algolingo.progress.data.SD_CATALOG_PATH
import com.tlenparty.algolingo.progress.data.loadCatalog
import com.tlenparty.algolingo.progress.domain.LessonProgress
import com.tlenparty.algolingo.progress.domain.LessonStatus
import com.tlenparty.algolingo.progress.domain.Progress

// UI e2e scenarios, shared by the Android device test and the iOS simulator test. No test annotations here:
// commonTest also compiles into androidHostTest, where runComposeUiTest cannot run.
// Everything expected is derived from the bundled JSON and the app's own functions, never from lesson literals.

private const val WAIT = 15_000L

// Bumping it drops the whole App composition (nav, remembered state) and builds it again on the same dataDir.
private val appGeneration = mutableIntStateOf(0)

/**
 * Progress is loaded before the first frame: otherwise DataStore's first emit (on a worker) can resume the test
 * recomposer inline on that worker (Android: "multithreaded access to SnapshotStateObserver" in a second @Test).
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.launchApp(dataDir: String) {
    runBlocking { AppGraph.progress(dataDir).progress.first { it != null } }
    setContent { key(appGeneration.intValue) { App(dataDir) } }
}

/** setContent works once per test: recreate App in place instead. AppGraph keeps the repository (as on Activity recreation). */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.recreateApp() {
    appGeneration.intValue++
    waitForIdle()
}

/** Home nodes in catalog order (fresh progress), via the app's own homeModel. */
private fun nodes(path: String = CATALOG_PATH): List<HomeNode> {
    val catalog = runBlocking { loadCatalog(path) }
    check(catalog.lessons.isNotEmpty()) { "catalog is empty: Res.readBytes found no content" }
    return homeModel(catalog, Progress(), LocalDate(2000, 1, 1)).nodes
}

private fun lesson(id: String, lang: CodeLang = CodeLang.Kotlin): Lesson = runBlocking { loadLesson(id, lang) }.getOrElse { fail("lesson $id failed to load: $it") }

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
    val repo = KbRepositoryImpl()
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
fun ComposeUiTest.scenarioAllLessons(): Int {
    val all = nodes()
    assertLevel2Order(all.map { it.id }, "kotlin")
    return playCatalog(all, 0)
}

/** Level 2 opens right after l1.5, in this order; later lessons may follow l2.6. */
private val LEVEL2_ORDER = listOf("l1.5", "l2.1", "l2.2", "l2.3", "l2.4", "l2.5", "l2.6")

private fun assertLevel2Order(ids: List<String>, where: String) {
    val from = ids.indexOf("l1.5")
    assertTrue(from >= 0, "$where: l1.5 missing from catalog")
    assertEquals(LEVEL2_ORDER, ids.drop(from).take(LEVEL2_ORDER.size), "$where: level 2 order after l1.5")
}

/** System Design tab: every SD lesson, same checks; XP continues from [startXp] (progress is shared with code lessons). */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioSystemDesign(startXp: Int): Int {
    click(hasText("System Design") and hasClickAction(), "sd tab")
    return playCatalog(nodes(SD_CATALOG_PATH), startXp)
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.playCatalog(nodes: List<HomeNode>, startXp: Int, lang: CodeLang = CodeLang.Kotlin): Int {
    var xp = startXp
    nodes.forEachIndexed { i, n ->
        openLesson(n)
        val s = playLesson(lesson(n.id, lang))
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
    println("e2e: played ${nodes.size} ${lang.id} lessons through the UI, $xp XP")
    return xp
}

/**
 * Python on fresh progress: lessons the catalog ships without Python (if any) show «Скоро на Python», the first Python lesson plays to «Чисто!»,
 * the language survives App recreation, and the lesson is Done on the Kotlin path too (progress is shared).
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioPython() {
    val catalog = runBlocking { loadCatalog() }
    val py = homeModel(catalog, Progress(lang = CodeLang.Python), LocalDate(2000, 1, 1)).nodes
    val pilot = py.firstOrNull { it.status == LessonStatus.Available } ?: fail("no Python lesson in the catalog")
    // Soon only for lessons the catalog ships without Python; none once every lesson is translated (phase 10).
    val noPy = catalog.lessons.filterNot { it.has(CodeLang.Python) }.mapTo(HashSet()) { it.id }
    assertEquals(noPy, py.filter { it.status == LessonStatus.Soon }.mapTo(HashSet()) { it.id }, "python: Soon set")
    val soon = py.firstOrNull { it.id in noPy }
    click(hasTestTag("lang-python"), "lang python")
    soon?.let { onHome(hasContentDescription("Скоро на Python: ${it.caption}"), "python: ${it.id} soon") }
    openLesson(pilot)
    val s = playLesson(lesson(pilot.id, CodeLang.Python))
    waitFor(hasText("Чисто!"), "${pilot.id} py: summary")
    waitFor(hasText("+${xpFor(LessonReducer.result(s))} XP"), "${pilot.id} py: summary xp")
    click(hasText("Продолжить"), "${pilot.id} py: summary")
    onHome(hasContentDescription("Пройден, повторить: ${pilot.caption}"), "${pilot.id} py: done")

    recreateApp()
    waitFor(hasTestTag("lang-python"), "after recreate: segment")
    waitFor(hasTestTag("lang-python") and isSelected(), "lang-python selected") // selection follows the DataStore write, not compose idle
    soon?.let { onHome(hasContentDescription("Скоро на Python: ${it.caption}"), "after recreate: still python") }
    onHome(hasContentDescription("Пройден, повторить: ${pilot.caption}"), "after recreate: ${pilot.id} done")

    click(hasTestTag("lang-kotlin"), "lang kotlin")
    waitFor(hasTestTag("lang-kotlin") and isSelected(), "lang-kotlin selected") // selection follows the DataStore write, not compose idle
    onHome(hasContentDescription("Пройден, повторить: ${pilot.caption}"), "kotlin: ${pilot.id} done")
    // Kotlin path continues after pilot (progress is shared): first Available with pilot Done, not fresh nodes() (pilot may be l0.1).
    val next = homeModel(catalog, Progress(lessons = mapOf(pilot.id to LessonProgress()), lang = CodeLang.Kotlin), LocalDate(2000, 1, 1)).nodes
        .firstOrNull { it.status == LessonStatus.Available } ?: fail("kotlin: no Available lesson after ${pilot.id}")
    onHome(hasContentDescription("Начать: ${next.caption}"), "kotlin: ${next.id} next after ${pilot.id}")
    assertEquals(0, onAllNodes(hasContentDescription("Скоро на Python", substring = true)).fetchSemanticsNodes().size, "kotlin: Soon nodes left")
}

/**
 * The whole Python chain on fresh progress: no «Скоро на Python» anywhere in Python mode (every code lesson ships Python),
 * then every lesson by its Python references with the same checks as [scenarioAllLessons]. Returns total XP.
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioPythonChain(): Int {
    val catalog = runBlocking { loadCatalog() }
    val py = homeModel(catalog, Progress(lang = CodeLang.Python), LocalDate(2000, 1, 1)).nodes
    val kotlinIds = nodes().map { it.id }
    assertEquals(kotlinIds, py.map { it.id }, "python: catalog nodes")
    assertLevel2Order(py.map { it.id }, "python")
    assertEquals(emptyList(), py.filter { it.status == LessonStatus.Soon }.map { it.id }, "python: Soon nodes in the model")
    assertEquals(emptyList(), catalog.lessons.filterNot { it.has(CodeLang.Python) }.map { it.id }, "python: lessons without Python")
    click(hasTestTag("lang-python"), "lang python")
    waitFor(hasTestTag("lang-python") and isSelected(), "lang-python selected") // selection follows the DataStore write, not compose idle
    waitFor(hasScrollAction(), "python: Home list")
    assertEquals(0, onAllNodes(hasContentDescription("Скоро на Python", substring = true)).fetchSemanticsNodes().size, "python: Soon nodes on Home")
    val xp = playCatalog(py, 0, CodeLang.Python)
    assertEquals(0, onAllNodes(hasContentDescription("Скоро на Python", substring = true)).fetchSemanticsNodes().size, "python: Soon nodes after the chain")
    println("e2e: python chain ${py.first().id}..${py.last().id} (${py.size} lessons) done")
    return xp
}
