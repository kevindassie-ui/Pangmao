package fr.kairossolum.pangmao.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.kairossolum.pangmao.domain.model.WordKnowledge
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus

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

@Entity(tableName = "word_knowledge")
data class WordKnowledgeEntity(
    @PrimaryKey val entryId: Long,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

internal fun WordKnowledgeStatus.toEntity(
    entryId: Long,
    updatedAt: Long = System.currentTimeMillis(),
): WordKnowledgeEntity? = takeUnless { it == WordKnowledgeStatus.UNMARKED }?.let { status ->
    WordKnowledgeEntity(entryId = entryId, status = status.name, updatedAt = updatedAt)
}

internal fun WordKnowledgeEntity.toModel(): WordKnowledge? {
    val parsedStatus = storedWordKnowledgeStatus(status)
    return if (parsedStatus == WordKnowledgeStatus.UNMARKED) null else {
        WordKnowledge(entryId = entryId, status = parsedStatus, updatedAt = updatedAt)
    }
}

internal fun storedWordKnowledgeStatus(value: String?): WordKnowledgeStatus =
    WordKnowledgeStatus.entries.firstOrNull { status -> status.name == value }
        ?: WordKnowledgeStatus.UNMARKED
