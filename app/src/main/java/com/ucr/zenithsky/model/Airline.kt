package com.ucr.zenithsky.model

data class Airline(
    val airlineId: Int = 0,
    val iata: String = "",
    val airlineName: String = "",
    val baseAirport: Airport? = null
)

data class AirlineRequest(
    val iata: String,
    val airlinename: String,
    val baseAirport: Int
)