package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.CharacterStroke
import fr.kairossolum.pangmao.domain.model.PracticePoint
import fr.kairossolum.pangmao.domain.model.StrokeOrder
import fr.kairossolum.pangmao.domain.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class StrokeEvaluatorTest {
    private val order = StrokeOrder(
        character = "十",
        strokes = listOf(
            stroke(canvasPoints(0.15f to 0.35f, 0.85f to 0.35f)),
            stroke(canvasPoints(0.50f to 0.10f, 0.50f to 0.90f)),
        ),
    )

    @Test
    fun `a tolerant trace in the expected direction advances`() {
        val trace = listOf(
            PracticePoint(0.14f, 0.36f),
            PracticePoint(0.48f, 0.33f),
            PracticePoint(0.86f, 0.36f),
        )

        assertEquals(StrokeEvaluationKind.MATCH, StrokeEvaluator.evaluate(trace, order, 0).kind)
    }

    @Test
    fun `the right shape in reverse reports its direction`() {
        val trace = listOf(PracticePoint(0.85f, 0.35f), PracticePoint(0.15f, 0.35f))

        assertEquals(
            StrokeEvaluationKind.WRONG_DIRECTION,
            StrokeEvaluator.evaluate(trace, order, 0).kind,
        )
    }

    @Test
    fun `drawing a later stroke does not advance the order`() {
        val trace = listOf(PracticePoint(0.50f, 0.10f), PracticePoint(0.50f, 0.90f))
        val result = StrokeEvaluator.evaluate(trace, order, 0)

        assertEquals(StrokeEvaluationKind.OUT_OF_ORDER, result.kind)
        assertEquals(1, result.closestIndex)
    }

    @Test
    fun `a distant scribble asks for another attempt`() {
        val trace = listOf(PracticePoint(0.05f, 0.95f), PracticePoint(0.20f, 0.80f))

        assertEquals(
            StrokeEvaluationKind.TRY_AGAIN,
            StrokeEvaluator.evaluate(trace, order, 0).kind,
        )
    }

    private fun stroke(points: List<StrokePoint>) = CharacterStroke("M 0 0", points)

    private fun canvasPoints(vararg points: Pair<Float, Float>): List<StrokePoint> = points.map {
        StrokePoint(x = it.first * 1024f, y = 900f - it.second * 1024f)
    }
}
