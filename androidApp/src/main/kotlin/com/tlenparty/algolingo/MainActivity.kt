package com.tlenparty.algolingo

import android.Manifest
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        PracticeReminder.schedule(this)

        setContent {
            // debug build: every lesson open, to check new content without passing the path
            App(filesDir.absolutePath, unlockAll = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        }
    }
}
