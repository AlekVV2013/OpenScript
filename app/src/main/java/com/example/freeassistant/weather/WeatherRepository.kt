package com.example.freeassistant.weather

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WeatherRepository(private val context: Context) {

    suspend fun getWeather(city: String): String = withContext(Dispatchers.IO) {
        try {
            val apiKey = "YOUR_API_KEY"
            val urlString = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val response = reader.readText()
                    val json = JSONObject(response)
                    val main = json.getJSONObject("main")
                    val temp = main.getDouble("temp")
                    val weatherArray = json.getJSONArray("weather")
                    val weather = weatherArray.getJSONObject(0)
                    val description = weather.getString("description")
                    
                    "Temperature: ${"%.1f".format(temp)}°C, $description"
                }
            } else {
                "Unable to fetch weather data"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
