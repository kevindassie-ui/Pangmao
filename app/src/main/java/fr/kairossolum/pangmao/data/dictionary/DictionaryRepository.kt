package fr.kairossolum.pangmao.data.dictionary

import fr.kairossolum.pangmao.domain.DictionarySegmenter
import fr.kairossolum.pangmao.domain.model.AnalyzedToken
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import fr.kairossolum.pangmao.domain.model.TextAnalysis
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
    suspend fun analyze(text: String): TextAnalysis
    suspend fun metadata(): Map<String, String>
}

class OfflineDictionaryRepository(
    private val source: DictionaryDataSource,
) : DictionaryRepository {
    @Volatile
    private var cachedSegmenter: DictionarySegmenter? = null

    override suspend fun search(query: String, limit: Int): List<DictionaryEntry> = io { source.search(query, limit) }
    override suspend fun popular(limit: Int): List<DictionaryEntry> = io { source.popular(limit) }
    override suspend fun entry(identifier: Long): DictionaryEntry? = io { source.entry(identifier) }
    override suspend fun entries(identifiers: List<Long>): List<DictionaryEntry> = io { source.entries(identifiers) }
    override suspend fun lookupExact(word: String): DictionaryEntry? = io { source.lookupExact(word) }
    override suspend fun examples(headword: String, limit: Int): List<ExampleSentence> = io { source.examples(headword, limit) }
    override suspend fun characters(text: String): List<CharacterInfo> = io { source.characters(text) }
    override suspend fun metadata(): Map<String, String> = io { source.metadata() }

    override suspend fun tokenize(text: String): List<TextToken> = io {
        segmenter().tokenize(text)
    }

    override suspend fun analyze(text: String): TextAnalysis = io {
        val normalized = text.trim()
        val tokens = segmenter().tokenize(normalized)
        val entries = source.entries(tokens.mapNotNull(TextToken::entryId).distinct())
            .associateBy(DictionaryEntry::id)
        TextAnalysis(
            sourceText = normalized,
            exactEntry = normalized.takeIf(String::isNotEmpty)?.let(source::lookupExact),
            exactExample = normalized.takeIf(String::isNotEmpty)?.let(source::exactExample),
            tokens = tokens.map { token -> AnalyzedToken(token, token.entryId?.let(entries::get)) },
        )
    }

    private fun segmenter(): DictionarySegmenter {
        cachedSegmenter?.let { return it }
        return synchronized(this) {
            cachedSegmenter ?: DictionarySegmenter(source.headwords()).also { cachedSegmenter = it }
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
