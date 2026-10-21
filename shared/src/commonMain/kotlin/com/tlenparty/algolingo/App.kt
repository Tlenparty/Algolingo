package com.tlenparty.algolingo

import algolingo.shared.generated.resources.Res
import androidx.compose.foundation.layout.Column
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Hello(val title: String, val subtitle: String)

private val json = Json { ignoreUnknownKeys = true }

fun parseHello(text: String): Hello = json.decodeFromString(text)

suspend fun loadHello(): Hello = parseHello(Res.readBytes("files/hello.json").decodeToString())

@Composable
@Preview
fun App() {
    MaterialTheme {
        val hello by produceState<Hello?>(null) { value = loadHello() }
        Column(
            Modifier.safeContentPadding().fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(hello?.title ?: "…", style = MaterialTheme.typography.headlineMedium)
            Text(hello?.subtitle ?: "")
            Text(getPlatform().name)
        }
    }
}
