package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OpmlTest {
    @Test
    fun parseOpmlFeedsReadsDirectCategoryTags() {
        val feeds = parseOpmlFeeds(
            """
            <opml version="2.0">
              <body>
                <outline text="BBC" xmlUrl="https://example.com/rss" category="news, world" />
              </body>
            </opml>
            """.trimIndent(),
        )

        assertEquals("news, world", feeds.single().tags)
    }

    @Test
    fun parseOpmlFeedsInheritsFolderTags() {
        val feeds = parseOpmlFeeds(
            """
            <opml version="2.0">
              <body>
                <outline text="Technology">
                  <outline text="Android" category="mobile">
                    <outline text="NewPipe" xmlUrl="https://github.com/TeamNewPipe/NewPipe/releases.atom" />
                  </outline>
                </outline>
              </body>
            </opml>
            """.trimIndent(),
        )

        assertEquals("Technology, Android, mobile", feeds.single().tags)
    }

    @Test
    fun importedTagsNormalizeCategoriesWithoutDuplicates() {
        assertEquals(
            "Technology, android, mobile",
            "Technology, android, technology, #mobile".normalizedPodcastTags(),
        )
    }
}
