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
import androidx.lifecycle.Lifecycle
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
data class LessonRoute(val id: String)

@Composable
fun App(dataDir: String) {
    AlgoTheme {
        val progressRepo = remember(dataDir) { AppGraph.progress(dataDir) }
        val repo = remember { KbRepository() }
        val loaded by produceState(false) { repo.load(); value = true }
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
                    val progress by progressRepo.progress.collectAsStateWithLifecycle(null)
                    // ponytail: today recomputed per composition; add a LifecycleResumeEffect refresh if midnight staleness matters.
                    HomeScreen(progress, progressRepo.today()) { id -> entry.ifResumed { nav.navigate(LessonRoute(id)) } }
                }
                composable<LessonRoute> { entry ->
                    val id = entry.toRoute<LessonRoute>().id
                    LessonHost(id, onFinish = { progressRepo.record(id, it) }) { entry.ifResumed { nav.popBackStack() } }
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
