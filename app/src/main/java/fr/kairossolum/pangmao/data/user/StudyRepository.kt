package fr.kairossolum.pangmao.data.user

import fr.kairossolum.pangmao.data.dictionary.DictionaryRepository
import fr.kairossolum.pangmao.domain.ReviewRating
import fr.kairossolum.pangmao.domain.ReviewScheduler
import fr.kairossolum.pangmao.domain.ReviewState
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.WordKnowledge
import fr.kairossolum.pangmao.domain.model.WordKnowledgeStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

data class StudyCard(
    val entry: DictionaryEntry,
    val scheduling: FlashcardEntity,
)

class StudyRepository(
    private val dao: UserDao,
    private val dictionary: DictionaryRepository,
) {
    fun favorites(): Flow<List<DictionaryEntry>> = dao.favoriteIds().mapLatest(dictionary::entries)

    fun history(): Flow<List<DictionaryEntry>> = dao.historyIds().mapLatest(dictionary::entries)

    fun queryHistory(): Flow<List<QueryHistoryEntity>> = dao.queryHistory()

    fun isFavorite(entryId: Long): Flow<Boolean> = dao.isFavorite(entryId)

    fun isFlashcard(entryId: Long): Flow<Boolean> = dao.isFlashcard(entryId)

    fun wordKnowledgeStatus(entryId: Long): Flow<WordKnowledgeStatus> =
        dao.wordKnowledge(entryId).map { value -> storedWordKnowledgeStatus(value?.status) }

    fun wordKnowledge(): Flow<List<WordKnowledge>> =
        dao.wordKnowledge().map { values -> values.mapNotNull(WordKnowledgeEntity::toModel) }

    suspend fun toggleFavorite(entryId: Long, currentlyFavorite: Boolean) {
        if (currentlyFavorite) dao.deleteFavorite(entryId) else dao.insertFavorite(FavoriteEntity(entryId))
    }

    suspend fun recordHistory(entryId: Long) = dao.recordHistory(entryId)

    suspend fun recordQuery(value: String) {
        val query = value.trim().replace(Regex("\\s+"), " ").take(MAX_QUERY_LENGTH)
        if (query.isNotBlank()) dao.recordQuery(query)
    }

    suspend fun deleteQuery(query: String) = dao.deleteQuery(query)

    suspend fun clearQueryHistory() = dao.clearQueryHistory()

    suspend fun addFlashcard(entryId: Long) {
        val today = LocalDate.now().toEpochDay()
        dao.insertFlashcard(FlashcardEntity(entryId = entryId, dueEpochDay = today))
    }

    suspend fun removeFlashcard(entryId: Long) = dao.deleteFlashcard(entryId)

    suspend fun setWordKnowledgeStatus(entryId: Long, status: WordKnowledgeStatus) {
        val value = status.toEntity(entryId)
        if (value == null) dao.deleteWordKnowledge(entryId) else dao.upsertWordKnowledge(value)
    }

    fun allCards(): Flow<List<StudyCard>> = dao.flashcards().mapLatest(::resolveCards)

    fun dueCards(today: Long = LocalDate.now().toEpochDay()): Flow<List<StudyCard>> =
        dao.dueFlashcards(today).mapLatest(::resolveCards)

    suspend fun review(entryId: Long, rating: ReviewRating) {
        val card = dao.flashcard(entryId) ?: return
        val next = ReviewScheduler.schedule(
            ReviewState(card.intervalDays, card.ease, card.repetitions, card.lapses),
            rating,
        )
        val today = LocalDate.now().toEpochDay()
        dao.upsertFlashcard(
            card.copy(
                dueEpochDay = today + next.intervalDays,
                intervalDays = next.intervalDays,
                ease = next.ease,
                repetitions = next.repetitions,
                lapses = next.lapses,
                lastReviewedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun resolveCards(cards: List<FlashcardEntity>): List<StudyCard> {
        val entries = dictionary.entries(cards.map(FlashcardEntity::entryId)).associateBy(DictionaryEntry::id)
        return cards.mapNotNull { card -> entries[card.entryId]?.let { StudyCard(it, card) } }
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 200
    }
}
