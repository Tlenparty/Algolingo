package com.tlenparty.algolingo

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.core.domain.CodeLang
import kotlinx.serialization.Serializable
import com.tlenparty.algolingo.di.AppGraph
import com.tlenparty.algolingo.home.domain.homeModel
import com.tlenparty.algolingo.home.presentation.HomeScreen
import com.tlenparty.algolingo.kb.data.KbRepository
import com.tlenparty.algolingo.kb.domain.Note
import com.tlenparty.algolingo.kb.presentation.KbListScreen
import com.tlenparty.algolingo.kb.presentation.NoteScreen
import com.tlenparty.algolingo.lesson.presentation.LessonHost
import com.tlenparty.algolingo.progress.data.ProgressRepository
import com.tlenparty.algolingo.progress.data.SD_CATALOG_PATH
import com.tlenparty.algolingo.progress.data.loadCatalog
import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.progress.domain.routeLang

@Serializable
object Home

@Serializable
object SdHome

@Serializable
object KbList

@Serializable
data class NoteRoute(val id: String, val anchor: String? = null)

@Serializable
data class LessonRoute(val id: String, val review: Boolean = false, val lang: String = CodeLang.Kotlin.id)

private const val KB_ERROR = "Не удалось загрузить базу знаний"

@Composable
fun App(dataDir: String) {
    AlgoTheme {
        val progressRepo = remember { AppGraph.progress(dataDir) }
        val repo = remember { KbRepository() }
        val catalog by produceState<Catalog?>(null) { value = loadCatalog() }
        val sdCatalog by produceState<Catalog?>(null) { value = loadCatalog(SD_CATALOG_PATH) }
        val nav = rememberNavController()
        val current by nav.currentBackStackEntryAsState()
        val dest = current?.destination
        Scaffold(
            bottomBar = {
                if (dest?.hasRoute<NoteRoute>() != true && dest?.hasRoute<LessonRoute>() != true) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = dest?.hasRoute<Home>() == true,
                            onClick = { nav.switchTab(Home) },
                            icon = {},
                            label = { Text("Главная") },
                        )
                        NavigationBarItem(
                            selected = dest?.hasRoute<KbList>() == true,
                            onClick = { nav.switchTab(KbList) },
                            icon = {},
                            label = { Text("База знаний") },
                        )
                        NavigationBarItem(
                            selected = dest?.hasRoute<SdHome>() == true,
                            onClick = { nav.switchTab(SdHome) },
                            icon = {},
                            label = { Text("System Design") },
                        )
                    }
                }
            },
        ) { pad ->
            NavHost(nav, startDestination = Home, modifier = Modifier.padding(pad)) {
                composable<Home> { entry -> HomeTab(entry, catalog, progressRepo, nav) }
                composable<SdHome> { entry -> HomeTab(entry, sdCatalog, progressRepo, nav, byLang = false) }
                composable<LessonRoute> { entry ->
                    val route = entry.toRoute<LessonRoute>()
                    // Language is frozen in the route at start: switching it on Home never reloads an open lesson.
                    val lang = CodeLang.entries.firstOrNull { it.id == route.lang } ?: CodeLang.Kotlin
                    LessonHost(route.id, lang, route.review, onFinish = { progressRepo.record(route.id, it) }) { entry.ifResumed { nav.popBackStack() } }
                }
                composable<KbList> { entry ->
                    // KB loads lazily on first open (load() is idempotent), so a bad KB file never blocks Home or lessons.
                    val kb by produceState<Result<List<Note>>?>(null) { value = repo.load() }
                    when {
                        kb == null -> Text("…")
                        kb?.isFailure == true -> Text(KB_ERROR)
                        else -> KbListScreen(repo.notes, { n -> entry.ifResumed { nav.navigate(NoteRoute(n.id)) } }, search = repo::search)
                    }
                }
                composable<NoteRoute> { entry ->
                    // NoteRoute triggers its own load: Activity recreation restores NoteRoute before KbList was ever opened.
                    val kb by produceState<Result<List<Note>>?>(null) { value = repo.load() }
                    if (kb == null) { Text("…"); return@composable }
                    if (kb?.isFailure == true) { Text(KB_ERROR); return@composable }
                    val route = entry.toRoute<NoteRoute>()
                    val note = repo.byId(route.id)
                    if (note != null) NoteScreen(
                        note, route.anchor,
                        resolvable = { t -> repo.resolve(t) != null },
                        onLink = { t, a -> entry.ifResumed { repo.resolve(t)?.let { n -> nav.navigate(NoteRoute(n.id, a)) } } },
                        onBack = { entry.ifResumed { nav.popBackStack() } },
                    )
                    else Text("Заметка не найдена")
                }
            }
        }
    }
}

/** One Home screen per tab: the path and due reviews come from that tab's catalog; progress is shared. */
@Composable
private fun HomeTab(entry: NavBackStackEntry, catalog: Catalog?, progressRepo: ProgressRepository, nav: NavController, byLang: Boolean = true) {
    val progress by progressRepo.progress.collectAsStateWithLifecycle()
    // The process can outlive midnight: re-read the date on every resume, then once a minute while visible.
    var today by remember { mutableStateOf(progressRepo.today()) }
    LaunchedEffect(entry) {
        entry.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // ponytail: 1-min poll, so a midnight rollover shows up to a minute late.
            while (true) { today = progressRepo.today(); delay(60_000) }
        }
    }
    val saveFailed by progressRepo.saveFailed.collectAsStateWithLifecycle()
    val c = catalog
    val p = progress
    val lang = p?.lang ?: CodeLang.Kotlin
    val filter = if (byLang) lang else null // SD: no language filter, always routes Kotlin
    val model = if (c != null && p != null) homeModel(c, p, today, lang = filter) else null
    fun langOf(id: String) = (c?.routeLang(id, filter) ?: CodeLang.Kotlin).id
    HomeScreen(
        model,
        onStart = { id -> entry.ifResumed { nav.navigate(LessonRoute(id, lang = langOf(id))) } },
        // ponytail: review opens only the first due lesson; chain them into one session if N regularly exceeds 3.
        onReview = { model?.due?.firstOrNull()?.let { id -> entry.ifResumed { nav.navigate(LessonRoute(id, review = true, lang = langOf(id))) } } },
        saveFailed = saveFailed,
        lang = lang,
        onLang = if (byLang) { l -> progressRepo.setLang(l) } else null,
    )
}

/** Ignores clicks while the entry is transitioning out, so a double tap cannot pop/push twice. */
private fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

private fun NavController.switchTab(route: Any) = navigate(route) {
    popUpTo(Home) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
