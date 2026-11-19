package com.ucr.zenithsky.model

import org.json.JSONObject
import retrofit2.Response

// --- MODELOS DE RESPUESTA (GET) ---
data class Airport(
    val airportId: Int = 0,
    val iata: String = "",
    val icao: String = "",
    val name: String = ""
)

data class Airline(
    val airlineId: Int = 0,
    val iata: String = "",
    val airlineName: String = "",
    val baseAirport: Airport? = null
)

data class AirplaneType(
    val typeId: Int = 0,
    val identifier: String = "",
    val description: String = ""
)

data class Airplane(
    val airplaneId: Int = 0,
    val capacity: Int = 0,
    val airplaneType: AirplaneType? = null,
    val airline: Airline? = null
)

// --- MODELOS DE PETICIÓN (POST / PUT) ---
data class AirportRequest(
    val iata: String,
    val icao: String,
    val name: String
)

data class AirlineRequest(
    val iata: String,
    val airlinename: String,
    val baseAirport: Int
)

data class AirplaneRequest(
    val capacity: Int,
    val typeId: Int,
    val airlineId: Int
)

// --- PARSER DE ERRORES DEL BACKEND C# ---
fun <T> parseApiError(response: Response<T>): String {
    return try {
        val raw = response.errorBody()?.string()
        if (!raw.isNullOrBlank()) {
            val json = JSONObject(raw)
            json.optString("message", "Error HTTP ${response.code()}")
        } else {
            "Error HTTP ${response.code()}"
        }
    } catch (_: Exception) {
        "Error HTTP ${response.code()}"
    }
}