package fr.kairossolum.pangmao.domain.model

enum class LearningLanguage {
    CHINESE,
    FRENCH,
    ENGLISH,
}

enum class WritingSystem {
    HANZI,
    LATIN,
}

enum class PronunciationSystem {
    PINYIN,
    IPA,
}

enum class MeaningPolicy {
    DEFINITION_LANGUAGE_SETTING,
    CHINESE,
}

enum class LexicalFeature {
    TONE_COLORS,
    CHARACTER_BREAKDOWN,
    GRAMMATICAL_GENDER,
    PLURAL,
    CONJUGATION,
    INFLECTIONS,
    COLLOCATIONS,
}

data class LearningProfilePresentation(
    val language: LearningLanguage,
    val writingSystem: WritingSystem,
    val pronunciationSystem: PronunciationSystem,
    val meaningPolicy: MeaningPolicy,
    val lexicalFeatures: Set<LexicalFeature>,
)

val LearningLanguage.presentation: LearningProfilePresentation
    get() = when (this) {
        LearningLanguage.CHINESE -> LearningProfilePresentation(
            language = this,
            writingSystem = WritingSystem.HANZI,
            pronunciationSystem = PronunciationSystem.PINYIN,
            meaningPolicy = MeaningPolicy.DEFINITION_LANGUAGE_SETTING,
            lexicalFeatures = setOf(
                LexicalFeature.TONE_COLORS,
                LexicalFeature.CHARACTER_BREAKDOWN,
            ),
        )
        LearningLanguage.FRENCH -> LearningProfilePresentation(
            language = this,
            writingSystem = WritingSystem.LATIN,
            pronunciationSystem = PronunciationSystem.IPA,
            meaningPolicy = MeaningPolicy.CHINESE,
            lexicalFeatures = setOf(
                LexicalFeature.GRAMMATICAL_GENDER,
                LexicalFeature.PLURAL,
                LexicalFeature.CONJUGATION,
            ),
        )
        LearningLanguage.ENGLISH -> LearningProfilePresentation(
            language = this,
            writingSystem = WritingSystem.LATIN,
            pronunciationSystem = PronunciationSystem.IPA,
            meaningPolicy = MeaningPolicy.CHINESE,
            lexicalFeatures = setOf(
                LexicalFeature.INFLECTIONS,
                LexicalFeature.COLLOCATIONS,
            ),
        )
    }
