package fr.kairossolum.pangmao.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val entryId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val entryId: Long,
    val lastViewedAt: Long = System.currentTimeMillis(),
    val viewCount: Int = 1,
)

@Entity(tableName = "query_history")
data class QueryHistoryEntity(
    @PrimaryKey val query: String,
    val lastSearchedAt: Long = System.currentTimeMillis(),
    val searchCount: Int = 1,
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val entryId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val dueEpochDay: Long,
    val intervalDays: Int = 0,
    val ease: Double = 2.5,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAt: Long? = null,
)
