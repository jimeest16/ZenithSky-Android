package com.ucr.zenithsky.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// Entidades de Room para la base de datos local SQLite
@Entity(tableName = "operacion_log")
data class OperacionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entidad: String,
    val accion: String,
    val detalle: String,
    val creadoEn: Long = System.currentTimeMillis()
)

@Entity(tableName = "avion_destacado")
data class AvionDestacado(
    @PrimaryKey val airplaneId: Int,
    val capacity: Int,
    val tipoAvion: String,
    val aerolinea: String
)

// DAO con funciones suspend y Flow<List<...>> para flujos reactivos en Compose
@Dao
interface ZenithDao {
    @Insert
    suspend fun insertarLog(log: OperacionLog)

    @Query("SELECT * FROM operacion_log ORDER BY creadoEn DESC")
    fun obtenerLogs(): Flow<List<OperacionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun destacarAvion(avion: AvionDestacado)

    @Delete
    suspend fun quitarDestacado(avion: AvionDestacado)

    @Query("SELECT * FROM avion_destacado ORDER BY airplaneId ASC")
    fun obtenerDestacados(): Flow<List<AvionDestacado>>
}

// Base de datos Room con patrón Singleton en companion object
@Database(
    entities = [OperacionLog::class, AvionDestacado::class],
    version = 1,
    exportSchema = false
)
abstract class ZenithDatabase : RoomDatabase() {
    abstract fun zenithDao(): ZenithDao

    companion object {
        @Volatile
        private var INSTANCE: ZenithDatabase? = null

        fun getDatabase(context: Context): ZenithDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenithDatabase::class.java,
                    "zenithsky_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}