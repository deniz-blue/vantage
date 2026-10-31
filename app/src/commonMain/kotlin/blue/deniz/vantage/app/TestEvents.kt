package blue.deniz.vantage.app

import directory.evnt.Activity
import directory.evnt.Component
import directory.evnt.OpenEvnt
import directory.evnt.Occurrence
import directory.evnt.PartialDate
import directory.evnt.Translations
import directory.evnt.Venue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private fun english(text: String): Translations = mapOf("en" to text)

private fun markdown(text: String) = Component.MarkdownText(english(text.trimIndent()))

private fun link(url: String, name: String) = Component.Link(url, english(name))

private fun place(name: String?, address: String, countryCode: String, latitude: Double? = null, longitude: Double? = null) = Venue(
    name = name?.let(::english),
    address = address,
    countryCode = countryCode,
    components = if (latitude != null && longitude != null) listOf(Component.Coordinates(latitude, longitude)) else null,
)

@OptIn(ExperimentalUuidApi::class)
private fun testEvent(
    name: Translations,
    occurrences: List<Occurrence>?,
    venue: Venue? = null,
    status: OpenEvnt.Status? = null,
    components: List<Component>? = null,
) = OpenEvnt(id = "urn:uuid:${Uuid.random()}", name = name, status = status, venue = venue, occurrences = occurrences, components = components)

fun testEvents(now: Instant, timeZone: TimeZone): List<OpenEvnt> {
    val zone = timeZone.id
    val today = now.toLocalDateTime(timeZone).date
    fun day(daysAhead: Int) = PartialDate.Day(today.plus(daysAhead, DateTimeUnit.DAY), zone)
    fun time(daysAhead: Int, hour: Int, minute: Int = 0) = day(daysAhead).toTime(hour, minute)
    val soon = PartialDate.fromInstant(now + 3.hours, zone).copy(minute = 0)
    val soonEnd = PartialDate.fromInstant(now + 6.hours, zone).copy(minute = 0)
    val nextMonth = today.plus(1, DateTimeUnit.MONTH)
    val tokyo = "Asia/Tokyo"
    val tokyoDay = today.plus(20, DateTimeUnit.DAY)
    val oldTown = place("Vilnius Town Hall", "Didžioji g. 31, Vilnius", "LT", 54.6788, 25.2873)

    return listOf(
        testEvent(
            english("Board Game Night"),
            listOf(Occurrence(start = soon, end = soonEnd)),
            venue = place("Vilnius University Library", "Universiteto g. 3, Vilnius", "LT", 54.6827, 25.2876),
            components = listOf(
                markdown(
                    """
                    Bring a game or play one of ours. **Free entry**, snacks welcome.

                    - Beginners table until 20:00
                    - Tournament after
                    """,
                ),
                link("https://example.com/board-games", "Sign Up"),
            ),
        ),
        testEvent(
            english("Morning Run"),
            listOf(Occurrence(start = time(1, 7, 30), end = time(1, 8, 30))),
            venue = place("Vingis Park", "M. K. Čiurlionio g. 100, Vilnius", "LT"),
        ),
        testEvent(
            english("Pottery Workshop"),
            listOf(Occurrence(start = time(3, 18), end = time(3, 20, 30))),
            venue = oldTown,
            status = OpenEvnt.Status.Uncertain,
            components = listOf(markdown("Dates might still move, the studio confirms a week before.")),
        ),
        testEvent(
            english("Online Talk: Calendars Are Hard"),
            listOf(Occurrence(start = time(4, 17), end = time(4, 18))),
            components = listOf(
                Component.PlainText(english("A talk about time zones, partial dates and why nobody agrees on weeks.")),
                link("https://example.com/talk", "Join Stream"),
            ),
        ),
        testEvent(
            english("Concert in the Courtyard"),
            listOf(Occurrence(start = time(5, 20), end = time(5, 22))),
            venue = oldTown,
            status = OpenEvnt.Status.Cancelled,
            components = listOf(markdown("Cancelled because of the weather. Tickets are refunded.")),
        ),
        testEvent(
            english("Weekly Language Exchange"),
            listOf(
                Occurrence(start = time(2, 18, 30), end = time(2, 20)),
                Occurrence(start = time(9, 18, 30), end = time(9, 20), status = Occurrence.Status.Cancelled),
                Occurrence(start = time(16, 18, 30), end = time(16, 20)),
            ),
            venue = place("Café Library", "Pilies g. 22, Vilnius", "LT", 54.6830, 25.2895),
        ),
        testEvent(
            english("Jazz Evening"),
            listOf(Occurrence(start = time(7, 21), end = time(8, 0, 30))),
            venue = place("Jazz Club", "Vilniaus g. 14, Vilnius", "LT"),
            components = listOf(link("https://example.com/jazz/tickets", "Tickets")),
        ),
        testEvent(
            english("Spring Market"),
            listOf(Occurrence(start = day(10))),
            venue = place("Cathedral Square", "Katedros a., Vilnius", "LT", 54.6858, 25.2877),
        ),
        testEvent(
            mapOf("en" to "Game Convention", "lt" to "Žaidimų konventas", "tr" to "Oyun Fuarı"),
            listOf(
                Occurrence(
                    start = day(14),
                    end = day(16),
                    activities = listOf(
                        Activity(english("Opening Ceremony")),
                        Activity(english("Cosplay Contest")),
                        Activity(english("Retro Arcade"), status = Activity.Status.Cancelled),
                    ),
                ),
            ),
            venue = place("LITEXPO", "Laisvės pr. 5, Vilnius", "LT", 54.6862, 25.2148),
            components = listOf(
                markdown("Three days of games, cosplay and artists' alley."),
                link("https://example.com/convention", "Website"),
                link("https://example.com/convention/tickets", "Tickets"),
            ),
        ),
        testEvent(
            mapOf("en" to "Tokyo Anime Expo", "ja" to "東京アニメエキスポ"),
            listOf(
                Occurrence(
                    start = PartialDate.Time(tokyoDay.year, tokyoDay.month.number, tokyoDay.day, 10, 0, tokyo),
                    end = PartialDate.Time(tokyoDay.year, tokyoDay.month.number, tokyoDay.day, 18, 0, tokyo),
                ),
            ),
            venue = place("Tokyo Big Sight", "3-11-1 Ariake, Koto City, Tokyo", "JP", 35.6298, 139.7942),
        ),
        testEvent(
            english("The Unreasonably Long Annual Gathering of People Who Like Naming Things Far Too Thoroughly, Third Edition"),
            listOf(Occurrence(start = time(25, 12), end = time(25, 16))),
            venue = place(
                "A Venue With an Equally Long Name That Keeps Going for Quite a While",
                "Some Very Long Street Name g. 123-45, LT-01234 Vilnius",
                "LT",
            ),
        ),
        testEvent(
            english("Postponed Meetup"),
            listOf(Occurrence(start = PartialDate.Month(nextMonth.year, nextMonth.month.number, zone))),
            status = OpenEvnt.Status.Postponed,
        ),
        testEvent(
            english("Summer Festival"),
            listOf(Occurrence(start = PartialDate.Year(today.year + 1, zone))),
            venue = place(null, "Kaunas", "LT"),
        ),
    )
}
