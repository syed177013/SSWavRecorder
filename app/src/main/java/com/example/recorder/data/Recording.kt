package com.example.recorder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val path: String, val durationMs: Long,
    val createdAt: Long, val format: String, val sizeBytes: Long,
)

@Dao interface RecordingDao {
    @Query("SELECT * FROM Recording ORDER BY createdAt DESC") fun all(): Flow<List<Recording>>
    @Insert suspend fun insert(r: Recording): Long
    @Update suspend fun update(r: Recording)
    @Delete suspend fun delete(r: Recording)
}

@Database(entities = [Recording::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() { abstract fun dao(): RecordingDao }
