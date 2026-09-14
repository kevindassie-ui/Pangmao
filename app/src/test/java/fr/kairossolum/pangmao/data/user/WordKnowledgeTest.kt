package fr.kairossolum.pangmao.data.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordKnowledgeTest {
    @Test
    fun `stored values restore a safe explicit status`() {
        assertEquals(WordKnowledgeStatus.LEARNING, WordKnowledgeStatus.fromStored("LEARNING"))
        assertEquals(WordKnowledgeStatus.KNOWN, WordKnowledgeStatus.fromStored("KNOWN"))
        assertEquals(WordKnowledgeStatus.UNMARKED, WordKnowledgeStatus.fromStored(null))
        assertEquals(WordKnowledgeStatus.UNMARKED, WordKnowledgeStatus.fromStored("OBSOLETE"))
    }

    @Test
    fun `unmarked words are absent while learning and known words are stored`() {
        assertNull(WordKnowledgeStatus.UNMARKED.toEntity(entryId = 42L, updatedAt = 10L))
        assertEquals(
            WordKnowledgeEntity(entryId = 42L, status = "LEARNING", updatedAt = 10L),
            WordKnowledgeStatus.LEARNING.toEntity(entryId = 42L, updatedAt = 10L),
        )
        assertEquals(
            WordKnowledgeEntity(entryId = 42L, status = "KNOWN", updatedAt = 11L),
            WordKnowledgeStatus.KNOWN.toEntity(entryId = 42L, updatedAt = 11L),
        )
    }

    @Test
    fun `invalid stored status is ignored instead of leaking into the domain`() {
        assertNull(WordKnowledgeEntity(7L, "OBSOLETE", 12L).toModel())
        assertEquals(
            WordKnowledge(7L, WordKnowledgeStatus.KNOWN, 12L),
            WordKnowledgeEntity(7L, "KNOWN", 12L).toModel(),
        )
    }
}
