package com.tlenparty.algolingo.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test

/** One @Test per process: AppGraph keeps the first dataDir, so all scenarios share one fresh dir. */
@OptIn(ExperimentalTestApi::class, ExperimentalForeignApi::class)
class E2eSmokeTest {
    private fun freshDir(): String {
        val dir = NSTemporaryDirectory() + "e2e-" + NSUUID().UUIDString
        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
        return dir
    }

    @Test
    fun smoke() = runComposeUiTest {
        launchApp(freshDir())
        scenarioLaunch()
    }
}
