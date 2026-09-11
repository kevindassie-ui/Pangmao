package fr.kairossolum.pangmao.data.dictionary

import fr.kairossolum.pangmao.domain.LongestMatchTokenizer
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import fr.kairossolum.pangmao.domain.model.TextToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface DictionaryRepository {
    suspend fun search(query: String, limit: Int = 80): List<DictionaryEntry>
    suspend fun popular(limit: Int = 30): List<DictionaryEntry>
    suspend fun entry(identifier: Long): DictionaryEntry?
    suspend fun entries(identifiers: List<Long>): List<DictionaryEntry>
    suspend fun lookupExact(word: String): DictionaryEntry?
    suspend fun examples(headword: String, limit: Int = 6): List<ExampleSentence>
    suspend fun characters(text: String): List<CharacterInfo>
    suspend fun tokenize(text: String): List<TextToken>
    suspend fun metadata(): Map<String, String>
}

class OfflineDictionaryRepository(
    private val source: DictionaryDataSource,
) : DictionaryRepository {
    @Volatile
    private var tokenizer: LongestMatchTokenizer? = null

    override suspend fun search(query: String, limit: Int): List<DictionaryEntry> = io { source.search(query, limit) }
    override suspend fun popular(limit: Int): List<DictionaryEntry> = io { source.popular(limit) }
    override suspend fun entry(identifier: Long): DictionaryEntry? = io { source.entry(identifier) }
    override suspend fun entries(identifiers: List<Long>): List<DictionaryEntry> = io { source.entries(identifiers) }
    override suspend fun lookupExact(word: String): DictionaryEntry? = io { source.lookupExact(word) }
    override suspend fun examples(headword: String, limit: Int): List<ExampleSentence> = io { source.examples(headword, limit) }
    override suspend fun characters(text: String): List<CharacterInfo> = io { source.characters(text) }
    override suspend fun metadata(): Map<String, String> = io { source.metadata() }

    override suspend fun tokenize(text: String): List<TextToken> = io {
        var current = tokenizer
        if (current == null) {
            synchronized(this) {
                current = tokenizer
                if (current == null) {
                    current = LongestMatchTokenizer(source.headwords())
                    tokenizer = current
                }
            }
        }
        checkNotNull(current).tokenize(text)
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
