package com.ucr.zenithsky.network

import com.ucr.zenithsky.model.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ZenithApiService {
    // aeropuertos
    @GET("api/airports")
    suspend fun getAirports(@Query("search") search: String? = null): Response<List<Airport>>

    @POST("api/airports")
    suspend fun createAirport(@Body request: AirportRequest): Response<Airport>

    @PUT("api/airports/{id}")
    suspend fun updateAirport(@Path("id") id: Int, @Body request: AirportRequest): Response<Unit>

    @DELETE("api/airports/{id}")
    suspend fun deleteAirport(@Path("id") id: Int): Response<Unit>

    // aerolineas
    @GET("api/airlines")
    suspend fun getAirlines(): Response<List<Airline>>

    @POST("api/airlines")
    suspend fun createAirline(@Body request: AirlineRequest): Response<Airline>

    @PUT("api/airlines/{id}")
    suspend fun updateAirline(@Path("id") id: Int, @Body request: AirlineRequest): Response<Unit>

    @DELETE("api/airlines/{id}")
    suspend fun deleteAirline(@Path("id") id: Int): Response<Unit>

    // tipos y aeronaves
    @GET("api/airplane-types")
    suspend fun getAirplaneTypes(): Response<List<AirplaneType>>

    @GET("api/airplanes")
    suspend fun getAirplanes(
        @Query("typeId") typeId: Int? = null,
        @Query("airlineId") airlineId: Int? = null
    ): Response<List<Airplane>>

    @POST("api/airplanes")
    suspend fun createAirplane(@Body request: AirplaneRequest): Response<Airplane>

    @PUT("api/airplanes/{id}")
    suspend fun updateAirplane(@Path("id") id: Int, @Body request: AirplaneRequest): Response<Unit>

    @DELETE("api/airplanes/{id}")
    suspend fun deleteAirplane(@Path("id") id: Int): Response<Unit>
}

// se utilizo para el canal entre el back y front
// con el puerto 5175

    object RetrofitClient {
        private const val BASE_URL = "http://127.0.0.1:5175/"

    val api: ZenithApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZenithApiService::class.java)
    }
}

