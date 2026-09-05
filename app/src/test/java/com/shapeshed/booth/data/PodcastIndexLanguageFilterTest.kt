package com.shapeshed.booth.data

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastIndexLanguageFilterTest {
    @Test
    fun `regional locale includes regional and base language`() {
        assertEquals("en-gb,en", podcastIndexLanguageFilter(Locale.UK))
        assertEquals("en-us,en", podcastIndexLanguageFilter(Locale.US))
        assertEquals("fr-fr,fr", podcastIndexLanguageFilter(Locale.FRANCE))
        assertEquals("fr-ma,fr", podcastIndexLanguageFilter(Locale.Builder().setLanguage("fr").setRegion("MA").build()))
    }

    @Test
    fun `language-only locale is not duplicated`() {
        assertEquals("fr", podcastIndexLanguageFilter(Locale.FRENCH))
    }
}
