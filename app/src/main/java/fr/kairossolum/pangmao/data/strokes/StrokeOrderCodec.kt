package fr.kairossolum.pangmao.data.strokes

import fr.kairossolum.pangmao.domain.model.CharacterStroke
import fr.kairossolum.pangmao.domain.model.StrokeOrder
import fr.kairossolum.pangmao.domain.model.StrokePoint
import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream

object StrokeOrderCodec {
    fun decode(character: String, compressedPayload: ByteArray): StrokeOrder =
        InflaterInputStream(ByteArrayInputStream(compressedPayload))
            .bufferedReader(Charsets.UTF_8)
            .use { reader -> decodeText(character, reader.readText()) }

    internal fun decodeText(character: String, payload: String): StrokeOrder {
        require(character.codePointCount(0, character.length) == 1) {
            "Stroke order requires exactly one character"
        }
        val strokes = payload.lineSequence()
            .filter(String::isNotBlank)
            .mapIndexed { index, line ->
                val separator = line.indexOf('\t')
                require(separator > 0 && separator < line.lastIndex) {
                    "Malformed stroke ${index + 1} for $character"
                }
                val path = line.substring(0, separator)
                val points = line.substring(separator + 1)
                    .split(';')
                    .map { encoded ->
                        val comma = encoded.indexOf(',')
                        require(comma > 0 && comma < encoded.lastIndex) {
                            "Malformed median point for $character"
                        }
                        StrokePoint(
                            x = encoded.substring(0, comma).toFloat(),
                            y = encoded.substring(comma + 1).toFloat(),
                        )
                    }
                require(points.size >= 2) { "Missing stroke median for $character" }
                CharacterStroke(pathData = path, median = points)
            }
            .toList()
        require(strokes.isNotEmpty()) { "Empty stroke order for $character" }
        return StrokeOrder(character, strokes)
    }
}
