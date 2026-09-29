package com.shapeshed.booth.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 to v2: drops `podcasts.includeInAutoRefresh`.
 *
 * The refresh policy is no longer per podcast. Every subscribed podcast is refreshed on the
 * scheduled cadence, so the column recorded a decision that no longer exists.
 *
 * Two things make this less mechanical than it looks.
 *
 * The column cannot be dropped with `ALTER TABLE ... DROP COLUMN`, which needs SQLite 3.35, because
 * Booth supports API 26. So `podcasts` is rebuilt without it.
 *
 * And rebuilding `podcasts` on its own would silently delete every category assignment:
 * `categories_podcasts` holds `FOREIGN KEY(podcastId) REFERENCES podcasts(id) ON DELETE CASCADE`,
 * and SQLite's `DROP TABLE` is an implicit `DELETE FROM`, so dropping the parent cascades the child
 * away. Room's schema validation then passes, because rows were deleted rather than a table being
 * malformed. So the child is rebuilt too, against a temporary parent, and the old parent is only
 * dropped once nothing references it.
 *
 * The child is rebuilt a second time at the end because renaming a table does not reliably rewrite
 * foreign keys that point at it. Android's SQLite left the child's key pointing at `podcasts_v2`
 * after the parent was renamed, and Room rejected the database for it. The first attempt relied on
 * the rename doing that rewrite, which a desktop SQLite does and a device does not. This version
 * does not depend on it at all: every table is created with the name it will be validated under.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The new parent, under a temporary name because the old one is still in the way.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `podcasts_v2` (" +
                "`id` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`author` TEXT, " +
                "`feedUrl` TEXT NOT NULL, " +
                "`siteUrl` TEXT, " +
                "`descriptionHtml` TEXT, " +
                "`artworkUrl` TEXT, " +
                "`explicit` INTEGER, " +
                "`tags` TEXT NOT NULL, " +
                "`appleCategories` TEXT NOT NULL, " +
                "`appleCategoryIds` TEXT NOT NULL, " +
                "`skipStartSeconds` INTEGER NOT NULL, " +
                "`skipEndSeconds` INTEGER NOT NULL, " +
                "`playbackSpeed` REAL, " +
                "`skipSilence` INTEGER, " +
                "`includeInAutoDownload` INTEGER NOT NULL, " +
                "`includeInVideoDownload` INTEGER NOT NULL, " +
                "`includeInNotifications` INTEGER NOT NULL, " +
                "`includeInAutoQueue` INTEGER NOT NULL, " +
                "`isSubscribed` INTEGER NOT NULL DEFAULT 1, " +
                "`subscribedAtMillis` INTEGER NOT NULL, " +
                "`lastRefreshMillis` INTEGER, " +
                "`feedEtag` TEXT, " +
                "`feedLastModified` TEXT, " +
                "`lastRefreshAttemptMillis` INTEGER, " +
                "`lastRefreshError` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `podcasts_v2` (" +
                "`id`, `title`, `author`, `feedUrl`, `siteUrl`, `descriptionHtml`, `artworkUrl`, " +
                "`explicit`, `tags`, `appleCategories`, `appleCategoryIds`, `skipStartSeconds`, " +
                "`skipEndSeconds`, `playbackSpeed`, `skipSilence`, `includeInAutoDownload`, " +
                "`includeInVideoDownload`, `includeInNotifications`, `includeInAutoQueue`, " +
                "`isSubscribed`, `subscribedAtMillis`, `lastRefreshMillis`, `feedEtag`, " +
                "`feedLastModified`, `lastRefreshAttemptMillis`, `lastRefreshError`" +
                ") SELECT " +
                "`id`, `title`, `author`, `feedUrl`, `siteUrl`, `descriptionHtml`, `artworkUrl`, " +
                "`explicit`, `tags`, `appleCategories`, `appleCategoryIds`, `skipStartSeconds`, " +
                "`skipEndSeconds`, `playbackSpeed`, `skipSilence`, `includeInAutoDownload`, " +
                "`includeInVideoDownload`, `includeInNotifications`, `includeInAutoQueue`, " +
                "`isSubscribed`, `subscribedAtMillis`, `lastRefreshMillis`, `feedEtag`, " +
                "`feedLastModified`, `lastRefreshAttemptMillis`, `lastRefreshError`" +
                " FROM `podcasts`",
        )

        // The child, pointing at the temporary parent. It cannot point at `podcasts` yet, because
        // dropping `podcasts` would then cascade the rows just copied.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories_podcasts_v2` (" +
                "`categoryId` INTEGER NOT NULL, " +
                "`podcastId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`categoryId`, `podcastId`), " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`podcastId`) REFERENCES `podcasts_v2`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "INSERT INTO `categories_podcasts_v2` (`categoryId`, `podcastId`) " +
                "SELECT `categoryId`, `podcastId` FROM `categories_podcasts`",
        )

        // The old child goes first, so the old parent has no referencing child left. Dropping the
        // parent while anything still references it is the cascade this ordering exists to avoid.
        db.execSQL("DROP TABLE `categories_podcasts`")
        db.execSQL("DROP TABLE `podcasts`")
        db.execSQL("ALTER TABLE `podcasts_v2` RENAME TO `podcasts`")

        // The child is rebuilt once more so its foreign key names `podcasts` outright. Renaming the
        // parent above is not enough: on Android the key still read `podcasts_v2` afterwards, and
        // Room refuses a database whose foreign keys do not match the entities.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories_podcasts` (" +
                "`categoryId` INTEGER NOT NULL, " +
                "`podcastId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`categoryId`, `podcastId`), " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`podcastId`) REFERENCES `podcasts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL(
            "INSERT INTO `categories_podcasts` (`categoryId`, `podcastId`) " +
                "SELECT `categoryId`, `podcastId` FROM `categories_podcasts_v2`",
        )
        db.execSQL("DROP TABLE `categories_podcasts_v2`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_categories_podcasts_podcastId` " +
                "ON `categories_podcasts` (`podcastId`)",
        )
    }
}
