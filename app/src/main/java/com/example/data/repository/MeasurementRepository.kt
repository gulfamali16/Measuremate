package com.example.data.repository

import com.example.data.database.Measurement
import com.example.data.database.MeasurementDao
import com.example.data.database.Project
import com.example.data.database.ProjectDao
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(
    private val projectDao: ProjectDao,
    private val measurementDao: MeasurementDao
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val allMeasurements: Flow<List<Measurement>> = measurementDao.getAllMeasurements()

    fun getMeasurementsForProject(projectId: Long): Flow<List<Measurement>> {
        return measurementDao.getMeasurementsForProject(projectId)
    }

    suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun deleteProject(projectId: Long) {
        measurementDao.deleteMeasurementsForProject(projectId)
        projectDao.deleteProjectById(projectId)
    }

    suspend fun insertMeasurement(measurement: Measurement): Long {
        return measurementDao.insertMeasurement(measurement)
    }

    suspend fun deleteMeasurement(id: Long) {
        measurementDao.deleteMeasurementById(id)
    }
}
