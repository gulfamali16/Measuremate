package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY createdAt DESC")
    fun getAllMeasurements(): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getMeasurementsForProject(projectId: Long): Flow<List<Measurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: Measurement): Long

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Long)

    @Query("DELETE FROM measurements WHERE projectId = :projectId")
    suspend fun deleteMeasurementsForProject(projectId: Long)
}
