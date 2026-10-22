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
import com.tlenparty.algolingo.home.HomeScreen
import com.tlenparty.algolingo.kb.KbRepository
import com.tlenparty.algolingo.kb.ui.KbListScreen
import com.tlenparty.algolingo.kb.ui.NoteScreen
import com.tlenparty.algolingo.lesson.ui.LessonHost
import com.tlenparty.algolingo.progress.AppGraph
import com.tlenparty.algolingo.progress.Catalog
import com.tlenparty.algolingo.progress.homeModel
import com.tlenparty.algolingo.progress.loadCatalog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tlenparty.algolingo.design.AlgoTheme
import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object KbList

@Serializable
data class NoteRoute(val id: String, val anchor: String? = null)

@Serializable
data class LessonRoute(val id: String, val review: Boolean = false)

@Composable
fun App(dataDir: String) {
    AlgoTheme {
        val progressRepo = remember(dataDir) { AppGraph.progress(dataDir) }
        val repo = remember { KbRepository() }
        val loaded by produceState(false) { repo.load(); value = true }
        val catalog by produceState<Catalog?>(null) { value = loadCatalog() }
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val dest = entry?.destination
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
                    }
                }
            },
        ) { pad ->
            NavHost(nav, startDestination = Home, modifier = Modifier.padding(pad)) {
                composable<Home> { entry ->
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
                    val model = if (c != null && p != null) homeModel(c, p, today) else null
                    HomeScreen(
                        model,
                        onStart = { id -> entry.ifResumed { nav.navigate(LessonRoute(id)) } },
                        // ponytail: review opens only the first due lesson; chain them into one session if N regularly exceeds 3.
                        onReview = { model?.due?.firstOrNull()?.let { id -> entry.ifResumed { nav.navigate(LessonRoute(id, review = true)) } } },
                        saveFailed = saveFailed,
                    )
                }
                composable<LessonRoute> { entry ->
                    val route = entry.toRoute<LessonRoute>()
                    LessonHost(route.id, route.review, onFinish = { progressRepo.record(route.id, it) }) { entry.ifResumed { nav.popBackStack() } }
                }
                composable<KbList> { entry ->
                    if (loaded) KbListScreen(repo.notes, { n -> entry.ifResumed { nav.navigate(NoteRoute(n.id)) } }, search = repo::search) else Text("…")
                }
                composable<NoteRoute> { entry ->
                    // Reading `loaded` here re-composes this entry once notes arrive (Activity recreation restores NoteRoute first).
                    if (!loaded) { Text("…"); return@composable }
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

/** Ignores clicks while the entry is transitioning out, so a double tap cannot pop/push twice. */
private fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

private fun androidx.navigation.NavController.switchTab(route: Any) = navigate(route) {
    popUpTo(Home) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
