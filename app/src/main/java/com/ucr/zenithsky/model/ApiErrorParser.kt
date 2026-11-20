package com.ucr.zenithsky.model

import org.json.JSONObject
import retrofit2.Response

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