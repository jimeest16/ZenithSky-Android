package com.ucr.zenithsky.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "avion_destacado")
 data class AvionDestacado(
    @PrimaryKey val airplaneId: Int,
    val capacity: Int,
    val tipoAvion: String,
    val aerolinea: String
)