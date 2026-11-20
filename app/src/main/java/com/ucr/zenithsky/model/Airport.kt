package com.ucr.zenithsky.model

data class Airport(
    val airportId: Int = 0,
    val iata: String = "",
    val icao: String = "",
    val name: String = ""
)

data class AirportRequest(
    val iata: String,
    val icao: String,
    val name: String
)