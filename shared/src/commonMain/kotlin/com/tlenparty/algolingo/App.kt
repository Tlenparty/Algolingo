package com.tlenparty.algolingo

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tlenparty.algolingo.core.domain.CodeLang
import com.tlenparty.algolingo.design.AlgoTheme
import com.tlenparty.algolingo.di.AppContainer
import com.tlenparty.algolingo.di.AppGraph
import com.tlenparty.algolingo.home.presentation.HomeHost
import com.tlenparty.algolingo.kb.presentation.KbListHost
import com.tlenparty.algolingo.kb.presentation.NoteHost
import com.tlenparty.algolingo.lesson.presentation.LessonHost
import com.tlenparty.algolingo.progress.domain.Track
import kotlinx.serialization.Serializable

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

@Composable
fun App(dataDir: String) {
    AlgoTheme {
        val graph = remember { AppGraph.container(dataDir) }
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
                composable<Home> { entry -> HomeTab(entry, graph, nav, Track.Code) }
                composable<SdHome> { entry -> HomeTab(entry, graph, nav, Track.SystemDesign) }
                composable<LessonRoute> { entry ->
                    val route = entry.toRoute<LessonRoute>()
                    // Language is frozen in the route at start: switching it on Home never reloads an open lesson.
                    val lang = CodeLang.entries.firstOrNull { it.id == route.lang } ?: CodeLang.Kotlin
                    val vm = viewModel(key = "${route.id}/${route.review}/${lang.id}") { graph.lessonViewModel(route.id, lang, route.review) }
                    LessonHost(vm, lang) { entry.ifResumed { nav.popBackStack() } }
                }
                composable<KbList> { entry ->
                    KbListHost(viewModel { graph.kbListViewModel() }) { n -> entry.ifResumed { nav.navigate(NoteRoute(n.id)) } }
                }
                composable<NoteRoute> { entry ->
                    val route = entry.toRoute<NoteRoute>()
                    NoteHost(
                        viewModel { graph.noteViewModel(route.id) }, graph.kbImages, route.anchor,
                        onOpenNote = { n -> entry.ifResumed { nav.navigate(NoteRoute(n.id, n.anchor)) } },
                        onBack = { entry.ifResumed { nav.popBackStack() } },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTab(entry: NavBackStackEntry, graph: AppContainer, nav: NavController, track: Track) {
    HomeHost(viewModel { graph.homeViewModel(track) }, showLang = track == Track.Code) { open ->
        entry.ifResumed { nav.navigate(LessonRoute(open.id, open.review, open.lang.id)) }
    }
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
