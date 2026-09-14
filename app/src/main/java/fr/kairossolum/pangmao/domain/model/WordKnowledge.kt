package fr.kairossolum.pangmao.domain.model

enum class WordKnowledgeStatus {
    UNMARKED,
    LEARNING,
    KNOWN,
}

data class WordKnowledge(
    val entryId: Long,
    val status: WordKnowledgeStatus,
    val updatedAt: Long,
)
