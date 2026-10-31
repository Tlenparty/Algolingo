package com.tlenparty.algolingo.kb.presentation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.tlenparty.algolingo.kb.domain.KbRepository
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Survives LazyColumn item disposal so scrolling back does not re-decode and re-grow the row. */
class KbImages(private val repo: KbRepository) {
    // ponytail: unbounded, fine for the bundled few dozen images; switch to an LRU if the bundle grows large.
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun cached(file: String): ImageBitmap? = cache[file]

    /** null = the image is missing or broken; the note shows a placeholder. */
    suspend fun load(file: String): ImageBitmap? = try {
        repo.image(file).decodeToImageBitmap().also { cache[file] = it }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }
}

val LocalKbImages = staticCompositionLocalOf<KbImages?> { null }
