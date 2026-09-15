package fr.kairossolum.pangmao.domain.model

data class StrokePoint(
    val x: Float,
    val y: Float,
)

data class CharacterStroke(
    val pathData: String,
    val median: List<StrokePoint>,
)

data class StrokeOrder(
    val character: String,
    val strokes: List<CharacterStroke>,
)

data class PracticePoint(
    val x: Float,
    val y: Float,
)
