package com.ucr.zenithsky.model

data class Airplane(
    val airplaneId: Int = 0,
    val capacity: Int = 0,
    val airplaneType: AirplaneType? = null,
    val airline: Airline? = null
)

data class AirplaneRequest(
    val capacity: Int,
    val typeId: Int,
    val airlineId: Int
)