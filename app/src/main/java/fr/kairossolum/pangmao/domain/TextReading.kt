package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.TextAnalysis

/** Produces one continuous numbered-pinyin line while retaining source punctuation. */
fun TextAnalysis.numberedPinyinReading(): String = tokens
    .map { analyzed ->
        analyzed.entry?.pinyin
            ?.takeIf { analyzed.token.isChinese && it.isNotBlank() }
            ?: analyzed.token.text
    }
    .joinToString(" ")
    .replace(Regex("[ \\t]+([，。！？；：、,.!?;:])"), "$1")
    .replace(Regex("([（(])\\s+"), "$1")
    .replace(Regex("\\s+([）)])"), "$1")
    .replace(Regex(" *\\n *"), "\n")
    .trim()
