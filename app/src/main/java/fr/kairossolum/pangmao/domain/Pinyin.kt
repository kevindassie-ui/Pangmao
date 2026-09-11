package fr.kairossolum.pangmao.domain

import java.text.Normalizer

object Pinyin {
    private val toneMarks = mapOf(
        'a' to "āáǎàa",
        'e' to "ēéěèe",
        'i' to "īíǐìi",
        'o' to "ōóǒòo",
        'u' to "ūúǔùu",
        'ü' to "ǖǘǚǜü",
        'A' to "ĀÁǍÀA",
        'E' to "ĒÉĚÈE",
        'I' to "ĪÍǏÌI",
        'O' to "ŌÓǑÒO",
        'U' to "ŪÚǓÙU",
        'Ü' to "ǕǗǙǛÜ",
    )

    fun withToneMarks(numbered: String): String = numbered
        .replace("u:", "ü", ignoreCase = true)
        .split(Regex("(?<=\\s)|(?=\\s)"))
        .joinToString("") { part ->
            if (part.isBlank()) part else markSyllable(part)
        }

    fun plain(value: String): String {
        val prepared = value.replace("u:", "v", ignoreCase = true).replace('ü', 'v').replace('Ü', 'v')
        return Normalizer.normalize(prepared, Normalizer.Form.NFD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
            .filter { it.isLetter() }
            .lowercase()
    }

    fun toneOf(syllable: String): Int {
        syllable.lastOrNull { it in '1'..'5' }?.let { return it.digitToInt() }
        for ((base, marks) in toneMarks) {
            val index = syllable.indexOfFirst { it in marks }
            if (index >= 0) {
                val tone = marks.indexOf(syllable[index]) + 1
                if (tone in 1..4) return tone
            }
            if (base in syllable) continue
        }
        return 5
    }

    private fun markSyllable(raw: String): String {
        val match = Regex("^(.*?)([1-5])([^A-Za-züÜ:]*)$").matchEntire(raw) ?: return raw
        val body = match.groupValues[1].replace("u:", "ü", ignoreCase = true)
        val tone = match.groupValues[2].toInt()
        val suffix = match.groupValues[3]
        if (tone == 5) return body + suffix

        val lower = body.lowercase()
        val vowelIndex = when {
            'a' in lower -> lower.indexOf('a')
            'e' in lower -> lower.indexOf('e')
            "ou" in lower -> lower.indexOf('o')
            else -> body.indexOfLast { it.lowercaseChar() in "aeiouü" }
        }
        if (vowelIndex < 0) return body + suffix
        val vowel = body[vowelIndex]
        val marked = toneMarks[vowel]?.get(tone - 1) ?: return body + suffix
        return body.replaceRange(vowelIndex, vowelIndex + 1, marked.toString()) + suffix
    }
}

