package com.shapeshed.booth.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 to v2: drops `podcasts.includeInAutoRefresh`.
 *
 * The refresh policy is no longer per podcast. Every subscribed podcast is refreshed on the
 * scheduled cadence, so the column recorded a decision that no longer exists.
 *
 * The column cannot simply be dropped with `ALTER TABLE ... DROP COLUMN`, because that needs SQLite
 * 3.35 and Booth supports API 26. Nor can this rebuild `podcasts` on its own, because
 * `categories_podcasts` holds `FOREIGN KEY(podcastId) REFERENCES podcasts(id) ON DELETE CASCADE`,
 * and SQLite's `DROP TABLE` is an implicit `DELETE FROM`: dropping the parent would cascade away
 * every category assignment the user had. The child is therefore rebuilt against the new table
 * first, so that when the old parent is dropped nothing references it and there is nothing to
 * cascade to.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite cannot drop a column on the oldest supported release, so the table is rebuilt
        // without it. Column order and types must match what Room expects for v2 exactly.
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

        // Rebuild the child against the new parent, so the old parent has no referencing child left
        // when it is dropped. Without this, the drop cascades and every category assignment is lost.
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
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_categories_podcasts_v2_podcastId` " +
                "ON `categories_podcasts_v2` (`podcastId`)",
        )

        // Safe now in this order: the child referencing `podcasts` is already gone, and the only
        // table referencing `podcasts_v2` is the new child.
        db.execSQL("DROP TABLE `categories_podcasts`")
        db.execSQL("DROP TABLE `podcasts`")

        // Renaming rewrites the new child's foreign key onto the final table name.
        db.execSQL("ALTER TABLE `podcasts_v2` RENAME TO `podcasts`")
        db.execSQL("ALTER TABLE `categories_podcasts_v2` RENAME TO `categories_podcasts`")
        db.execSQL("DROP INDEX IF EXISTS `index_categories_podcasts_v2_podcastId`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_categories_podcasts_podcastId` " +
                "ON `categories_podcasts` (`podcastId`)",
        )
    }
}
