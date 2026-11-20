package com.ucr.zenithsky.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Entity
import com.ucr.zenithsky.entity.AvionDestacado
import com.ucr.zenithsky.entity.OperacionLog
import kotlinx.coroutines.flow.Flow


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