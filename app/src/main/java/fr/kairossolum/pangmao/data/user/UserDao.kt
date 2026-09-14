package fr.kairossolum.pangmao.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT entryId FROM favorites ORDER BY addedAt DESC")
    fun favoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE entryId = :entryId)")
    fun isFavorite(entryId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE entryId = :entryId")
    suspend fun deleteFavorite(entryId: Long)

    @Query("SELECT entryId FROM history ORDER BY lastViewedAt DESC LIMIT :limit")
    fun historyIds(limit: Int = 100): Flow<List<Long>>

    @Query("SELECT * FROM history WHERE entryId = :entryId LIMIT 1")
    suspend fun history(entryId: Long): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: HistoryEntity)

    @Transaction
    suspend fun recordHistory(entryId: Long) {
        val previous = history(entryId)
        upsertHistory(
            HistoryEntity(
                entryId = entryId,
                lastViewedAt = System.currentTimeMillis(),
                viewCount = (previous?.viewCount ?: 0) + 1,
            )
        )
    }

    @Query("SELECT * FROM query_history ORDER BY lastSearchedAt DESC LIMIT :limit")
    fun queryHistory(limit: Int = 20): Flow<List<QueryHistoryEntity>>

    @Query("SELECT * FROM query_history WHERE `query` = :query LIMIT 1")
    suspend fun queryHistoryItem(query: String): QueryHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQueryHistory(item: QueryHistoryEntity)

    @Transaction
    suspend fun recordQuery(query: String) {
        val previous = queryHistoryItem(query)
        upsertQueryHistory(
            QueryHistoryEntity(
                query = query,
                lastSearchedAt = System.currentTimeMillis(),
                searchCount = (previous?.searchCount ?: 0) + 1,
            )
        )
    }

    @Query("DELETE FROM query_history WHERE `query` = :query")
    suspend fun deleteQuery(query: String)

    @Query("DELETE FROM query_history")
    suspend fun clearQueryHistory()

    @Query("SELECT * FROM flashcards ORDER BY dueEpochDay, createdAt")
    fun flashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE dueEpochDay <= :today ORDER BY dueEpochDay, createdAt")
    fun dueFlashcards(today: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM flashcards WHERE entryId = :entryId)")
    fun isFlashcard(entryId: Long): Flow<Boolean>

    @Query("SELECT * FROM flashcards WHERE entryId = :entryId LIMIT 1")
    suspend fun flashcard(entryId: Long): FlashcardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFlashcard(card: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlashcard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE entryId = :entryId")
    suspend fun deleteFlashcard(entryId: Long)
}
