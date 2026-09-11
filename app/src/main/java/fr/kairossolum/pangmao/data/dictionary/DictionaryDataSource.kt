package fr.kairossolum.pangmao.data.dictionary

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import fr.kairossolum.pangmao.domain.Pinyin
import fr.kairossolum.pangmao.domain.model.CharacterInfo
import fr.kairossolum.pangmao.domain.model.DictionaryEntry
import fr.kairossolum.pangmao.domain.model.ExampleSentence
import java.io.File

class DictionaryDataSource(private val context: Context) {
    private val database: SQLiteDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val directory = File(context.filesDir, "dictionary/v1").apply { mkdirs() }
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
                    WHERE simplified LIKE ? ESCAPE '\\' OR traditional LIKE ? ESCAPE '\\'
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
                    WHERE pinyin_plain LIKE ?
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
            SELECT id, tatoeba_chinese_id, chinese, pinyin, tatoeba_english_id, english
            FROM examples
            WHERE chinese LIKE ? ESCAPE '\\'
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
                        )
                    )
                }
            }
        }
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

    fun headwords(): Map<String, Long> = database.rawQuery(
        "SELECT word, preferred_entry_id FROM headwords WHERE length <= 8",
        null,
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) put(cursor.getString(0), cursor.getLong(1))
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
}

