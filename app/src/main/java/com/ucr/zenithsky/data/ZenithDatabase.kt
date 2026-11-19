package com.ucr.zenithsky.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OperacionLog(
    val id: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
    val entidad: String,
    val accion: String,
    val detalle: String,
    val creadoEn: Long = System.currentTimeMillis()
)

data class AvionDestacado(
    val airplaneId: Int,
    val capacity: Int,
    val tipoAvion: String,
    val aerolinea: String
)

class ZenithDao(context: Context) {
    private val prefs = context.getSharedPreferences("ZenithSkyLocalDB", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _logsFlow = MutableStateFlow(cargarLogs())
    private val _destacadosFlow = MutableStateFlow(cargarDestacados())

    private fun cargarLogs(): List<OperacionLog> {
        val json = prefs.getString("logs", null) ?: return emptyList()
        val type = object : TypeToken<List<OperacionLog>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun guardarLogs(lista: List<OperacionLog>) {
        prefs.edit().putString("logs", gson.toJson(lista)).apply()
        _logsFlow.value = lista
    }

    private fun cargarDestacados(): List<AvionDestacado> {
        val json = prefs.getString("destacados", null) ?: return emptyList()
        val type = object : TypeToken<List<AvionDestacado>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun guardarDestacados(lista: List<AvionDestacado>) {
        prefs.edit().putString("destacados", gson.toJson(lista)).apply()
        _destacadosFlow.value = lista
    }

    suspend fun insertarLog(log: OperacionLog) {
        val actual = listOf(log) + cargarLogs()
        guardarLogs(actual)
    }

    fun obtenerLogs(): Flow<List<OperacionLog>> = _logsFlow.asStateFlow()

    suspend fun destacarAvion(avion: AvionDestacado) {
        val actual = cargarDestacados().filterNot { it.airplaneId == avion.airplaneId } + avion
        guardarDestacados(actual.sortedBy { it.airplaneId })
    }

    suspend fun quitarDestacado(avion: AvionDestacado) {
        val actual = cargarDestacados().filterNot { it.airplaneId == avion.airplaneId }
        guardarDestacados(actual)
    }

    fun obtenerDestacados(): Flow<List<AvionDestacado>> = _destacadosFlow.asStateFlow()
}

class ZenithDatabase private constructor(context: Context) {
    private val dao = ZenithDao(context.applicationContext)

    fun zenithDao(): ZenithDao = dao

    companion object {
        @Volatile
        private var INSTANCE: ZenithDatabase? = null

        fun getDatabase(context: Context): ZenithDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = ZenithDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}