package fr.kairossolum.pangmao.data.strokes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StrokeOrderCodecTest {
    @Test
    fun `plain payload preserves stroke order and decimal medians`() {
        val decoded = StrokeOrderCodec.decodeText(
            "猫",
            "M 0 0 L 10 10 Z\t0,0;10,10\nM 2 3 Q 4 5 6 7 Z\t2,3;4,5.5;6,7",
        )

        assertEquals("猫", decoded.character)
        assertEquals(2, decoded.strokes.size)
        assertEquals("M 2 3 Q 4 5 6 7 Z", decoded.strokes[1].pathData)
        assertEquals(5.5f, decoded.strokes[1].median[1].y)
    }

    @Test
    fun `malformed payload is rejected instead of partially displayed`() {
        assertThrows(IllegalArgumentException::class.java) {
            StrokeOrderCodec.decodeText("好", "M 0 0 L 1 1 Z\tbroken")
        }
    }
}
