package blue.deniz.vantage.core

import kotlin.test.Test
import kotlin.test.assertEquals

class TypedTimeOfDayTest {
    private val sixPm = TypedTimeOfDay(hour = 18, minute = 0, typingMinute = false)

    private fun TypedTimeOfDay.typed(keys: String) = keys.fold(this) { time, key ->
        if (key == ':') time.typingMinuteNext() else time.plusDigit(key.digitToInt())
    }

    private fun TypedTimeOfDay.clock() = hour to minute

    @Test
    fun fourDigitsAreHoursThenMinutes() {
        assertEquals(19 to 30, sixPm.typed("1930").clock())
        assertEquals(9 to 30, sixPm.typed("0930").clock())
    }

    @Test
    fun anHourThatCannotHaveASecondDigitMovesOnToMinutes() {
        assertEquals(9 to 30, sixPm.typed("930").clock())
        assertEquals(2 to 50, sixPm.typed("250").clock())
    }

    @Test
    fun aColonEndsTheHour() {
        assertEquals(1 to 45, sixPm.typed("1:45").clock())
    }

    @Test
    fun aMinuteThatCannotHaveASecondDigitIsDone() {
        val typed = sixPm.typed("187")
        assertEquals(18 to 7, typed.clock())
        assertEquals(18 to 15, typed.typed("15").clock())
    }

    @Test
    fun theFirstDigitShowsAtOnce() {
        val typed = sixPm.typed("1")
        assertEquals(1 to 0, typed.clock())
        assertEquals(false, typed.typingMinute)
    }
}
