package com.tlenparty.algolingo.home.domain

import com.tlenparty.algolingo.progress.domain.Catalog
import com.tlenparty.algolingo.progress.domain.Progress
import com.tlenparty.algolingo.progress.domain.Track
import kotlinx.datetime.LocalDate

class BuildHomeModelUseCase {
    /** The path and due reviews come from the track's catalog; progress is shared between tracks. */
    operator fun invoke(track: Track, catalog: Catalog, progress: Progress, today: LocalDate): HomeModel =
        homeModel(catalog, progress, today, lang = track.langFilter(progress.lang))
}
