package com.example.freeassistant.weather

import android.content.Context
import com.example.freeassistant.SettingsManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WeatherRepository {
    data class Weather(
        val city: String,
        val temperatureC: Double,
        val windKmh: Double,
        val condition: String
    )

    fun getWeather(
        context: Context,
        city: String,
        language: String
    ): Result<Weather> {
        val apiKey = SettingsManager.getOpenWeatherApiKey(context).trim()
        return if (apiKey.isNotBlank()) {
            runCatching {
                getOpenWeatherMapWeather(city, language, apiKey)
            }.recoverCatching {
                getOpenMeteoWeather(city, language)
            }
        } else {
            runCatching {
                getOpenMeteoWeather(city, language)
            }
        }
    }

    private fun getOpenWeatherMapWeather(
        city: String,
        language: String,
        apiKey: String
    ): Weather {
        val lang = if (language == "ru") "ru" else "en"
        val encodedCity = URLEncoder.encode(city.trim(), "UTF-8")
        val url = "https://api.openweathermap.org/data/2.5/weather" +
            "?q=$encodedCity" +
            "&appid=$apiKey" +
            "&units=metric" +
            "&lang=$lang"

        val json = JSONObject(readUrl(url))
        val cityName = json.optString("name", city)
        val main = json.getJSONObject("main")
        val temperature = main.getDouble("temp")
        val windObject = json.optJSONObject("wind")
        val windMs = windObject?.optDouble("speed", 0.0) ?: 0.0
        val windKmh = windMs * 3.6
        val weatherArray = json.optJSONArray("weather")
        val condition = weatherArray
            ?.optJSONObject(0)
            ?.optString("description", "weather")
            ?: "weather"

        return Weather(
            city = cityName,
            temperatureC = temperature,
            windKmh = windKmh,
            condition = condition
        )
    }

    private fun getOpenMeteoWeather(
        city: String,
        language: String
    ): Weather {
        val lang = if (language == "ru") "ru" else "en"
        val encodedCity = URLEncoder.encode(city.trim(), "UTF-8")
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=$encodedCity" +
            "&count=1" +
            "&language=$lang" +
            "&format=json"

        val geoJson = readUrl(geoUrl)
        val geo = JSONObject(geoJson)
        val results = geo.optJSONArray("results")
        if (results == null || results.length() == 0) {
            error("City not found: $city")
        }

        val first = results.getJSONObject(0)
        val latitude = first.getDouble("latitude")
        val longitude = first.getDouble("longitude")
        val foundCity = first.optString("name", city)
        val forecastUrl = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude" +
            "&longitude=$longitude" +
            "&current_weather=true"

        val forecastJson = readUrl(forecastUrl)
        val forecast = JSONObject(forecastJson)
        val current = forecast.getJSONObject("current_weather")
        val temperature = current.getDouble("temperature")
        val windKmh = current.getDouble("windspeed")
        val code = current.getInt("weathercode")

        return Weather(
            city = foundCity,
            temperatureC = temperature,
            windKmh = windKmh,
            condition = weatherCodeToCondition(code)
        )
    }

    private fun readUrl(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                error("HTTP $code")
            }
            return connection.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun weatherCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "clear sky"
            1, 2, 3 -> "partly cloudy"
            45, 48 -> "fog"
            51, 53, 55, 56, 57 -> "drizzle"
            61, 63, 65, 66, 67 -> "rain"
            71, 73, 75, 77 -> "snow"
            80, 81, 82 -> "rain showers"
            85, 86 -> "snow showers"
            95 -> "thunderstorm"
            96, 99 -> "thunderstorm with hail"
            else -> "weather code $code"
        }
    }
}
