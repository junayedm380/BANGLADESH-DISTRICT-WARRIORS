package com.example.game.save

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlayerSaveEntity::class,
        PlayerAccountEntity::class,
        FriendLinkEntity::class,
        ModerationReportEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
    abstract fun socialAccountDao(): SocialAccountDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_save ADD COLUMN secondaryWeaponId TEXT NOT NULL DEFAULT 'pistol_p92'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN meleeWeaponId TEXT NOT NULL DEFAULT 'melee_knife'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN selectedHeadwearId TEXT NOT NULL DEFAULT 'head_none'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN selectedAccessoryId TEXT NOT NULL DEFAULT 'acc_none'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN selectedColorTintId TEXT NOT NULL DEFAULT 'tint_default'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN opticAttachmentId TEXT NOT NULL DEFAULT 'optic_red_dot'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN muzzleAttachmentId TEXT NOT NULL DEFAULT 'muzzle_none'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN magazineAttachmentId TEXT NOT NULL DEFAULT 'mag_standard'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN underbarrelAttachmentId TEXT NOT NULL DEFAULT 'grip_none'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN aiDifficultyId TEXT NOT NULL DEFAULT 'normal'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN audioEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS player_accounts (
                        playerId TEXT NOT NULL PRIMARY KEY,
                        username TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        passwordSalt TEXT NOT NULL,
                        xp INTEGER NOT NULL DEFAULT 0,
                        matchesPlayed INTEGER NOT NULL DEFAULT 0,
                        matchesWon INTEGER NOT NULL DEFAULT 0,
                        eliminations INTEGER NOT NULL DEFAULT 0,
                        isSignedIn INTEGER NOT NULL DEFAULT 0,
                        createdAtEpochMs INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS friend_links (
                        friendPlayerId TEXT NOT NULL,
                        ownerPlayerId TEXT NOT NULL,
                        friendUsername TEXT NOT NULL,
                        statusId TEXT NOT NULL,
                        isMuted INTEGER NOT NULL DEFAULT 0,
                        updatedAtEpochMs INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(ownerPlayerId, friendPlayerId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS moderation_reports (
                        reportId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reporterPlayerId TEXT NOT NULL,
                        targetPlayerId TEXT NOT NULL,
                        targetUsername TEXT NOT NULL,
                        reasonId TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_save ADD COLUMN currentDistrictId TEXT NOT NULL DEFAULT 'dhaka'")
                db.execSQL("ALTER TABLE player_save ADD COLUMN completedMissionDistrictIdsCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE player_save ADD COLUMN configurableDistrictOrderCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE player_save ADD COLUMN discoveredTravelNodesCsv TEXT NOT NULL DEFAULT 'dhaka'")
            }
        }

        @Volatile
        private var instance: GameDatabase? = null

        fun getInstance(context: Context): GameDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "district_warriors_save.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
