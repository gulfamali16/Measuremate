package com.example.ui.measurement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.Measurement
import com.example.data.database.Project
import com.example.data.repository.MeasurementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeasurementViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = MeasurementRepository(db.projectDao(), db.measurementDao())

    // All offline lists
    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val measurements: StateFlow<List<Measurement>> = repository.allMeasurements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected state
    private val _selectedUnit = MutableStateFlow(AppUnit.METERS)
    val selectedUnit = _selectedUnit.asStateFlow()

    private val _selectedMode = MutableStateFlow(MeasurementMode.LENGTH)
    val selectedMode = _selectedMode.asStateFlow()

    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId = _currentProjectId.asStateFlow()

    // Tracking states
    private val _trackingConfidence = MutableStateFlow(95)
    val trackingConfidence = _trackingConfidence.asStateFlow()

    // Interactive simulator points in 3D
    private val _tappedPoints = MutableStateFlow<List<ArPoint>>(emptyList())
    val tappedPoints = _tappedPoints.asStateFlow()

    // Is measuring active right now
    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring = _isMeasuring.asStateFlow()

    // Active UI screen
    private val _activeScreen = MutableStateFlow(ActiveScreen.CAMERA)
    val activeScreen = _activeScreen.asStateFlow()

    // Custom simulated scene index (to change simulated environments like Couch, Window, Kitchen Table)
    private val _selectedSceneIndex = MutableStateFlow(0)
    val selectedSceneIndex = _selectedSceneIndex.asStateFlow()

    enum class MeasurementMode { LENGTH, HEIGHT, AREA }
    enum class ActiveScreen { CAMERA, HISTORY, PROJECTS, SETTINGS }

    init {
        viewModelScope.launch {
            projects.collect { list ->
                if (list.isEmpty()) {
                    repository.insertProject(Project(name = "Quick Measuring"))
                } else if (_currentProjectId.value == null) {
                    _currentProjectId.value = list.first().id
                }
            }
        }
    }

    fun navigateTo(screen: ActiveScreen) {
        _activeScreen.value = screen
    }

    fun selectUnit(unit: AppUnit) {
        _selectedUnit.value = unit
    }

    fun selectMode(mode: MeasurementMode) {
        _selectedMode.value = mode
        resetPoints()
    }

    fun selectProject(projectId: Long) {
        _currentProjectId.value = projectId
    }

    fun selectScene(index: Int) {
        _selectedSceneIndex.value = index
        resetPoints()
    }

    fun addPoint(point: ArPoint) {
        val current = _tappedPoints.value.toMutableList()
        when (_selectedMode.value) {
            MeasurementMode.LENGTH -> {
                if (current.size >= 2) {
                    current.clear()
                }
                current.add(point)
            }
            MeasurementMode.HEIGHT -> {
                if (current.size >= 2) {
                    current.clear()
                }
                if (current.isEmpty()) {
                    current.add(point) // ground base
                } else {
                    val ground = current[0]
                    current.add(ArPoint(ground.x, point.y, ground.z, "Top Height Annotation"))
                }
            }
            MeasurementMode.AREA -> {
                if (current.size >= 4) {
                    current.clear()
                }
                current.add(point)
            }
        }
        _tappedPoints.value = current
        _isMeasuring.value = true
        _trackingConfidence.value = (86..99).random()
    }

    fun undoLastPoint() {
        val current = _tappedPoints.value.toMutableList()
        if (current.isNotEmpty()) {
            current.removeAt(current.size - 1)
            _tappedPoints.value = current
        }
        if (current.isEmpty()) {
            _isMeasuring.value = false
        }
    }

    fun resetPoints() {
        _tappedPoints.value = emptyList()
        _isMeasuring.value = false
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val id = repository.insertProject(Project(name = name))
            _currentProjectId.value = id
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            // Relocate selected project id safely
            projects.value.firstOrNull { it.id != projectId }?.id?.let {
                _currentProjectId.value = it
            }
        }
    }

    fun saveCurrentMeasurement(note: String) {
        viewModelScope.launch {
            val points = _tappedPoints.value
            if (points.isEmpty()) return@launch

            val projectId = _currentProjectId.value ?: projects.value.firstOrNull()?.id ?: 1L
            val typeStr = _selectedMode.value.name
            val rawValue: Double = calculateCurrentValue()

            val pointsStr = points.joinToString(";") { "${it.x},${it.y},${it.z}" }

            val measurement = Measurement(
                projectId = projectId,
                type = typeStr,
                valueMeters = rawValue,
                unit = _selectedUnit.value.suffix.uppercase(),
                pointsJson = pointsStr,
                note = note,
                confidence = _trackingConfidence.value
            )

            repository.insertMeasurement(measurement)
            resetPoints()
        }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch {
            repository.deleteMeasurement(id)
        }
    }

    fun calculateCurrentValue(): Double {
        val points = _tappedPoints.value
        if (points.size < 2) return 0.0

        return when (_selectedMode.value) {
            MeasurementMode.LENGTH -> {
                MeasurementUtils.calculateDistance3D(
                    points[0].x, points[0].y, points[0].z,
                    points[1].x, points[1].y, points[1].z
                )
            }
            MeasurementMode.HEIGHT -> {
                Math.abs(points[1].y - points[0].y).toDouble()
            }
            MeasurementMode.AREA -> {
                val areaPoints = points.map { Pair(it.x, it.z) }
                MeasurementUtils.calculateArea2D(areaPoints)
            }
        }
    }
}

// ARPoint class represents simulated 3D coordinates
data class ArPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val label: String = ""
)
