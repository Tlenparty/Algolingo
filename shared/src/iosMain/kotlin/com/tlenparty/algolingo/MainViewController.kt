package com.tlenparty.algolingo

import androidx.compose.ui.window.ComposeUIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Debug framework (Xcode Debug configuration) opens every lesson, like FLAG_DEBUGGABLE on Android. */
@OptIn(ExperimentalNativeApi::class)
fun MainViewController() = ComposeUIViewController { App(documentsDir(), unlockAll = kotlin.native.Platform.isDebugBinary) }

@OptIn(ExperimentalForeignApi::class)
private fun documentsDir(): String = requireNotNull(
    NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)?.path,
)
