package fr.kairossolum.pangmao.domain

data class CuratedTranslation(
    val french: String,
    val english: String,
)

/** Small, reviewable corrections for expressions where generic offline translation is misleading. */
object TranslationQuality {
    private val curated = mapOf(
        "傻比" to CuratedTranslation(
            french = "idiot ; imbécile (vulgaire)",
            english = "idiot; dumbass (vulgar)",
        ),
        "傻逼" to CuratedTranslation(
            french = "idiot ; imbécile (vulgaire)",
            english = "idiot; dumbass (vulgar)",
        ),
        "煞笔" to CuratedTranslation(
            french = "idiot ; imbécile (vulgaire)",
            english = "idiot; dumbass (vulgar)",
        ),
        "好喜欢" to CuratedTranslation(
            french = "aimer beaucoup ; adorer",
            english = "really like; be very fond of",
        ),
        "大笨蛋" to CuratedTranslation(
            french = "gros imbécile",
            english = "big idiot",
        ),
        "今天我们一起学习中文。认识一个新词时，轻触它即可查看释义。" to CuratedTranslation(
            french = "Aujourd’hui, nous apprenons le chinois ensemble. Quand vous rencontrez un nouveau mot, touchez-le pour en afficher le sens.",
            english = "Today, we’re learning Chinese together. When you encounter a new word, tap it to see its meaning.",
        ),
    )

    fun curated(source: String): CuratedTranslation? = curated[normalizeSource(source)]

    fun polishFrench(source: String, candidate: String): String =
        curated(source)?.french ?: clean(candidate)

    fun polishEnglish(source: String, candidate: String): String =
        curated(source)?.english ?: clean(candidate)

    private fun normalizeSource(value: String): String = value.filterNot(Char::isWhitespace)

    private fun clean(value: String): String = value
        .trim()
        .replace(Regex("[ \\t]+"), " ")
}
