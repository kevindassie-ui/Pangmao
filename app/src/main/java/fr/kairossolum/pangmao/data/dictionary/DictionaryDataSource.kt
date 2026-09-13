package fr.kairossolum.pangmao.data.dictionary

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import fr.kairossolum.pangmao.domain.Pinyin
import fr.kairossolum.pangmao.domain.HeadwordMatch
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import fr.kairossolum.pangmao.domain.model.LearningDictionaryEntry
import fr.kairossolum.pangmao.domain.model.LearningDictionarySense
import fr.kairossolum.pangmao.domain.model.LearningLanguage
import fr.kairossolum.pangmao.domain.model.RelatedWordPosition
import fr.kairossolum.pangmao.domain.model.RelatedWordSort
import java.io.File

class DictionaryDataSource(private val context: Context) {
    private val database: SQLiteDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val directory = File(context.filesDir, "dictionary/v4").apply { mkdirs() }
        val target = File(directory, "pangmao.db")
        if (!target.exists() || target.length() < 1024L) {
            val temporary = File(directory, "pangmao.db.copying")
            context.assets.open("databases/pangmao.db").use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        }
        SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    fun search(rawQuery: String, limit: Int = 80): List<DictionaryEntry> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return popular(limit)
        val ranked = linkedMapOf<Long, DictionaryEntry>()
        val containsHan = query.codePoints().anyMatch(::isHanCodePoint)
        if (containsHan) {
            queryEntries(
                """
                SELECT * FROM entries
                WHERE simplified = ? OR traditional = ?
                ORDER BY frequency DESC, length(simplified), id
                LIMIT ?
                """.trimIndent(),
                arrayOf(query, query, limit.toString()),
            ).forEach { ranked[it.id] = it }
            if (ranked.size < limit) {
                val escaped = escapeLike(query) + "%"
                queryEntries(
                    """
                    SELECT * FROM entries
                    WHERE simplified LIKE ? ESCAPE '\' OR traditional LIKE ? ESCAPE '\'
                    ORDER BY CASE WHEN simplified = ? OR traditional = ? THEN 0 ELSE 1 END,
                        frequency DESC, length(simplified), id
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(escaped, escaped, query, query, limit.toString()),
                ).forEach { ranked.putIfAbsent(it.id, it) }
            }
        } else {
            val plain = Pinyin.plain(query)
            if (plain.isNotEmpty()) {
                queryEntries(
                    """
                    SELECT * FROM entries
                    WHERE pinyin_plain = ?
                    ORDER BY frequency DESC, length(simplified), id
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(plain, limit.toString()),
                ).forEach { ranked[it.id] = it }
                queryEntries(
                    """
                    SELECT * FROM entries
                    WHERE pinyin_plain LIKE ? ESCAPE '\'
                    ORDER BY CASE WHEN pinyin_plain = ? THEN 0 ELSE 1 END,
                        frequency DESC, length(simplified), id
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf("${escapeLike(plain)}%", plain, limit.toString()),
                ).forEach { ranked.putIfAbsent(it.id, it) }
            }
            if (ranked.size < limit) {
                val match = ftsQuery(query)
                if (match.isNotEmpty()) {
                    try {
                        queryEntries(
                            """
                            SELECT e.* FROM entries_fts f
                            JOIN entries e ON e.id = f.docid
                            WHERE entries_fts MATCH ?
                            ORDER BY e.frequency DESC, length(e.simplified), e.id
                            LIMIT ?
                            """.trimIndent(),
                            arrayOf(match, limit.toString()),
                        ).forEach { ranked.putIfAbsent(it.id, it) }
                    } catch (_: SQLiteException) {
                        // A malformed user FTS query must never crash dictionary search.
                    }
                }
            }
        }
        return ranked.values.take(limit)
    }

    fun searchLearning(
        language: LearningLanguage,
        rawQuery: String,
        limit: Int = 80,
    ): List<LearningDictionaryEntry> {
        val languageCode = LearningDictionaryQuery.languageCode(language)
        val query = LearningDictionaryQuery.normalize(rawQuery)
        if (query.isEmpty() || limit <= 0) return emptyList()
        val ranked = linkedSetOf<Long>()

        queryLearningEntryIds(
            """
            SELECT e.id FROM learning_forms f
            JOIN learning_entries e ON e.id = f.entry_id
            WHERE e.language = ? AND f.form_search = ?
            GROUP BY e.id
            ORDER BY length(e.primary_form), e.primary_form_search, e.id
            LIMIT ?
            """.trimIndent(),
            arrayOf(languageCode, query, limit.toString()),
        ).forEach(ranked::add)

        if (ranked.size < limit) {
            queryLearningEntryIds(
                """
                SELECT e.id FROM learning_forms f
                JOIN learning_entries e ON e.id = f.entry_id
                WHERE e.language = ? AND f.form_search LIKE ? ESCAPE '\'
                GROUP BY e.id
                ORDER BY CASE WHEN e.primary_form_search = ? THEN 0 ELSE 1 END,
                    length(e.primary_form), e.primary_form_search, e.id
                LIMIT ?
                """.trimIndent(),
                arrayOf(languageCode, "${escapeLike(query)}%", query, limit.toString()),
            ).forEach(ranked::add)
        }

        if (ranked.size < limit) {
            val match = LearningDictionaryQuery.fts(query)
            if (match.isNotEmpty()) {
                try {
                    queryLearningEntryIds(
                        """
                        SELECT e.id FROM learning_entries_fts f
                        JOIN learning_entries e ON e.id = f.docid
                        WHERE e.language = ? AND learning_entries_fts MATCH ?
                        ORDER BY length(e.primary_form), e.primary_form_search, e.id
                        LIMIT ?
                        """.trimIndent(),
                        arrayOf(languageCode, match, limit.toString()),
                    ).forEach(ranked::add)
                } catch (_: SQLiteException) {
                    // User input must not be able to crash full-text lookup.
                }
            }
        }
        return learningEntries(ranked.take(limit))
    }

    fun learningEntry(identifier: Long): LearningDictionaryEntry? =
        learningEntries(listOf(identifier)).firstOrNull()

    fun popular(limit: Int = 30): List<DictionaryEntry> = queryEntries(
        "SELECT * FROM entries WHERE length(simplified) BETWEEN 1 AND 4 AND frequency > 0 ORDER BY frequency DESC, id LIMIT ?",
        arrayOf(limit.toString()),
    )

    fun entry(identifier: Long): DictionaryEntry? = queryEntries(
        "SELECT * FROM entries WHERE id = ? LIMIT 1",
        arrayOf(identifier.toString()),
    ).firstOrNull()

    fun entries(identifiers: List<Long>): List<DictionaryEntry> {
        if (identifiers.isEmpty()) return emptyList()
        val placeholders = identifiers.joinToString(",") { "?" }
        val byId = queryEntries(
            "SELECT * FROM entries WHERE id IN ($placeholders)",
            identifiers.map(Long::toString).toTypedArray(),
        ).associateBy(DictionaryEntry::id)
        return identifiers.mapNotNull(byId::get)
    }

    fun lookupExact(word: String): DictionaryEntry? {
        val cursor = database.rawQuery(
            "SELECT preferred_entry_id FROM headwords WHERE word = ? LIMIT 1",
            arrayOf(word),
    )
        return cursor.use { if (it.moveToFirst()) entry(it.getLong(0)) else null }
    }

    fun examples(headword: String, limit: Int = 6): List<ExampleSentence> {
        val escaped = "%${escapeLike(headword)}%"
        return database.rawQuery(
            """
            SELECT id, tatoeba_chinese_id, chinese, pinyin, tatoeba_english_id, english,
                tatoeba_french_id, french, chinese_source, english_source, french_source
            FROM examples
            WHERE chinese LIKE ? ESCAPE '\'
            ORDER BY length(chinese), id
            LIMIT ?
            """.trimIndent(),
            arrayOf(escaped, limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ExampleSentence(
                            id = cursor.getLong(0),
                            tatoebaChineseId = cursor.getLong(1),
                            chinese = cursor.getString(2),
                            pinyin = cursor.getString(3),
                            tatoebaEnglishId = cursor.getLong(4),
                            english = cursor.getString(5),
                            tatoebaFrenchId = cursor.getLong(6),
                            french = cursor.getString(7),
                            chineseSource = cursor.getString(8),
                            englishSource = cursor.getString(9),
                            frenchSource = cursor.getString(10),
                        )
                    )
                }
            }
        }
    }

    fun exactExample(text: String): ExampleSentence? = database.rawQuery(
        """
        SELECT id, tatoeba_chinese_id, chinese, pinyin, tatoeba_english_id, english,
            tatoeba_french_id, french, chinese_source, english_source, french_source
        FROM examples
        WHERE chinese = ?
        ORDER BY id
        LIMIT 1
        """.trimIndent(),
        arrayOf(text),
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        ExampleSentence(
            id = cursor.getLong(0),
            tatoebaChineseId = cursor.getLong(1),
            chinese = cursor.getString(2),
            pinyin = cursor.getString(3),
            tatoebaEnglishId = cursor.getLong(4),
            english = cursor.getString(5),
            tatoebaFrenchId = cursor.getLong(6),
            french = cursor.getString(7),
            chineseSource = cursor.getString(8),
            englishSource = cursor.getString(9),
            frenchSource = cursor.getString(10),
        )
    }

    fun relatedWords(
        character: String,
        position: RelatedWordPosition,
        frequentOnly: Boolean,
        sort: RelatedWordSort,
        limit: Int,
    ): List<DictionaryEntry> {
        val escaped = escapeLike(character)
        val pattern = when (position) {
            RelatedWordPosition.CONTAINS -> "%$escaped%"
            RelatedWordPosition.STARTS_WITH -> "$escaped%"
            RelatedWordPosition.ENDS_WITH -> "%$escaped"
        }
        val frequencyClause = if (frequentOnly) "AND frequency > 0" else ""
        val orderClause = when (sort) {
            RelatedWordSort.FREQUENCY -> "frequency DESC, length(simplified), pinyin_plain, id"
            RelatedWordSort.PINYIN -> "pinyin_plain, frequency DESC, length(simplified), id"
        }
        return queryEntries(
            """
            SELECT * FROM entries
            WHERE length(simplified) > 1
                AND (simplified LIKE ? ESCAPE '\' OR traditional LIKE ? ESCAPE '\')
                $frequencyClause
            ORDER BY $orderClause
            LIMIT ?
            """.trimIndent(),
            arrayOf(pattern, pattern, limit.toString()),
        )
    }

    fun characters(text: String): List<CharacterInfo> {
        val characters = text.codePoints().toArray().map { String(Character.toChars(it)) }.distinct()
        if (characters.isEmpty()) return emptyList()
        val placeholders = characters.joinToString(",") { "?" }
        val result = database.rawQuery(
            "SELECT * FROM characters WHERE character IN ($placeholders)",
            characters.toTypedArray(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CharacterInfo(
                            character = cursor.getString(cursor.getColumnIndexOrThrow("character")),
                            codepoint = cursor.getString(cursor.getColumnIndexOrThrow("codepoint")),
                            mandarin = cursor.getString(cursor.getColumnIndexOrThrow("mandarin")),
                            definition = cursor.getString(cursor.getColumnIndexOrThrow("definition")),
                            radical = cursor.nullableInt("radical"),
                            additionalStrokes = cursor.nullableInt("additional_strokes"),
                            totalStrokes = cursor.getString(cursor.getColumnIndexOrThrow("total_strokes")),
                            simplifiedVariants = cursor.getString(cursor.getColumnIndexOrThrow("simplified_variants")),
                            traditionalVariants = cursor.getString(cursor.getColumnIndexOrThrow("traditional_variants")),
                        )
                    )
                }
            }
        }.associateBy(CharacterInfo::character)
        return characters.mapNotNull(result::get)
    }

    fun headwords(): Map<String, HeadwordMatch> = database.rawQuery(
        """
        SELECT h.word, h.preferred_entry_id, e.frequency
        FROM headwords h
        JOIN entries e ON e.id = h.preferred_entry_id
        WHERE h.length <= 8
        """.trimIndent(),
        null,
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                put(cursor.getString(0), HeadwordMatch(cursor.getLong(1), cursor.getInt(2)))
            }
        }
    }

    fun metadata(): Map<String, String> = database.rawQuery("SELECT key, value FROM metadata", null).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
        }
    }

    private fun queryEntries(sql: String, arguments: Array<String>): List<DictionaryEntry> =
        database.rawQuery(sql, arguments).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toEntry())
            }
        }

    private fun Cursor.toEntry(): DictionaryEntry = DictionaryEntry(
        id = getLong(getColumnIndexOrThrow("id")),
        traditional = getString(getColumnIndexOrThrow("traditional")),
        simplified = getString(getColumnIndexOrThrow("simplified")),
        pinyin = getString(getColumnIndexOrThrow("pinyin")),
        definitionsEnglish = getString(getColumnIndexOrThrow("definitions_en")).lines().filter(String::isNotBlank),
        definitionsFrench = getString(getColumnIndexOrThrow("definitions_fr")).lines().filter(String::isNotBlank),
        sources = getString(getColumnIndexOrThrow("sources")),
        frequency = getInt(getColumnIndexOrThrow("frequency")),
        )

    private fun queryLearningEntryIds(sql: String, arguments: Array<String>): List<Long> =
        database.rawQuery(sql, arguments).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getLong(0))
            }
        }

    private fun learningEntries(identifiers: List<Long>): List<LearningDictionaryEntry> {
        if (identifiers.isEmpty()) return emptyList()
        val placeholders = identifiers.joinToString(",") { "?" }
        val arguments = identifiers.map(Long::toString).toTypedArray()
        val entries = linkedMapOf<Long, MutableLearningEntry>()
        database.rawQuery(
            """
            SELECT id, language, primary_form, parts_of_speech, genders,
                source_code, source_entry_index
            FROM learning_entries WHERE id IN ($placeholders)
            """.trimIndent(),
            arguments,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val identifier = cursor.getLong(0)
                entries[identifier] = MutableLearningEntry(
                    id = identifier,
                    language = cursor.getString(1).toLearningLanguage(),
                    primaryForm = cursor.getString(2),
                    partsOfSpeech = cursor.getString(3).nonBlankLines(),
                    genders = cursor.getString(4).nonBlankLines(),
                    sourceCode = cursor.getString(5),
                    sourceEntryIndex = cursor.getLong(6),
                )
            }
        }
        if (entries.isEmpty()) return emptyList()

        database.rawQuery(
            """
            SELECT entry_id, form FROM learning_forms
            WHERE entry_id IN ($placeholders) ORDER BY entry_id, position
            """.trimIndent(),
            arguments,
        ).use { cursor ->
            while (cursor.moveToNext()) entries[cursor.getLong(0)]?.forms?.add(cursor.getString(1))
        }
        database.rawQuery(
            """
            SELECT entry_id, pronunciation FROM learning_pronunciations
            WHERE entry_id IN ($placeholders) ORDER BY entry_id, position
            """.trimIndent(),
            arguments,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                entries[cursor.getLong(0)]?.pronunciations?.add(cursor.getString(1))
            }
        }

        val senses = linkedMapOf<Long, MutableLearningSense>()
        database.rawQuery(
            """
            SELECT id, entry_id, definitions, source_code FROM learning_senses
            WHERE entry_id IN ($placeholders) ORDER BY entry_id, position
            """.trimIndent(),
            arguments,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val identifier = cursor.getLong(0)
                val entryIdentifier = cursor.getLong(1)
                val sense = MutableLearningSense(
                    id = identifier,
                    definitions = cursor.getString(2).nonBlankLines(),
                    sourceCode = cursor.getString(3),
                )
                senses[identifier] = sense
                entries[entryIdentifier]?.senses?.add(sense)
            }
        }
        if (senses.isNotEmpty()) {
            val sensePlaceholders = senses.keys.joinToString(",") { "?" }
            database.rawQuery(
                """
                SELECT sense_id, chinese FROM learning_equivalents
                WHERE sense_id IN ($sensePlaceholders) ORDER BY sense_id, position
                """.trimIndent(),
                senses.keys.map(Long::toString).toTypedArray(),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    senses[cursor.getLong(0)]?.chineseEquivalents?.add(cursor.getString(1))
                }
            }
        }
        return identifiers.mapNotNull { entries[it]?.toModel() }
    }

    private fun Cursor.nullableInt(column: String): Int? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getInt(index)
    }

    private fun escapeLike(value: String): String = value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    private fun ftsQuery(value: String): String = Regex("[\\p{L}\\p{N}]+").findAll(value)
        .map { "\"${it.value.replace("\"", "\"\"")}\"*" }
        .joinToString(" ")

    private fun isHanCodePoint(codepoint: Int): Boolean =
        codepoint in 0x3400..0x4DBF ||
            codepoint in 0x4E00..0x9FFF ||
            codepoint in 0xF900..0xFAFF ||
            codepoint in 0x20000..0x323AF

    private fun String.toLearningLanguage(): LearningLanguage = when (this) {
        "fr" -> LearningLanguage.FRENCH
        "en" -> LearningLanguage.ENGLISH
        else -> error("Unsupported learning dictionary language: $this")
    }

    private fun String.nonBlankLines(): List<String> = lines().filter(String::isNotBlank)
}

private data class MutableLearningEntry(
    val id: Long,
    val language: LearningLanguage,
    val primaryForm: String,
    val partsOfSpeech: List<String>,
    val genders: List<String>,
    val sourceCode: String,
    val sourceEntryIndex: Long,
    val forms: MutableList<String> = mutableListOf(),
    val pronunciations: MutableList<String> = mutableListOf(),
    val senses: MutableList<MutableLearningSense> = mutableListOf(),
) {
    fun toModel(): LearningDictionaryEntry = LearningDictionaryEntry(
        id = id,
        language = language,
        forms = forms.ifEmpty { listOf(primaryForm) },
        pronunciations = pronunciations,
        partsOfSpeech = partsOfSpeech,
        genders = genders,
        senses = senses.map(MutableLearningSense::toModel),
        sourceCode = sourceCode,
        sourceEntryIndex = sourceEntryIndex,
    )
}

private data class MutableLearningSense(
    val id: Long,
    val definitions: List<String>,
    val sourceCode: String,
    val chineseEquivalents: MutableList<String> = mutableListOf(),
) {
    fun toModel(): LearningDictionarySense = LearningDictionarySense(
        id = id,
        definitions = definitions,
        chineseEquivalents = chineseEquivalents,
        sourceCode = sourceCode,
    )
}
