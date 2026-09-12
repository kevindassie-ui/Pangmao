package fr.kairossolum.pangmao.domain.model

data class DictionaryEntry(
    val id: Long,
    val traditional: String,
    val simplified: String,
    val pinyin: String,
    val definitionsEnglish: List<String>,
    val definitionsFrench: List<String>,
    val sources: String,
    val frequency: Int,
) {
    val displayHeadword: String
        get() = simplified

    val alternateHeadword: String?
        get() = traditional.takeIf { it != simplified }
}

data class ExampleSentence(
    val id: Long,
    val chinese: String,
    val pinyin: String,
    val english: String,
    val french: String,
    val tatoebaChineseId: Long,
    val tatoebaEnglishId: Long,
    val tatoebaFrenchId: Long,
    val chineseSource: String,
    val englishSource: String,
    val frenchSource: String,
)

enum class RelatedWordPosition {
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
}

enum class RelatedWordSort {
    FREQUENCY,
    PINYIN,
}

data class CharacterInfo(
    val character: String,
    val codepoint: String,
    val mandarin: String,
    val definition: String,
    val radical: Int?,
    val additionalStrokes: Int?,
    val totalStrokes: String,
    val simplifiedVariants: String,
    val traditionalVariants: String,
)

data class TextToken(
    val text: String,
    val entryId: Long? = null,
    val isChinese: Boolean = false,
)

data class AnalyzedToken(
    val token: TextToken,
    val entry: DictionaryEntry?,
)

data class TextAnalysis(
    val sourceText: String,
    val exactEntry: DictionaryEntry?,
    val exactExample: ExampleSentence?,
    val tokens: List<AnalyzedToken>,
)
