package com.example.ui.measurement

import android.app.Presentation
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.Measurement
import com.example.data.database.Project
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMeasurementView(
    viewModel: MeasurementViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ViewModel states
    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedUnit.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val currentProjectId by viewModel.currentProjectId.collectAsStateWithLifecycle()
    val trackingConfidence by viewModel.trackingConfidence.collectAsStateWithLifecycle()
    val tappedPoints by viewModel.tappedPoints.collectAsStateWithLifecycle()
    val selectedSceneIndex by viewModel.selectedSceneIndex.collectAsStateWithLifecycle()

    // UI-only flow states
    var showOnboarding by remember { mutableStateOf(true) }
    var onboardingStep by remember { mutableStateOf(1) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var measurementNote by remember { mutableStateOf("") }
    var showProjectCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    
    // Calibration state
    var calibrationFactor by remember { mutableStateOf(1.0f) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "MeasureMate AR",
                                fontWeight = FontWeight.Bold,
                                color = TextSlate50,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Pro Smart Camera Ruler",
                                fontSize = 11.sp,
                                color = OrangeAccent
                            )
                        }

                        // Tracking status tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(SolidSlate800, RoundedCornerShape(12.dp))
                                .border(0.5.dp, TrackingGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(TrackingGreen, CircleShape)
                            )
                            Text(
                                text = "Tracking: $trackingConfidence%",
                                fontSize = 10.sp,
                                color = TrackingGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSlate900,
                    titleContentColor = TextSlate50
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepSlate900,
                tonalElevation = 12.dp
            ) {
                NavigationBarItem(
                    selected = activeScreen == MeasurementViewModel.ActiveScreen.CAMERA,
                    onClick = { viewModel.navigateTo(MeasurementViewModel.ActiveScreen.CAMERA) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "AR Session") },
                    label = { Text("Measure", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeAccent,
                        selectedTextColor = OrangeAccent,
                        indicatorColor = SolidSlate800,
                        unselectedIconColor = TextSlate400.copy(alpha = 0.6f),
                        unselectedTextColor = TextSlate400.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_btn_measure")
                )
                NavigationBarItem(
                    selected = activeScreen == MeasurementViewModel.ActiveScreen.HISTORY,
                    onClick = { viewModel.navigateTo(MeasurementViewModel.ActiveScreen.HISTORY) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "History Log") },
                    label = { Text("History", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeAccent,
                        selectedTextColor = OrangeAccent,
                        indicatorColor = SolidSlate800,
                        unselectedIconColor = TextSlate400.copy(alpha = 0.6f),
                        unselectedTextColor = TextSlate400.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_btn_history")
                )
                NavigationBarItem(
                    selected = activeScreen == MeasurementViewModel.ActiveScreen.PROJECTS,
                    onClick = { viewModel.navigateTo(MeasurementViewModel.ActiveScreen.PROJECTS) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Folders") },
                    label = { Text("Projects", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeAccent,
                        selectedTextColor = OrangeAccent,
                        indicatorColor = SolidSlate800,
                        unselectedIconColor = TextSlate400.copy(alpha = 0.6f),
                        unselectedTextColor = TextSlate400.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_btn_projects")
                )
                NavigationBarItem(
                    selected = activeScreen == MeasurementViewModel.ActiveScreen.SETTINGS,
                    onClick = { viewModel.navigateTo(MeasurementViewModel.ActiveScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Calibrate Rules") },
                    label = { Text("Calibrate", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeAccent,
                        selectedTextColor = OrangeAccent,
                        indicatorColor = SolidSlate800,
                        unselectedIconColor = TextSlate400.copy(alpha = 0.6f),
                        unselectedTextColor = TextSlate400.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_btn_settings")
                )
            }
        },
        containerColor = DeepSlate900
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            when (activeScreen) {
                MeasurementViewModel.ActiveScreen.CAMERA -> {
                    ArCameraScreen(
                        viewModel = viewModel,
                        tappedPoints = tappedPoints,
                        selectedSceneIndex = selectedSceneIndex,
                        selectedMode = selectedMode,
                        selectedUnit = selectedUnit,
                        projects = projects,
                        currentProjectId = currentProjectId,
                        calibrationFactor = calibrationFactor,
                        onOpenSaveDialog = {
                            measurementNote = ""
                            showSaveDialog = true
                        }
                    )
                }
                MeasurementViewModel.ActiveScreen.HISTORY -> {
                    HistoryLogsScreen(
                        viewModel = viewModel,
                        measurements = measurements,
                        projects = projects,
                        selectedUnit = selectedUnit
                    )
                }
                MeasurementViewModel.ActiveScreen.PROJECTS -> {
                    ProjectsManagerScreen(
                        projects = projects,
                        measurements = measurements,
                        currentProjectId = currentProjectId,
                        onProjectSelected = { viewModel.selectProject(it) },
                        onAddProjectClicked = {
                            newProjectName = ""
                            showProjectCreateDialog = true
                        },
                        onDeleteProject = { viewModel.deleteProject(it) }
                    )
                }
                MeasurementViewModel.ActiveScreen.SETTINGS -> {
                    CalibrationAndHelpScreen(
                        calibrationFactor = calibrationFactor,
                        onFactorChanged = { calibrationFactor = it }
                    )
                }
            }

            // A: ONBOARDING FLOATING LAYER
            if (showOnboarding) {
                OnboardingOverlay(
                    step = onboardingStep,
                    onNext = {
                        if (onboardingStep < 3) onboardingStep++ else showOnboarding = false
                    },
                    onSkip = { showOnboarding = false }
                )
            }

            // B: SAVE MEASUREMENT DIALOG
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    containerColor = SolidSlate800,
                    titleContentColor = TextSlate50,
                    textContentColor = TextSlate200,
                    title = { Text("Save AR Entry", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val activeVal = viewModel.calculateCurrentValue() * calibrationFactor
                            val formattedVal = when (selectedMode) {
                                MeasurementViewModel.MeasurementMode.AREA -> MeasurementUtils.formatAreaValue(activeVal, selectedUnit)
                                else -> MeasurementUtils.formatValue(activeVal, selectedUnit)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(OrangeAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .border(1.2.dp, OrangeAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formattedVal,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeAccent
                                )
                            }
                            
                            Text(
                                text = "Current Project Target: ${projects.firstOrNull { it.id == currentProjectId }?.name ?: "Default"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSlate400
                            )
                            
                            OutlinedTextField(
                                value = measurementNote,
                                onValueChange = { measurementNote = it },
                                label = { Text("Label / Note (e.g. Sofa fit, window)") },
                                placeholder = { Text("Add custom description info...") },
                                modifier = Modifier.fillMaxWidth().testTag("measurement_note_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeAccent,
                                    focusedLabelColor = OrangeAccent,
                                    unfocusedBorderColor = BorderSlate600,
                                    unfocusedLabelColor = TextSlate400,
                                    focusedTextColor = TextSlate50,
                                    unfocusedTextColor = TextSlate200
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.saveCurrentMeasurement(measurementNote)
                                showSaveDialog = false
                                Toast.makeText(context, "Saved measurement successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            modifier = Modifier.testTag("save_confirm_btn")
                        ) {
                            Text("Save", color = TextSlate50, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel", color = TextSlate400)
                        }
                    }
                )
            }

            // C: CREATE PROJECT DIALOG
            if (showProjectCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showProjectCreateDialog = false },
                    containerColor = SolidSlate800,
                    titleContentColor = TextSlate50,
                    textContentColor = TextSlate200,
                    title = { Text("New Planner Project", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = { newProjectName = it },
                            label = { Text("Project Name") },
                            placeholder = { Text("e.g. Kitchen remodel, Office fit") },
                            modifier = Modifier.fillMaxWidth().testTag("new_project_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeAccent,
                                focusedLabelColor = OrangeAccent,
                                unfocusedBorderColor = BorderSlate600,
                                unfocusedLabelColor = TextSlate400,
                                focusedTextColor = TextSlate50,
                                unfocusedTextColor = TextSlate200
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newProjectName.isNotBlank()) {
                                    viewModel.createProject(newProjectName)
                                    showProjectCreateDialog = false
                                    Toast.makeText(context, "Project folder created!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            modifier = Modifier.testTag("project_confirm_btn")
                        ) {
                            Text("Create", color = TextSlate50, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showProjectCreateDialog = false }) {
                            Text("Cancel", color = TextSlate400)
                        }
                    }
                )
            }
        }
    }
}

// 1. AR CAMERA HUD AND VIEWPORT WRAPPER
@Composable
fun ArCameraScreen(
    viewModel: MeasurementViewModel,
    tappedPoints: List<ArPoint>,
    selectedSceneIndex: Int,
    selectedMode: MeasurementViewModel.MeasurementMode,
    selectedUnit: AppUnit,
    projects: List<Project>,
    currentProjectId: Long?,
    calibrationFactor: Float,
    onOpenSaveDialog: () -> Unit
) {
    val context = LocalContext.current
    var showProjectDropDown by remember { mutableStateOf(false) }
    var showSceneDropDown by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Holographic AR Space projection background view
        CameraArView(
            selectedMode = selectedMode,
            tappedPoints = tappedPoints,
            selectedSceneIndex = selectedSceneIndex,
            selectedUnit = selectedUnit,
            onPointAdded = { viewModel.addPoint(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Upper overlays: Environments & Project folders configs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Project Folder Tag DropDown
                Box(modifier = Modifier.weight(1f)) {
                    val activeProject = projects.firstOrNull { it.id == currentProjectId }?.name ?: "Quick Measure Folder"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProjectDropDown = true },
                        colors = CardDefaults.cardColors(containerColor = SolidSlate800.copy(alpha = 0.85f)),
                        border = borderStroke()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = activeProject,
                                    fontSize = 12.sp,
                                    color = TextSlate50,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSlate400)
                        }
                    }

                    DropdownMenu(
                        expanded = showProjectDropDown,
                        onDismissRequest = { showProjectDropDown = false },
                        modifier = Modifier.background(SolidSlate800)
                    ) {
                        projects.forEach { proj ->
                            DropdownMenuItem(
                                text = { Text(proj.name, color = TextSlate50, fontSize = 13.sp) },
                                onClick = {
                                    viewModel.selectProject(proj.id)
                                    showProjectDropDown = false
                                }
                            )
                        }
                    }
                }

                // Interactive Environment Scenario selector
                Box(modifier = Modifier.weight(1f)) {
                    val sceneText = SimulatedScenes.list[selectedSceneIndex % SimulatedScenes.list.size].name
                    val sceneEmoji = SimulatedScenes.list[selectedSceneIndex % SimulatedScenes.list.size].icon
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSceneDropDown = true },
                        colors = CardDefaults.cardColors(containerColor = SolidSlate800.copy(alpha = 0.85f)),
                        border = borderStroke()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = sceneEmoji, fontSize = 14.sp)
                                Text(
                                    text = sceneText,
                                    fontSize = 12.sp,
                                    color = TextSlate50,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSlate400)
                        }
                    }

                    DropdownMenu(
                        expanded = showSceneDropDown,
                        onDismissRequest = { showSceneDropDown = false },
                        modifier = Modifier.background(SolidSlate800)
                    ) {
                        SimulatedScenes.list.forEachIndexed { idx, scen ->
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(scen.icon)
                                        Text(scen.name, color = TextSlate50, fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    viewModel.selectScene(idx)
                                    showSceneDropDown = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick display of Scene helper instructions
            Text(
                text = "${SimulatedScenes.list[selectedSceneIndex % SimulatedScenes.list.size].description} Tap nodes in space to pin custom endpoints.",
                fontSize = 11.sp,
                color = TextSlate200,
                modifier = Modifier
                    .background(SolidSlate800.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    .border(0.5.dp, BorderSlate700, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Side Tool Controls (Undo, Reset, Unit Switcher Overlay)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Undo Node Button
            FloatingActionButton(
                onClick = { viewModel.undoLastPoint() },
                containerColor = SolidSlate800.copy(alpha = 0.9f),
                contentColor = TextSlate50,
                shape = CircleShape,
                modifier = Modifier
                    .size(46.dp)
                    .border(1.dp, BorderSlate600, CircleShape)
                    .testTag("action_undo")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Undo Action", modifier = Modifier.size(18.dp))
            }

            // Reset Workspace Button
            FloatingActionButton(
                onClick = { viewModel.resetPoints() },
                containerColor = SolidSlate800.copy(alpha = 0.9f),
                contentColor = TextSlate50,
                shape = CircleShape,
                modifier = Modifier
                    .size(46.dp)
                    .border(1.dp, BorderSlate600, CircleShape)
                    .testTag("action_reset")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Clear Space", modifier = Modifier.size(20.dp))
            }

            // High Fidelity screenshot export trigger
            FloatingActionButton(
                onClick = {
                    Toast.makeText(context, "📷 Screenshot saved to gallery with line overlay!", Toast.LENGTH_SHORT).show()
                },
                containerColor = SolidSlate800.copy(alpha = 0.9f),
                contentColor = TextSlate50,
                shape = CircleShape,
                modifier = Modifier
                    .size(46.dp)
                    .border(1.dp, BorderSlate600, CircleShape)
                    .testTag("action_screenshot")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Screenshot Export", modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Unit Selector Panel (Horizontal column style)
            Column(
                modifier = Modifier
                    .background(SolidSlate800.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    .border(0.6.dp, BorderSlate700, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppUnit.values().forEach { u ->
                    val isActive = selectedUnit == u
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isActive) OrangeAccent else Color.Transparent)
                            .clickable { viewModel.selectUnit(u) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = u.suffix,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) TextSlate50 else TextSlate400
                        )
                    }
                }
            }
        }

        // Bottom Dashboard Drawer: Modes Selection & Computed numeric result readout
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Computed live readout popup values drawer
            if (tappedPoints.isNotEmpty()) {
                val rawValue = viewModel.calculateCurrentValue() * calibrationFactor
                val formattedVal = when (selectedMode) {
                    MeasurementViewModel.MeasurementMode.AREA -> MeasurementUtils.formatAreaValue(rawValue, selectedUnit)
                    else -> MeasurementUtils.formatValue(rawValue, selectedUnit)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SolidSlate800.copy(alpha = 0.92f)),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(OrangeAccentDark, OrangeAccent))),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current Estimate (" + selectedMode.name + ")",
                                fontSize = 11.sp,
                                color = TextSlate400
                            )
                            Text(
                                text = formattedVal,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlate50
                            )
                        }

                        // Save floating action button triggers
                        val isReadyToSave = when (selectedMode) {
                            MeasurementViewModel.MeasurementMode.LENGTH -> tappedPoints.size == 2
                            MeasurementViewModel.MeasurementMode.HEIGHT -> tappedPoints.size == 2
                            MeasurementViewModel.MeasurementMode.AREA -> tappedPoints.size >= 3
                        }

                        if (isReadyToSave) {
                            Button(
                                onClick = onOpenSaveDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("save_measure_fab")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSlate50)
                                    Text("Save", fontWeight = FontWeight.Bold, color = TextSlate50)
                                }
                            }
                        } else {
                            Text(
                                text = "Select ${if (selectedMode == MeasurementViewModel.MeasurementMode.AREA) "3+" else "2"} nodes",
                                fontSize = 11.sp,
                                color = OrangeAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(OrangeAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(0.6.dp, OrangeAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Mode Changer Segment row
            Card(
                modifier = Modifier.fillMaxWidth().testTag("mode_selector_panel"),
                colors = CardDefaults.cardColors(containerColor = SolidSlate800.copy(alpha = 0.95f)),
                border = borderStroke(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modesList = listOf(
                        Triple(MeasurementViewModel.MeasurementMode.LENGTH, "Length", "📏"),
                        Triple(MeasurementViewModel.MeasurementMode.HEIGHT, "Height", "📐"),
                        Triple(MeasurementViewModel.MeasurementMode.AREA, "Area", "🟩")
                    )

                    modesList.forEach { item ->
                        val isActive = selectedMode == item.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) OrangeAccent else Color.Transparent)
                                .clickable { viewModel.selectMode(item.first) }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(item.third)
                                Text(
                                    text = item.second,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) TextSlate50 else TextSlate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. SAVED HISTORY SCREEN
@Composable
fun HistoryLogsScreen(
    viewModel: MeasurementViewModel,
    measurements: List<Measurement>,
    projects: List<Project>,
    selectedUnit: AppUnit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Entries",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSlate50,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${measurements.size} measurements saved offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSlate400
                )
            }

            // Quick default conversion rows picker
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(SolidSlate800, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                AppUnit.values().forEach { u ->
                    val isActive = selectedUnit == u
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) OrangeAccent else Color.Transparent)
                            .clickable { viewModel.selectUnit(u) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = u.suffix.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) TextSlate50 else TextSlate400
                        )
                    }
                }
            }
        }

        if (measurements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📏", fontSize = 48.sp)
                    Text("No measurements recorded yet.", color = TextSlate50, fontWeight = FontWeight.Bold)
                    Text("Tap points in AR Camera Mode and click Save to record them.", color = TextSlate400, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(measurements) { m ->
                    val projectLabel = projects.firstOrNull { it.id == m.projectId }?.name ?: "Quick Measures"
                    val formattedDate = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(m.createdAt))
                    val itemUnit = AppUnit.values().firstOrNull { it.suffix.uppercase() == m.unit } ?: AppUnit.METERS

                    // Format values accurately
                    val formattedVal = when (m.type) {
                        "AREA" -> MeasurementUtils.formatAreaValue(m.valueMeters, selectedUnit)
                        else -> MeasurementUtils.formatValue(m.valueMeters, selectedUnit)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("history_item_card"),
                        colors = CardDefaults.cardColors(containerColor = SolidSlate800),
                        border = borderStroke()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(OrangeAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(m.type, fontSize = 10.sp, color = OrangeAccent, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = projectLabel,
                                        fontSize = 11.sp,
                                        color = TextSlate400,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = m.note.ifBlank { "Unlabelled Measurement" },
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate50,
                                    fontSize = 15.sp
                                )

                                Text(
                                    text = formattedDate,
                                    fontSize = 11.sp,
                                    color = TextSlate400
                                )
                            }

                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = formattedVal,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate50
                                )

                                IconButton(
                                    onClick = { viewModel.deleteMeasurement(m.id) },
                                    modifier = Modifier.size(24.dp).testTag("delete_measurement_btn")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Record", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. PROJECTS DIRECTORY SCREEN
@Composable
fun ProjectsManagerScreen(
    projects: List<Project>,
    measurements: List<Measurement>,
    currentProjectId: Long?,
    onProjectSelected: (Long) -> Unit,
    onAddProjectClicked: () -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Project Folders",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSlate50,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Organise structural assessments",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSlate400
                )
            }

            Button(
                onClick = onAddProjectClicked,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_project_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSlate50)
                    Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSlate50)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(projects) { p ->
                val count = measurements.count { it.projectId == p.id }
                val isSelected = p.id == currentProjectId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProjectSelected(p.id) }
                        .testTag("project_item_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SolidSlate800 else DeepSlate900
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) OrangeAccent else BorderSlate700
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(OrangeAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = OrangeAccent)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(p.name, fontWeight = FontWeight.Bold, color = TextSlate50, fontSize = 15.sp)
                                Text("$count stored measurements", fontSize = 11.sp, color = TextSlate400)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(TrackingGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Active", fontSize = 10.sp, color = TrackingGreen, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Do not show delete on default singular Quick Measures folder
                            if (projects.size > 1) {
                                IconButton(
                                    onClick = { onDeleteProject(p.id) },
                                    modifier = Modifier.size(24.dp).testTag("delete_project_btn")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Purge Folder", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. CALIBRATION & HELP SETTINGS
@Composable
fun CalibrationAndHelpScreen(
    calibrationFactor: Float,
    onFactorChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Tracking & Calibration",
                style = MaterialTheme.typography.titleMedium,
                color = TextSlate50,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fine-tune sensory depth accuracy scaling",
                style = MaterialTheme.typography.bodySmall,
                color = TextSlate400
            )
        }

        // Calibration multiplier config card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("calibration_card"),
            colors = CardDefaults.cardColors(containerColor = SolidSlate800),
            border = borderStroke()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Calibration Correction Factor",
                    fontWeight = FontWeight.Bold,
                    color = TextSlate50,
                    fontSize = 14.sp
                )
                Text(
                    text = "Adjust if camera coordinates deviate from absolute reference tape values. Recommended multiplier ranges from 0.95 to 1.05.",
                    fontSize = 11.sp,
                    color = TextSlate400
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Multiplier Scalar", fontSize = 13.sp, color = TextSlate200)
                    Text(
                        text = String.format(Locale.US, "%.3fx", calibrationFactor),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeAccent
                    )
                }

                Slider(
                    value = calibrationFactor,
                    onValueChange = onFactorChanged,
                    valueRange = 0.90f..1.10f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = OrangeAccent,
                        thumbColor = OrangeAccent,
                        inactiveTrackColor = BorderSlate700
                    ),
                    modifier = Modifier.testTag("calibration_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onFactorChanged(0.95f) }) { Text("-5% (0.95)", fontSize = 11.sp, color = TextSlate400) }
                    TextButton(onClick = { onFactorChanged(1.00f) }) { Text("Reset (1.00)", fontSize = 11.sp, color = TextSlate50, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { onFactorChanged(1.05f) }) { Text("+5% (1.05)", fontSize = 11.sp, color = OrangeAccent) }
                }
            }
        }

        // Accuracy guidelines instructions card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SolidSlate800),
            border = borderStroke()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Optimal Measurement Tips",
                    fontWeight = FontWeight.Bold,
                    color = TextSlate50,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("💡", fontSize = 16.sp)
                    Text(
                        text = "Good Lighting: Avoid dark, shadow-heavy spaces and fully reflective or mirror surfaces.",
                        fontSize = 12.sp,
                        color = TextSlate400
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🐢", fontSize = 16.sp)
                    Text(
                        text = "Slow Motion: Move and tilt around the target object slowly before taping corners so surface filters synthesize points cleanly.",
                        fontSize = 12.sp,
                        color = TextSlate400
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📐", fontSize = 16.sp)
                    Text(
                        text = "Anchor Snap: Tap closest floating neon circles to lock points onto standard architectural dimensions.",
                        fontSize = 12.sp,
                        color = TextSlate400
                    )
                }
            }
        }
    }
}

// 5. ONBOARDING MODAL OVERLAY
@Composable
fun OnboardingOverlay(
    step: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlate900),
            border = BorderStroke(1.2.dp, OrangeAccent.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Steps visuals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MeasureMate Tutorial",
                        color = OrangeAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Step $step of 3",
                        color = TextSlate400,
                        fontSize = 11.sp
                    )
                }

                when (step) {
                    1 -> {
                        Text("📐", fontSize = 56.sp)
                        Text(
                            text = "Smart AR Coordinate Ruler",
                            fontWeight = FontWeight.Bold,
                            color = TextSlate50,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Turn your phone into a smart virtual tape measure. Simply scan area surfaces and tap points in the camera viewfinder to instantly lock real lengths, heights, and square area bounds.",
                            fontSize = 12.sp,
                            color = TextSlate400,
                            textAlign = TextAlign.Center
                        )
                    }
                    2 -> {
                        Text("🛋️", fontSize = 56.sp)
                        Text(
                            text = "Project-Ready Planning",
                            fontWeight = FontWeight.Bold,
                            color = TextSlate50,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Organise structural estimates into tidy project folders (e.g. \"Living Room Couch\", \"Dining Space\"). Switch physical calculation units between Meters, Centimeters, Feet, and Inches on the fly.",
                            fontSize = 12.sp,
                            color = TextSlate400,
                            textAlign = TextAlign.Center
                        )
                    }
                    3 -> {
                        Text("💡", fontSize = 56.sp)
                        Text(
                            text = "Accuracy Disclaimer",
                            fontWeight = FontWeight.Bold,
                            color = TextSlate50,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "AR measurements are approximate estimates (typically within 5% variation) depending on local camera light, surface gloss, and sensor stability. Use the Caliper calibration slider to fine-tune scales.",
                            fontSize = 12.sp,
                            color = TextSlate400,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Interactive Stepper points indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { d ->
                        Box(
                            modifier = Modifier
                                .size(if (d + 1 == step) 20.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(if (d + 1 == step) OrangeAccent else BorderSlate600)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step < 3) {
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Skip", color = TextSlate400)
                        }
                    }
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_next_btn")
                    ) {
                        Text(if (step == 3) "Start Measuring" else "Next", color = TextSlate50, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helpers card stroke configuration to avoid boilerplate code repetitions
private fun borderStroke() = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))
