package blue.deniz.vantage.core

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val DIGIT_BASE = 10

data class TypedTimeOfDay(val hour: Int, val minute: Int, val typingMinute: Boolean, val firstDigit: Int? = null) {
    fun plusDigit(digit: Int): TypedTimeOfDay = if (typingMinute) plusMinuteDigit(digit) else plusHourDigit(digit)

    fun typingMinuteNext(): TypedTimeOfDay = copy(typingMinute = true, firstDigit = null)

    private fun plusHourDigit(digit: Int): TypedTimeOfDay {
        val first = firstDigit
            ?: return if (digit * DIGIT_BASE < HOURS_PER_DAY) copy(hour = digit, firstDigit = digit) else copy(hour = digit).typingMinuteNext()
        val hour = first * DIGIT_BASE + digit
        return if (hour < HOURS_PER_DAY) copy(hour = hour).typingMinuteNext() else typingMinuteNext().plusMinuteDigit(digit)
    }

    private fun plusMinuteDigit(digit: Int): TypedTimeOfDay {
        val first = firstDigit
            ?: return if (digit * DIGIT_BASE < MINUTES_PER_HOUR) copy(minute = digit, firstDigit = digit) else copy(minute = digit)
        return copy(minute = first * DIGIT_BASE + digit, firstDigit = null)
    }
}
