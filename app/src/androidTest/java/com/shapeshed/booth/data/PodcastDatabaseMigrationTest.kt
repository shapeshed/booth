package com.shapeshed.booth.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the v1 to v2 migration, which drops `podcasts.includeInAutoRefresh`.
 *
 * The reason this is an instrumented test rather than a unit test is the part that is easy to get
 * wrong and impossible to see: `categories_podcasts` holds
 * `FOREIGN KEY(podcastId) REFERENCES podcasts(id) ON DELETE CASCADE`, and SQLite's `DROP TABLE` is
 * an implicit `DELETE FROM`. A migration that rebuilds `podcasts` without regard for that cascades
 * every category assignment away, and the schema validation then passes, because the rows were
 * deleted rather than the table being malformed. A test that only checks the column is gone would
 * be green throughout that failure.
 */
@RunWith(AndroidJUnit4::class)
class PodcastDatabaseMigrationTest {
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PodcastDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratesWithoutLosingCategoryAssignments() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO podcasts (id, title, feedUrl, tags, appleCategories, appleCategoryIds, " +
                    "skipStartSeconds, skipEndSeconds, includeInAutoRefresh, includeInAutoDownload, " +
                    "includeInVideoDownload, includeInNotifications, includeInAutoQueue, " +
                    "isSubscribed, subscribedAtMillis) VALUES " +
                    "(1, 'Kept', 'https://example.com/feed', '', '', '', 0, 0, 0, 1, 1, 0, 0, 1, 100)",
            )
            execSQL(
                "INSERT INTO directory_providers (id, name) VALUES (1, 'Apple')",
            )
            execSQL(
                "INSERT INTO categories (id, providerId, name, externalId, parentId) "
                        + "VALUES (1, 1, 'Technology', NULL, NULL)",
            )
            execSQL("INSERT INTO categories_podcasts (categoryId, podcastId) VALUES (1, 1)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // The column is gone, and the row it was on survived.
        migrated.query("PRAGMA table_info('podcasts')").use {
            while (it.moveToNext()) {
                assertFalse(it.getString(1) == "includeInAutoRefresh")
            }
        }
        migrated.query("SELECT title, includeInAutoDownload FROM podcasts").use {
            assertTrue(it.moveToFirst())
            assertEquals("Kept", it.getString(0))
            assertEquals(1, it.getInt(1))
        }

        // The part a schema check alone would miss.
        migrated.query("SELECT categoryId, podcastId FROM categories_podcasts").use {
            assertTrue("category assignment was cascaded away by the table rebuild", it.moveToFirst())
            assertEquals(1, it.getInt(0))
            assertEquals(1, it.getInt(1))
            assertFalse("expected exactly one category assignment", it.moveToNext())
        }
        migrated.query("SELECT name FROM categories").use {
            assertTrue(it.moveToFirst())
            assertEquals("Technology", it.getString(0))
        }

        // The index the child table needs must be rebuilt too, or its lookups go unindexed.
        migrated.query("PRAGMA index_list('categories_podcasts')").use {
            var found = false
            while (it.moveToNext()) {
                if (it.getString(1) == "index_categories_podcasts_podcastId") found = true
            }
            assertTrue("index_categories_podcasts_podcastId was not recreated", found)
        }
        migrated.close()
    }

    @Test
    fun migratedDatabasePassesRoomValidation() {
        // Opening at v2 with the real entity set proves the rebuilt table matches what Room expects,
        // which is a different check from the columns merely being absent.
        helper.createDatabase(VALIDATION_DB, 1).apply {
            execSQL(
                "INSERT INTO podcasts (id, title, feedUrl, tags, appleCategories, appleCategoryIds, " +
                    "skipStartSeconds, skipEndSeconds, includeInAutoRefresh, includeInAutoDownload, " +
                    "includeInVideoDownload, includeInNotifications, includeInAutoQueue, " +
                    "isSubscribed, subscribedAtMillis) VALUES " +
                    "(1, 'Kept', 'https://example.com/feed', '', '', '', 0, 0, 0, 1, 1, 0, 0, 1, 100)",
            )
            close()
        }
        helper.runMigrationsAndValidate(VALIDATION_DB, 2, true, MIGRATION_1_2).close()
        // No exception from createDatabase is the assertion: it validates the schema hash on open.
        helper.createDatabase(VALIDATION_DB, 2).use { it.query("SELECT COUNT(*) FROM podcasts").close() }
    }

    private companion object {
        const val TEST_DB = "migration-cascade-test"
        const val VALIDATION_DB = "migration-validation-test"
    }
}
