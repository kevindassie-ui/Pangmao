package fr.kairossolum.pangmao.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.kairossolum.pangmao.data.user.StudyCard
import fr.kairossolum.pangmao.data.user.StudyRepository
import fr.kairossolum.pangmao.domain.ReviewRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewSession(
    val cards: List<StudyCard> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val completedCount: Int = 0,
) {
    val current: StudyCard? get() = cards.getOrNull(index)
    val isActive: Boolean get() = current != null
}

class StudyViewModel(private val study: StudyRepository) : ViewModel() {
    val dueCards = study.dueCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allCards = study.allCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favorites = study.favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _session = MutableStateFlow(ReviewSession())
    val session: StateFlow<ReviewSession> = _session.asStateFlow()

    fun startReview() {
        _session.value = ReviewSession(cards = dueCards.value)
    }

    fun reveal() {
        _session.value = _session.value.copy(revealed = true)
    }

    fun rate(rating: ReviewRating) {
        val current = _session.value.current ?: return
        viewModelScope.launch {
            study.review(current.entry.id, rating)
            val state = _session.value
            _session.value = state.copy(
                index = state.index + 1,
                revealed = false,
                completedCount = state.completedCount + 1,
            )
        }
    }

    fun closeReview() {
        _session.value = ReviewSession()
    }

    fun remove(entryId: Long) {
        viewModelScope.launch { study.removeFlashcard(entryId) }
    }
}

