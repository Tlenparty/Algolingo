package com.tlenparty.algolingo

import algolingo.shared.generated.resources.Res
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tlenparty.algolingo.kb.KbRepository
import com.tlenparty.algolingo.kb.ui.KbListScreen
import com.tlenparty.algolingo.kb.ui.NoteScreen
import com.tlenparty.algolingo.ui.theme.AppTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Hello(val title: String, val subtitle: String)

private val json = Json { ignoreUnknownKeys = true }

fun parseHello(text: String): Hello = json.decodeFromString(text)

suspend fun loadHello(): Hello = parseHello(Res.readBytes("files/hello.json").decodeToString())

@Serializable
object Home

@Serializable
object KbList

@Serializable
data class NoteRoute(val id: String, val anchor: String? = null)

@Composable
@Preview
fun App() {
    AppTheme {
        val repo = remember { KbRepository() }
        val loaded by produceState(false) { repo.load(); value = true }
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val dest = entry?.destination
        Scaffold(
            bottomBar = {
                if (dest?.hasRoute<NoteRoute>() != true) {
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
                composable<Home> { HomeStub() }
                composable<KbList> {
                    if (loaded) KbListScreen(repo.notes, { nav.navigate(NoteRoute(it.id)) }) else Text("…")
                }
                composable<NoteRoute> {
                    val note = repo.byId(it.toRoute<NoteRoute>().id)
                    if (note != null) NoteScreen(note, { nav.popBackStack() })
                    else Text("Заметка не найдена")
                }
            }
        }
    }
}

private fun androidx.navigation.NavController.switchTab(route: Any) = navigate(route) {
    popUpTo(Home) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun HomeStub() {
    val hello by produceState<Hello?>(null) { value = loadHello() }
    Column(
        Modifier.safeContentPadding().fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(hello?.title ?: "…", style = MaterialTheme.typography.headlineMedium)
        Text(hello?.subtitle ?: "")
        Text(getPlatform().name)
        Text("Скоро уроки")
    }
}
