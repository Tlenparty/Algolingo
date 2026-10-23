package com.tlenparty.algolingo.e2e

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.tlenparty.algolingo.App
import com.tlenparty.algolingo.progress.Progress
import com.tlenparty.algolingo.progress.homeModel
import com.tlenparty.algolingo.progress.loadCatalog
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

// UI e2e scenarios, shared by the Android device test and the iOS simulator test. No @Test here:
// commonTest also compiles into androidHostTest, where runComposeUiTest cannot run.

private const val WAIT = 15_000L

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.launchApp(dataDir: String) = setContent { App(dataDir) }

/** Home captions in catalog order, via the app's own homeModel. */
private fun captions(): List<String> {
    val catalog = runBlocking { loadCatalog() }
    check(catalog.lessons.isNotEmpty()) { "catalog is empty: Res.readBytes found no content" }
    return homeModel(catalog, Progress(), LocalDate(2000, 1, 1)).nodes.map { it.caption }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.scenarioLaunch() {
    waitUntilAtLeastOneExists(hasContentDescription("Начать: ${captions().first()}"), WAIT)
}
