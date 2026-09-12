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
        "肉夹馍" to CuratedTranslation(
            french = "roujiamo ; petit pain chinois garni de viande",
            english = "roujiamo; Chinese flatbread filled with chopped meat",
        ),
        "肉夾饃" to CuratedTranslation(
            french = "roujiamo ; petit pain chinois garni de viande",
            english = "roujiamo; Chinese flatbread filled with chopped meat",
        ),
        "你是不是一个大笨蛋" to CuratedTranslation(
            french = "Tu es vraiment un gros imbécile ?",
            english = "Are you a complete idiot?",
        ),
        "你很像你哥哥" to CuratedTranslation(
            french = "Tu ressembles beaucoup à ton frère aîné.",
            english = "You look a lot like your older brother.",
        ),
        "我上次去市中心买了三个肉夹馍" to CuratedTranslation(
            french = "La dernière fois, je suis allé en centre-ville acheter trois roujiamos.",
            english = "Last time, I went downtown and bought three roujiamos.",
        ),
        "我喜欢喝奶茶" to CuratedTranslation(
            french = "J’aime boire du thé au lait.",
            english = "I like drinking milk tea.",
        ),
        "今天我们一起学习中文。认识一个新词时，轻触它即可查看释义。" to CuratedTranslation(
            french = "Aujourd’hui, nous apprenons le chinois ensemble. Quand vous rencontrez un nouveau mot, touchez-le pour en afficher le sens.",
            english = "Today, we’re learning Chinese together. When you encounter a new word, tap it to see its meaning.",
        ),
    ).mapKeys { (source, _) -> normalizeSource(source) }

    fun curated(source: String): CuratedTranslation? = curated[normalizeSource(source)]

    /** Keeps culturally specific food names intact while the local model translates the sentence. */
    fun prepareForMachineTranslation(source: String): String = source
        .replace("肉夹馍", " roujiamo ")
        .replace("肉夾饃", " roujiamo ")
        .replace(Regex("[ \\t]+"), " ")
        .trim()

    fun polishFrench(source: String, candidate: String): String {
        curated(source)?.let { return it.french }
        var result = clean(candidate)
        if (containsRoujiamo(source)) {
            result = result
                .replace(Regex("(?i)(?:pinces|sandwichs|pains|hamburgers) (?:à|de) (?:la )?viande"), "roujiamos")
                .replace(Regex("(?i)(?:pince|sandwich|pain|hamburger) (?:à|de) (?:la )?viande"), "roujiamo")
        }
        return result
    }

    fun polishEnglish(source: String, candidate: String): String {
        curated(source)?.let { return it.english }
        var result = clean(candidate)
        if (containsRoujiamo(source)) {
            result = result
                .replace(Regex("(?i)meat (?:clamps|clips|wedges|sandwiches|buns|burgers)"), "roujiamos")
                .replace(Regex("(?i)meat (?:clamp|clip|wedge|sandwich|bun|burger)"), "roujiamo")
        }
        return result
    }

    private fun containsRoujiamo(value: String): Boolean = normalizeSource(value).let {
        it.contains("肉夹馍") || it.contains("肉夾饃")
    }

    private fun normalizeSource(value: String): String = value
        .filterNot(Char::isWhitespace)
        .trimEnd('。', '！', '？', '!', '?')

    private fun clean(value: String): String = value
        .trim()
        .replace(Regex("[ \\t]+"), " ")
}
