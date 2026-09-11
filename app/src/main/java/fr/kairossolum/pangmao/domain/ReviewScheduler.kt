package fr.kairossolum.pangmao.domain

import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewRating { AGAIN, HARD, GOOD, EASY }

data class ReviewState(
    val intervalDays: Int,
    val ease: Double,
    val repetitions: Int,
    val lapses: Int,
)

object ReviewScheduler {
    fun schedule(current: ReviewState, rating: ReviewRating): ReviewState {
        val ease = when (rating) {
            ReviewRating.AGAIN -> max(1.3, current.ease - 0.20)
            ReviewRating.HARD -> max(1.3, current.ease - 0.15)
            ReviewRating.GOOD -> current.ease
            ReviewRating.EASY -> current.ease + 0.15
        }
        val interval = when (rating) {
            ReviewRating.AGAIN -> 0
            ReviewRating.HARD -> max(1, (max(1, current.intervalDays) * 1.2).roundToInt())
            ReviewRating.GOOD -> when (current.repetitions) {
                0 -> 1
                1 -> 3
                else -> max(2, (current.intervalDays * ease).roundToInt())
            }
            ReviewRating.EASY -> when (current.repetitions) {
                0 -> 3
                else -> max(4, (max(1, current.intervalDays) * ease * 1.3).roundToInt())
            }
        }
        return ReviewState(
            intervalDays = interval,
            ease = ease,
            repetitions = if (rating == ReviewRating.AGAIN) 0 else current.repetitions + 1,
            lapses = current.lapses + if (rating == ReviewRating.AGAIN) 1 else 0,
        )
    }
}

