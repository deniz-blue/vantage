package blue.deniz.vantage.core

import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WeatherForecastsTest {
    @Test
    fun readsOpenMeteoForecast() = runTest {
        val url = "https://meteo.example/v1/forecast?latitude=54.68&longitude=25.28&current=temperature_2m%2Cweather_code%2Cis_day" +
            "&daily=weather_code%2Ctemperature_2m_max%2Ctemperature_2m_min&timezone=auto&forecast_days=4"
        val server = FakeServer(mapOf(
            url to {
                respond(
                    """{"current":{"time":"2026-10-21T09:45","temperature_2m":7.3,"weather_code":3,"is_day":1},
                        "daily":{"time":["2026-10-21","2026-10-22","2026-10-23"],"weather_code":[61,0,95],
                                 "temperature_2m_max":[9.1,11.0,8.4],"temperature_2m_min":[2.5,3.0,null]}}""",
                )
            },
        ))
        assertEquals(
            WeatherForecast(
                CurrentWeather(7.3, WeatherCondition.Cloudy, isDay = true),
                listOf(
                    DayForecast(LocalDate(2026, 10, 21), WeatherCondition.Rain, 2.5, 9.1),
                    DayForecast(LocalDate(2026, 10, 22), WeatherCondition.Clear, 3.0, 11.0),
                ),
            ),
            WeatherForecasts(server.client, "https://meteo.example/v1/forecast").fetch(54.68, 25.28),
        )
    }
}
