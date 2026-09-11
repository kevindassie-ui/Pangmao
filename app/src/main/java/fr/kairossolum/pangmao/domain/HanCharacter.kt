package fr.kairossolum.pangmao.domain

fun firstHanCharacter(value: String): String? {
    val codePoints = value.codePoints().iterator()
    while (codePoints.hasNext()) {
        val codePoint = codePoints.nextInt()
        if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
            return String(Character.toChars(codePoint))
        }
    }
    return null
}
