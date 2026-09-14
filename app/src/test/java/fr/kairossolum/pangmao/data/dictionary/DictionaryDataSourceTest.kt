package fr.kairossolum.pangmao.data.dictionary

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryDataSourceTest {
    @Test
    fun `large entry lookups use safe distinct SQLite batches`() {
        val identifiers = (1L..1_205L).toList() + listOf(1L, 600L, 1_205L)

        val batches = entryIdentifierBatches(identifiers)

        assertEquals(listOf(500, 500, 205), batches.map { it.size })
        assertEquals(1_205, batches.flatten().distinct().size)
        assertEquals(1L, batches.first().first())
        assertEquals(1_205L, batches.last().last())
    }
}
