package fr.kairossolum.pangmao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {
    @Test
    fun `good reviews expand intervals`() {
        val first = ReviewScheduler.schedule(ReviewState(0, 2.5, 0, 0), ReviewRating.GOOD)
        val second = ReviewScheduler.schedule(first, ReviewRating.GOOD)
        val third = ReviewScheduler.schedule(second, ReviewRating.GOOD)

        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertTrue(third.intervalDays > second.intervalDays)
        assertEquals(3, third.repetitions)
    }

    @Test
    fun `again resets repetition and records lapse`() {
        val result = ReviewScheduler.schedule(ReviewState(12, 2.5, 4, 1), ReviewRating.AGAIN)

        assertEquals(0, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertEquals(2, result.lapses)
        assertEquals(2.3, result.ease, 0.001)
    }

    @Test
    fun `ease never falls below floor`() {
        var state = ReviewState(10, 1.3, 3, 0)
        repeat(10) { state = ReviewScheduler.schedule(state, ReviewRating.AGAIN) }
        assertEquals(1.3, state.ease, 0.001)
    }
}

