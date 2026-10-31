package directory.evnt

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Clock
import kotlin.time.Instant

private val PARTIAL_DATE_PATTERN =
    Regex("""([0-9]{4})(?:-([0-9]{2})(?:-([0-9]{2})(?:T([0-9]{2}):([0-9]{2}))?)?)?\[([A-Za-z][A-Za-z0-9_/+-]*)]""")

internal expect val ianaTimeZoneIds: Set<String>

private fun isKnownTimeZoneId(timeZoneId: String) = timeZoneId in ianaTimeZoneIds

private fun isLeapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun lengthOfMonth(year: Int, month: Int) = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun problemWith(year: Int, month: Int?, day: Int?, hour: Int?, minute: Int?, timeZoneId: String): String? =
    when {
        year !in 0..9999 -> "year $year is not between 0 and 9999"
        month == null && day != null -> "a day needs a month"
        day == null && hour != null -> "a time needs a day"
        (hour == null) != (minute == null) -> "an hour and a minute go together"
        month != null && month !in 1..12 -> "month $month does not exist"
        month != null && day != null && day !in 1..lengthOfMonth(year, month) -> "$year-$month has no day $day"
        hour != null && hour !in 0..23 -> "hour $hour does not exist"
        minute != null && minute !in 0..59 -> "minute $minute does not exist"
        !isKnownTimeZoneId(timeZoneId) -> "$timeZoneId is not an IANA time zone"
        else -> null
    }

private fun requireExists(year: Int, month: Int?, day: Int?, hour: Int?, minute: Int?, timeZoneId: String) {
    problemWith(year, month, day, hour, minute, timeZoneId)?.let { throw IllegalArgumentException("Not a PartialDate: $it") }
}

private fun Int.twoDigits() = toString().padStart(2, '0')

@Serializable(with = PartialDateSerializer::class)
sealed class PartialDate {
    abstract val precision: Precision
    abstract val year: Int
    abstract val timeZoneId: String

    enum class Precision { Year, Month, Day, Time }

    enum class Fill { First, Last }

    val timeZone: TimeZone get() = TimeZone.of(timeZoneId)

    private val monthOrNull: Int?
        get() = when (this) {
            is Year -> null
            is Month -> month
            is Day -> month
            is Time -> month
        }

    private val dayOrNull: Int?
        get() = when (this) {
            is Year, is Month -> null
            is Day -> day
            is Time -> day
        }

    val startInstant: Instant by lazy {
        when (this) {
            is Time -> LocalDateTime(year, month, day, hour, minute).toInstant(timeZone)
            else -> LocalDate(year, monthOrNull ?: 1, dayOrNull ?: 1).atStartOfDayIn(timeZone)
        }
    }

