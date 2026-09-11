package fr.kairossolum.pangmao.data.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class, FlashcardEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PangmaoUserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        fun create(context: Context): PangmaoUserDatabase = Room.databaseBuilder(
            context.applicationContext,
            PangmaoUserDatabase::class.java,
            "pangmao_user.db",
        ).build()
    }
}

