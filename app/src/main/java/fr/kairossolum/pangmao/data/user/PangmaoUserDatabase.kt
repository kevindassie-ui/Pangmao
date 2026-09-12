package fr.kairossolum.pangmao.data.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class, QueryHistoryEntity::class, FlashcardEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PangmaoUserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        fun create(context: Context): PangmaoUserDatabase = Room.databaseBuilder(
            context.applicationContext,
            PangmaoUserDatabase::class.java,
            "pangmao_user.db",
        ).addMigrations(MIGRATION_1_2).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `query_history` (
                        `query` TEXT NOT NULL,
                        `lastSearchedAt` INTEGER NOT NULL,
                        `searchCount` INTEGER NOT NULL,
                        PRIMARY KEY(`query`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
