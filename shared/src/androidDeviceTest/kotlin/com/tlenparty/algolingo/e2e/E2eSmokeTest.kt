package com.tlenparty.algolingo.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/** Each @Test gets its own fresh dataDir (AppGraph keys the repository by dir); scenarios inside one test share it. */
@OptIn(ExperimentalTestApi::class)
class E2eSmokeTest {
    private fun freshDir(): String =
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "e2e-" + System.nanoTime())
            .apply { mkdirs() }.absolutePath

    @Test
    fun smoke() = runComposeUiTest(testTimeout = 10.minutes) { // default 1m is too short for every lesson
        launchApp(freshDir())
        scenarioLaunch()
        scenarioWrongChip() // exits without saving: progress stays clean for the full run
        scenarioKb()
        scenarioSystemDesign(startXp = scenarioAllLessons())
    }

    @Test
    fun python() = runComposeUiTest(testTimeout = 3.minutes) {
        launchApp(freshDir())
        scenarioPython()
    }

    @Test
    fun pythonChain() = runComposeUiTest(testTimeout = 10.minutes) { // every Python lesson, like smoke()
        launchApp(freshDir())
        scenarioPythonChain()
    }
}
