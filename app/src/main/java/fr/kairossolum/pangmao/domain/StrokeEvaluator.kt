package fr.kairossolum.pangmao.domain

import fr.kairossolum.pangmao.domain.model.PracticePoint
import fr.kairossolum.pangmao.domain.model.StrokeOrder
import fr.kairossolum.pangmao.domain.model.StrokePoint
import kotlin.math.hypot

enum class StrokeEvaluationKind {
    MATCH,
    WRONG_DIRECTION,
    OUT_OF_ORDER,
    TRY_AGAIN,
}

data class StrokeEvaluation(
    val kind: StrokeEvaluationKind,
    val expectedIndex: Int,
    val closestIndex: Int? = null,
)

object StrokeEvaluator {
    private const val SAMPLE_COUNT = 24
    private const val MIN_TRACE_LENGTH = 0.025f
    private const val MAX_MEAN_DISTANCE = 0.12f
    private const val MAX_ENDPOINT_DISTANCE = 0.20f
    private const val MIN_LENGTH_RATIO = 0.32f
    private const val MAX_LENGTH_RATIO = 2.8f

    fun evaluate(
        trace: List<PracticePoint>,
        strokeOrder: StrokeOrder,
        expectedIndex: Int,
    ): StrokeEvaluation {
        if (expectedIndex !in strokeOrder.strokes.indices) {
            return StrokeEvaluation(StrokeEvaluationKind.TRY_AGAIN, expectedIndex)
        }
        val user = trace
            .map { PracticePoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
            .withoutNearDuplicates()
        if (polylineLength(user) < MIN_TRACE_LENGTH) {
            return StrokeEvaluation(StrokeEvaluationKind.TRY_AGAIN, expectedIndex)
        }

        val expected = strokeOrder.strokes[expectedIndex].median.map { it.toPracticePoint() }
        val forward = compare(user, expected)
        if (forward.matches) {
            return StrokeEvaluation(StrokeEvaluationKind.MATCH, expectedIndex, expectedIndex)
        }

        val reverse = compare(user, expected.asReversed())
        if (reverse.matches && reverse.meanDistance + 0.015f < forward.meanDistance) {
            return StrokeEvaluation(
                StrokeEvaluationKind.WRONG_DIRECTION,
                expectedIndex,
                expectedIndex,
            )
        }

        val closestOther = strokeOrder.strokes.indices
            .asSequence()
            .filter { it != expectedIndex }
            .map { index ->
                index to compare(
                    user,
                    strokeOrder.strokes[index].median.map { it.toPracticePoint() },
                )
            }
            .filter { (_, comparison) -> comparison.matches }
            .minByOrNull { (_, comparison) -> comparison.meanDistance }
        if (
            closestOther != null &&
            closestOther.second.meanDistance + 0.025f < forward.meanDistance
        ) {
            return StrokeEvaluation(
                StrokeEvaluationKind.OUT_OF_ORDER,
                expectedIndex,
                closestOther.first,
            )
        }
        return StrokeEvaluation(StrokeEvaluationKind.TRY_AGAIN, expectedIndex)
    }

    private data class Comparison(
        val meanDistance: Float,
        val startDistance: Float,
        val endDistance: Float,
        val lengthRatio: Float,
    ) {
        val matches: Boolean
            get() = meanDistance <= MAX_MEAN_DISTANCE &&
                startDistance <= MAX_ENDPOINT_DISTANCE &&
                endDistance <= MAX_ENDPOINT_DISTANCE &&
                lengthRatio in MIN_LENGTH_RATIO..MAX_LENGTH_RATIO
    }

    private fun compare(user: List<PracticePoint>, expected: List<PracticePoint>): Comparison {
        val userLength = polylineLength(user)
        val expectedLength = polylineLength(expected).coerceAtLeast(0.0001f)
        val sampledUser = resample(user, SAMPLE_COUNT)
        val sampledExpected = resample(expected, SAMPLE_COUNT)
        val mean = sampledUser.zip(sampledExpected)
            .sumOf { (left, right) -> distance(left, right).toDouble() }
            .div(SAMPLE_COUNT)
            .toFloat()
        return Comparison(
            meanDistance = mean,
            startDistance = distance(user.first(), expected.first()),
            endDistance = distance(user.last(), expected.last()),
            lengthRatio = userLength / expectedLength,
        )
    }

    private fun resample(points: List<PracticePoint>, count: Int): List<PracticePoint> {
        if (points.size == 1) return List(count) { points.first() }
        val cumulative = buildList {
            add(0f)
            points.zipWithNext().forEach { (start, end) ->
                add(last() + distance(start, end))
            }
        }
        val total = cumulative.last()
        if (total <= 0f) return List(count) { points.first() }
        return List(count) { sampleIndex ->
            val target = total * sampleIndex / (count - 1)
            val segment = cumulative.indexOfFirst { it >= target }
                .coerceAtLeast(1)
                .coerceAtMost(points.lastIndex)
            val startDistance = cumulative[segment - 1]
            val segmentLength = (cumulative[segment] - startDistance).coerceAtLeast(0.0001f)
            val fraction = ((target - startDistance) / segmentLength).coerceIn(0f, 1f)
            val start = points[segment - 1]
            val end = points[segment]
            PracticePoint(
                x = start.x + (end.x - start.x) * fraction,
                y = start.y + (end.y - start.y) * fraction,
            )
        }
    }

    private fun List<PracticePoint>.withoutNearDuplicates(): List<PracticePoint> = buildList {
        this@withoutNearDuplicates.forEach { point ->
            if (isEmpty() || distance(last(), point) >= 0.002f) add(point)
        }
    }

    private fun StrokePoint.toPracticePoint(): PracticePoint = PracticePoint(
        x = x / 1024f,
        y = (900f - y) / 1024f,
    )

    private fun polylineLength(points: List<PracticePoint>): Float =
        points.zipWithNext().sumOf { (start, end) -> distance(start, end).toDouble() }.toFloat()

    private fun distance(left: PracticePoint, right: PracticePoint): Float =
        hypot(left.x - right.x, left.y - right.y)
}
