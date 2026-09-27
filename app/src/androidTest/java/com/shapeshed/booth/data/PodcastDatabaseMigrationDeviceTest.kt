package com.shapeshed.booth.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs every Room migration in sequence and validates the result against the newest exported
 * schema.
 *
 * Migrations are the only code in this app that can destroy a user's library. Booth is
 * local-first and account-free, so there is no server-side copy: a wrong `execSQL` is a crash on
 * upgrade and unrecoverable data loss. Nothing else exercises these 26 migrations.
 *
 * This mirrors what Room actually does on upgrade. `RoomOpenHelper.onUpgrade` applies the whole
 * chain from the user's version to the current one and then validates only the final schema, so
 * this is the check that matches the production path.
 *
 * Two known gaps, both artifacts of a schema that was never released:
 *  - `app/schemas` has 1 to 4 and then 6 to 27. `5.json` was never committed, so the isolated
 *    "arrive at version 5" checkpoint cannot be set up. The 4 to 5 and 5 to 6 migrations are still
 *    covered here, and the schema they produce is validated at version 6.
 *  - `22.json` lists `podcasts.includeInAutoQueue`, but `MIGRATION_21_22` does not add it;
 *    `MIGRATION_22_23` does. The exported v22 file therefore does not describe what the v21 to v22
 *    migration produces. No user is affected, because a real upgrade runs the whole chain and the
 *    column is present by version 23, but it does mean the historical schema files are not a
 *    faithful record, and it is why this test validates the chain rather than each version in turn.
 *
 * Both disappear if versions 1 to 27 are squashed into a single v1 before v0.1.0 ships, since
 * nothing has been released (versionCode is 1 and there is no v* tag). That is a data decision
 * rather than a test decision.
 */
@RunWith(AndroidJUnit4::class)
class PodcastDatabaseMigrationDeviceTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PodcastDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratesFromVersionOneToTheCurrentSchema() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, CURRENT_VERSION, true, *ALL_MIGRATIONS)
    }

    private companion object {
        const val TEST_DB = "booth-migration-test"
        const val CURRENT_VERSION = 27
    }
}
