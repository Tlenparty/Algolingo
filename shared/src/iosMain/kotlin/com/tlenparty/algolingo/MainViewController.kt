package com.tlenparty.algolingo

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun MainViewController() = ComposeUIViewController { App(documentsDir()) }

@OptIn(ExperimentalForeignApi::class)
private fun documentsDir(): String = requireNotNull(
    NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)?.path,
)
