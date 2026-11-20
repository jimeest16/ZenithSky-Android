package com.ucr.zenithsky.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operacion_log")
data class OperacionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entidad: String,
    val accion: String,
    val detalle: String,
    val creadoEn: Long = System.currentTimeMillis()
)
