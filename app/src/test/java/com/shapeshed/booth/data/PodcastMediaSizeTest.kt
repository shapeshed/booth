package com.shapeshed.booth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastMediaSizeTest {
    @Test
    fun acastRangeResponseUsesTotalInsteadOfTinyContentLength() {
        assertEquals(
            30_009_176L,
            mediaTotalBytes(
                contentRange = "bytes 0-0/30009176",
                contentLength = "1",
            ),
        )
    }

    @Test
    fun tinySentinelLengthsAreRejected() {
        assertNull(mediaTotalBytes(contentRange = null, contentLength = "2"))
        assertNull(mediaTotalBytes(contentRange = "bytes 0-0/*", contentLength = "1"))
    }

    @Test
    fun normalContentLengthIsAccepted() {
        assertEquals(12_345_678L, mediaTotalBytes(contentRange = null, contentLength = "12345678"))
    }
}
