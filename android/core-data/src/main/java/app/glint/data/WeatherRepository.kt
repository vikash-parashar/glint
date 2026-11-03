package app.glint.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import kotlin.math.roundToInt

data class DayForecast(val date: LocalDate, val highC: Int, val lowC: Int, val code: Int)

data class Weather(
    val city: String,
    val tempC: Int,
    val feelsC: Int,
    val highC: Int,
    val lowC: Int,
    val code: Int,
    val days: List<DayForecast>,
) {
    val condition: String get() = WeatherCodes.describe(code)
}

object WeatherCodes {
    /** WMO weather interpretation codes, shortened for small widgets. */
    fun describe(code: Int) = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67, in 80..82 -> "Rain"
        in 71..77, 85, 86 -> "Snow"
        in 95..99 -> "Storm"
        else -> "—"
    }

    /** One-character glyph that renders in every system font. */
    fun glyph(code: Int) = when (code) {
        0 -> "☀"
        1, 2 -> "⛅"
        3, 45, 48 -> "☁"
        in 51..67, in 80..82 -> "☂"
        in 71..77, 85, 86 -> "❄"
        in 95..99 -> "⚡"
        else -> "·"
    }
}

/**
 * Weather by city name: no location permission. Uses Open-Meteo (no API key).
 * Open-Meteo's free tier is non-commercial: before a paid launch, buy their
 * commercial plan or route this through the Go backend with a cache.
 * Process-wide 30-minute cache so many widgets don't each hit the network.
 */
object WeatherRepository {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cache: Triple<String, Long, Weather>? = null

    suspend fun forCity(city: String): Weather? = withContext(Dispatchers.IO) {
        cache?.let { (c, at, w) ->
            if (c.equals(city, true) && System.currentTimeMillis() - at < 30 * 60_000) return@withContext w
        }
        runCatching {
            val q = URLEncoder.encode(city, "UTF-8")
            val geo = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1")
            val place = geo["results"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@runCatching null
            val lat = place["latitude"]!!.jsonPrimitive.double
            val lon = place["longitude"]!!.jsonPrimitive.double
            val f = get(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,apparent_temperature,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=4&timezone=auto",
            )
            val cur = f["current"]!!.jsonObject
            val daily = f["daily"]!!.jsonObject
            val dates = daily["time"]!!.jsonArray.map { LocalDate.parse(it.jsonPrimitive.content) }
            val highs = daily["temperature_2m_max"]!!.jsonArray.map { it.jsonPrimitive.double.roundToInt() }
            val lows = daily["temperature_2m_min"]!!.jsonArray.map { it.jsonPrimitive.double.roundToInt() }
            val codes = daily["weather_code"]!!.jsonArray.map { it.jsonPrimitive.int }
            val days = dates.indices.map { DayForecast(dates[it], highs[it], lows[it], codes[it]) }
            Weather(
                city = place["name"]!!.jsonPrimitive.content,
                tempC = cur["temperature_2m"]!!.jsonPrimitive.double.roundToInt(),
                feelsC = cur["apparent_temperature"]!!.jsonPrimitive.double.roundToInt(),
                highC = highs.first(), lowC = lows.first(),
                code = cur["weather_code"]!!.jsonPrimitive.int,
                days = days,
            ).also { cache = Triple(city, System.currentTimeMillis(), it) }
        }.getOrNull()
    }

    private fun get(url: String): JsonObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        return c.inputStream.bufferedReader().use { json.parseToJsonElement(it.readText()).jsonObject }
    }
}
