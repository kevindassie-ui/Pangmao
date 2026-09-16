package fr.kairossolum.pangmao.data.strokes

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import fr.kairossolum.pangmao.domain.model.StrokeOrder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StrokeOrderRepository {
    suspend fun get(character: String): StrokeOrder?
    suspend fun metadata(): Map<String, String>
}

class OfflineStrokeOrderRepository(context: Context) : StrokeOrderRepository {
    private val database: SQLiteDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val directory = File(context.filesDir, "strokes/v1").apply { mkdirs() }
        val target = File(directory, "strokes.db")
        if (!target.exists() || target.length() < 1024L) {
            val temporary = File(directory, "strokes.db.copying")
            context.assets.open("databases/strokes.db").use { input ->
                temporary.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        }
        SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override suspend fun get(character: String): StrokeOrder? = withContext(Dispatchers.IO) {
        if (character.codePointCount(0, character.length) != 1) return@withContext null
        database.rawQuery(
            "SELECT stroke_count, payload FROM stroke_orders WHERE character = ? LIMIT 1",
            arrayOf(character),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val expectedCount = cursor.getInt(0)
            val decoded = StrokeOrderCodec.decode(character, cursor.getBlob(1))
            check(decoded.strokes.size == expectedCount) {
                "Stroke count mismatch for $character"
            }
            decoded
        }
    }

    override suspend fun metadata(): Map<String, String> = withContext(Dispatchers.IO) {
        database.rawQuery("SELECT key, value FROM metadata", null).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
            }
        }
    }
}
