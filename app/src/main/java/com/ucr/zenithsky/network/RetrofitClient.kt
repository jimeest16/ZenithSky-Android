package com.ucr.zenithsky.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Canal entre el backend .NET (puerto 5175) y la app Android vía adb reverse
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