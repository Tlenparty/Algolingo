package com.tlenparty.algolingo.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.test.Test

/** One @Test per process: AppGraph keeps the first dataDir, so all scenarios share one fresh dir. */
@OptIn(ExperimentalTestApi::class)
class E2eSmokeTest {
    private fun freshDir(): String =
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "e2e-" + System.nanoTime())
            .apply { mkdirs() }.absolutePath

    @Test
    fun smoke() = runComposeUiTest {
        launchApp(freshDir())
        scenarioLaunch()
    }
}
