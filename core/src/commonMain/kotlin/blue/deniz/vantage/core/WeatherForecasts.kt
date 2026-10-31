package blue.deniz.vantage.core

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

const val OPEN_METEO_ENDPOINT = "https://api.open-meteo.com/v1/forecast"
private const val FORECAST_DAYS = 4
private val FORECAST_FRESH_FOR = 30.minutes

enum class WeatherCondition { Clear, PartlyCloudy, Cloudy, Fog, Drizzle, Rain, Snow, Thunderstorm, Unknown }

data class CurrentWeather(val temperature: Double, val condition: WeatherCondition, val isDay: Boolean)

data class DayForecast(val date: LocalDate, val condition: WeatherCondition, val low: Double, val high: Double)

data class WeatherForecast(val current: CurrentWeather, val days: List<DayForecast>)

class WeatherForecasts(
    private val client: HttpClient,
    private val endpoint: String = OPEN_METEO_ENDPOINT,
    clock: Clock = Clock.System,
) {
    private val forecasts = ResultCache<Pair<Double, Double>, WeatherForecast>(FORECAST_FRESH_FOR, clock)

    fun lastForecastFor(latitude: Double, longitude: Double): WeatherForecast? = forecasts.last(latitude to longitude)

    suspend fun forecastFor(latitude: Double, longitude: Double): WeatherForecast =
        forecasts.freshOrLoad(latitude to longitude) { fetch(latitude, longitude) }

    suspend fun fetch(latitude: Double, longitude: Double): WeatherForecast {
        val url = URLBuilder(endpoint).apply {
            parameters.append("latitude", latitude.toString())
            parameters.append("longitude", longitude.toString())
            parameters.append("current", "temperature_2m,weather_code,is_day")
            parameters.append("daily", "weather_code,temperature_2m_max,temperature_2m_min")
            parameters.append("timezone", "auto")
            parameters.append("forecast_days", FORECAST_DAYS.toString())
        }.buildString()
        return forecastOfOpenMeteo(parseJsonObject(client.getOk(url).bodyAsText(), "The weather forecast"))
    }
}

fun weatherConditionOfWmoCode(code: Int?): WeatherCondition = when (code) {
    0 -> WeatherCondition.Clear
    1, 2 -> WeatherCondition.PartlyCloudy
    3 -> WeatherCondition.Cloudy
    45, 48 -> WeatherCondition.Fog
    in 51..57 -> WeatherCondition.Drizzle
    in 61..67, in 80..82 -> WeatherCondition.Rain
    in 71..77, 85, 86 -> WeatherCondition.Snow
    in 95..99 -> WeatherCondition.Thunderstorm
    else -> WeatherCondition.Unknown
}

private fun JsonElement?.doubleOrNull() = (this as? JsonPrimitive)?.doubleOrNull

private fun JsonElement?.intOrNull() = (this as? JsonPrimitive)?.intOrNull

private fun forecastOfOpenMeteo(response: JsonObject): WeatherForecast {
    val current = response["current"] as? JsonObject ?: failure(EventError.Kind.Parse, "The weather forecast has no current weather")
    val daily = response["daily"] as? JsonObject
    fun column(key: String) = (daily?.get(key) as? JsonArray).orEmpty()
    val dates = column("time")
    val codes = column("weather_code")
    val highs = column("temperature_2m_max")
    val lows = column("temperature_2m_min")
    val days = dates.indices.mapNotNull { index ->
        val date = dates[index].stringOrNull()?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
        val low = lows.getOrNull(index).doubleOrNull() ?: return@mapNotNull null
        val high = highs.getOrNull(index).doubleOrNull() ?: return@mapNotNull null
        DayForecast(date, weatherConditionOfWmoCode(codes.getOrNull(index).intOrNull()), low, high)
    }
    return WeatherForecast(
        CurrentWeather(
            temperature = current["temperature_2m"].doubleOrNull() ?: failure(EventError.Kind.Parse, "The weather forecast has no temperature"),
            condition = weatherConditionOfWmoCode(current["weather_code"].intOrNull()),
            isDay = current["is_day"].intOrNull() != 0,
        ),
        days,
    )
}
