package fr.kairossolum.pangmao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HanCharacterTest {
    @Test fun extractsFirstHanCharacter() {
        assertEquals("汉", firstHanCharacter("  汉字"))
    }

    @Test fun preservesSupplementaryHanCodePoint() {
        assertEquals("𠀀", firstHanCharacter("A𠀀B"))
    }

    @Test fun rejectsNonHanCandidate() {
        assertNull(firstHanCharacter("ABC 123"))
    }

    @Test fun detectsHanAnywhereInText() {
        assertTrue(containsHan("hello 大笨蛋!"))
        assertFalse(containsHan("hello 123"))
    }
}
