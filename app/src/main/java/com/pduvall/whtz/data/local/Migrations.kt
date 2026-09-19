package com.pduvall.whtz.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v3 → v4: add the nullable `rarity` column to oracle_cards (backfilled by re-import). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE oracle_cards ADD COLUMN rarity TEXT")
    }
}

/**
 * v4 → v5: bundle tokens + rulings offline and store each card's Scryfall `all_parts`.
 * The new tables are populated (and allPartsJson backfilled) by re-importing the card database.
 * SQL mirrors Room's generated schema for these entities.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE oracle_cards ADD COLUMN allPartsJson TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tokens` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameLower` TEXT NOT NULL, " +
                "`manaCost` TEXT, `typeLine` TEXT, `oracleText` TEXT, `power` TEXT, " +
                "`toughness` TEXT, `layout` TEXT NOT NULL, `artCropUrl` TEXT, " +
                "`normalImageUrl` TEXT, `cardFacesJson` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tokens_nameLower` ON `tokens` (`nameLower`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `rulings` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `oracleId` TEXT NOT NULL, " +
                "`source` TEXT, `publishedAt` TEXT, `comment` TEXT NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rulings_oracleId` ON `rulings` (`oracleId`)")
    }
}
