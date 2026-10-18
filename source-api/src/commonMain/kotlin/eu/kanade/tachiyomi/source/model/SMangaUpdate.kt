package eu.kanade.tachiyomi.source.model

/**
 * Returned by [eu.kanade.tachiyomi.source.MangaSource.getMangaUpdate], bundling the manga details
 * and its chapter list so a source can supply both from a single round trip.
 *
 * @since extensions-lib 1.6
 */
@Suppress("UNUSED")
class SMangaUpdate(val manga: SManga, val chapters: List<SChapter>)