    val endInstant: Instant by lazy {
        when (this) {
            is Time -> startInstant
            is Day -> LocalDate(year, month, day).plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
            is Month -> LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).atStartOfDayIn(timeZone)
            is Year -> LocalDate(year + 1, 1, 1).atStartOfDayIn(timeZone)
        }
    }

    fun has(precision: Precision) = this.precision >= precision

    fun toYear() = Year(year, timeZoneId)

    fun withPrecision(precision: Precision, fill: Fill = Fill.First): PartialDate {
        val last = fill == Fill.Last
        val month = monthOrNull ?: if (last) 12 else 1
        val day = dayOrNull ?: if (last) lengthOfMonth(year, month) else 1
        return when (precision) {
            Precision.Year -> Year(year, timeZoneId)
            Precision.Month -> Month(year, month, timeZoneId)
            Precision.Day -> Day(year, month, day, timeZoneId)
            Precision.Time -> (this as? Time) ?: Time(year, month, day, if (last) 23 else 0, if (last) 59 else 0, timeZoneId)
        }
    }

    fun isAfter(other: PartialDate) = startInstant >= other.endInstant && startInstant != other.startInstant

    fun isBefore(other: PartialDate) = other.isAfter(this)

    operator fun contains(instant: Instant) =
        if (this is Time) instant == startInstant else instant >= startInstant && instant < endInstant

    operator fun contains(other: PartialDate) =
        if (other is Time) other.startInstant in this
        else startInstant <= other.startInstant && other.endInstant <= endInstant

    fun intersects(other: PartialDate) = when {
        this is Time -> startInstant in other
        other is Time -> other.startInstant in this
        else -> startInstant < other.endInstant && other.startInstant < endInstant
    }

    final override fun toString() = buildString {
        append(year.toString().padStart(4, '0'))
        monthOrNull?.let { append('-').append(it.twoDigits()) }
        dayOrNull?.let { append('-').append(it.twoDigits()) }
        if (this@PartialDate is Time) append('T').append(hour.twoDigits()).append(':').append(minute.twoDigits())
        append('[').append(timeZoneId).append(']')
    }

    data class Year(override val year: Int, override val timeZoneId: String) : PartialDate() {
        override val precision get() = Precision.Year

        init {
            requireExists(year, null, null, null, null, timeZoneId)
        }

        fun toMonth(month: Int) = Month(year, month, timeZoneId)
    }

    data class Month(override val year: Int, val month: Int, override val timeZoneId: String) : PartialDate() {
        override val precision get() = Precision.Month

        init {
            requireExists(year, month, null, null, null, timeZoneId)
        }

        val daysInMonth get() = lengthOfMonth(year, month)

        fun toDay(day: Int) = Day(year, month, day, timeZoneId)
    }

    data class Day(override val year: Int, val month: Int, val day: Int, override val timeZoneId: String) : PartialDate() {
        override val precision get() = Precision.Day

        constructor(date: LocalDate, timeZoneId: String) : this(date.year, date.month.number, date.day, timeZoneId)

        init {
            requireExists(year, month, day, null, null, timeZoneId)
        }

        fun toMonth() = Month(year, month, timeZoneId)

        fun toTime(hour: Int, minute: Int) = Time(year, month, day, hour, minute, timeZoneId)

        fun toLocalDate() = LocalDate(year, month, day)
    }

    data class Time(
        override val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        override val timeZoneId: String,
    ) : PartialDate() {
        override val precision get() = Precision.Time

        constructor(dateTime: LocalDateTime, timeZoneId: String) :
            this(dateTime.year, dateTime.month.number, dateTime.day, dateTime.hour, dateTime.minute, timeZoneId)

        init {
            requireExists(year, month, day, hour, minute, timeZoneId)
        }

        fun toMonth() = Month(year, month, timeZoneId)

        fun toDay() = Day(year, month, day, timeZoneId)

        fun toLocalDate() = LocalDate(year, month, day)

        fun toLocalDateTime() = LocalDateTime(year, month, day, hour, minute)
    }

    companion object {
        fun of(
            year: Int,
            month: Int? = null,
            day: Int? = null,
            hour: Int? = null,
            minute: Int? = null,
            timeZoneId: String,
        ): PartialDate {
            requireExists(year, month, day, hour, minute, timeZoneId)
            return when {
                hour != null -> Time(year, month!!, day!!, hour, minute!!, timeZoneId)
                day != null -> Day(year, month!!, day, timeZoneId)
                month != null -> Month(year, month, timeZoneId)
                else -> Year(year, timeZoneId)
            }
        }

        fun parse(text: String): PartialDate {
            val groups = PARTIAL_DATE_PATTERN.matchEntire(text)?.groupValues
                ?: throw IllegalArgumentException("$text is not a PartialDate")
            fun number(index: Int) = groups[index].takeIf { it.isNotEmpty() }?.toInt()
            return of(groups[1].toInt(), number(2), number(3), number(4), number(5), groups[6])
        }

        fun parseOrNull(text: String) = try {
            parse(text)
        } catch (_: IllegalArgumentException) {
            null
        }

        fun isValid(text: String) = parseOrNull(text) != null

        fun isTimeZoneId(timeZoneId: String) = isKnownTimeZoneId(timeZoneId)

        fun fromInstant(instant: Instant, timeZoneId: String): Time {
            require(isTimeZoneId(timeZoneId)) { "$timeZoneId is not an IANA time zone" }
            return Time(instant.toLocalDateTime(TimeZone.of(timeZoneId)), timeZoneId)
        }

        fun now(timeZoneId: String = TimeZone.currentSystemDefault().id) = fromInstant(Clock.System.now(), timeZoneId)

        fun compare(one: PartialDate, other: PartialDate): Int {
            val byStart = one.startInstant.compareTo(other.startInstant)
            return if (byStart != 0) byStart else other.precision.compareTo(one.precision)
        }
    }
}

internal object PartialDateSerializer : KSerializer<PartialDate> {
    override val descriptor = PrimitiveSerialDescriptor("directory.evnt.PartialDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PartialDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = PartialDate.parse(decoder.decodeString())
}
