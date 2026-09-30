package com.shapeshed.booth.ui

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileNameTest {
    @Test
    fun carriesTheDateAndTime() {
        assertEquals(
            "booth-backup-2026-09-28-14-22-33.zip",
            backupFileName(LocalDateTime.of(2026, 9, 28, 14, 22, 33)),
        )
    }

    @Test
    fun sortsChronologically() {
        // ISO-ordered digits, so a directory listing in name order is also in time order. A
        // locale-formatted name would sort by month name and lose that.
        val earlier = backupFileName(LocalDateTime.of(2026, 9, 28, 9, 5, 0))
        val later = backupFileName(LocalDateTime.of(2026, 10, 1, 19, 40, 7))
        assertTrue(earlier < later)
    }

    @Test
    fun twoExportsInTheSameMinuteDoNotCollide() {
        // The case this exists for. Without the seconds, a quick second export reuses the name and
        // the picker resolves it by suffixing or overwriting.
        val first = backupFileName(LocalDateTime.of(2026, 9, 28, 14, 22, 3))
        val second = backupFileName(LocalDateTime.of(2026, 9, 28, 14, 22, 59))
        assertNotEquals(first, second)
    }

    @Test
    fun isAlwaysAZip() {
        // The only supported export format. A name that could be mistaken for a bare JSON document
        // is a bug, so this is pinned rather than left to the extension to imply.
        assertTrue(backupFileName(LocalDateTime.of(2026, 1, 2, 3, 4, 5)).endsWith(".zip"))
    }
}
