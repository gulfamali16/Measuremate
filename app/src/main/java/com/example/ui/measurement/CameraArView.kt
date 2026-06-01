package com.example.ui.measurement

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

// Defines a mock interactive scene containing floating hot nodes
data class MeasurementScene(
    val name: String,
    val description: String,
    val icon: String,
    val nodes: List<ArPoint>,
    val wireframes: List<Pair<Int, Int>> // connection indexes for sketching lines
)

object SimulatedScenes {
    val list = listOf(
        MeasurementScene(
            name = "Stellar Sofa",
            description = "Standard 3-seater Nordic Sofa. Great for wall-fit tests.",
            icon = "🛋️",
            nodes = listOf(
                ArPoint(-1.10f, 0.0f, -0.4f, "Corner Back L"),
                ArPoint(1.10f, 0.0f, -0.4f, "Corner Back R"),
                ArPoint(-1.05f, 0.0f, 0.4f, "Front Base L"),
                ArPoint(1.05f, 0.0f, 0.4f, "Front Base R"),
                ArPoint(-1.10f, 0.85f, -0.4f, "Armrest Top Back L"),
                ArPoint(1.10f, 0.85f, -0.4f, "Armrest Top Back R"),
                ArPoint(-1.05f, 0.65f, 0.4f, "Armrest Top Front L"),
                ArPoint(1.05f, 0.65f, 0.4f, "Armrest Top Front R")
            ),
            wireframes = listOf(
                Pair(0, 1), Pair(2, 3), Pair(0, 2), Pair(1, 3), // Base square
                Pair(4, 5), Pair(6, 7), // Armrest horizontal bars
                Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7), // Vertical risers
                Pair(4, 6), Pair(5, 7) // Arm depth connections
            )
        ),
        MeasurementScene(
            name = "Oak Dining Table",
            description = "Solid wooden dinner table. Perfect for checking clearance.",
            icon = "🪑",
            nodes = listOf(
                ArPoint(-0.80f, 0.0f, -0.45f, "Leg Left Back Ground"),
                ArPoint(0.80f, 0.0f, -0.45f, "Leg Right Back Ground"),
                ArPoint(-0.80f, 0.0f, 0.45f, "Leg Left Front Ground"),
                ArPoint(0.80f, 0.0f, 0.45f, "Leg Right Front Ground"),
                ArPoint(-0.80f, 0.75f, -0.45f, "Tabletop Corner BL"),
                ArPoint(0.80f, 0.75f, -0.45f, "Tabletop Corner BR"),
                ArPoint(-0.80f, 0.75f, 0.45f, "Tabletop Corner FL"),
                ArPoint(0.80f, 0.75f, 0.45f, "Tabletop Corner FR")
            ),
            wireframes = listOf(
                Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7), // 4 legs
                Pair(4, 5), Pair(5, 7), Pair(7, 6), Pair(6, 4)  // Tabletop frame
            )
        ),
        MeasurementScene(
            name = "Picture Frame Accent",
            description = "Wall hanging portrait frame structure.",
            icon = "🖼️",
            nodes = listOf(
                ArPoint(-0.6f, 1.20f, 0.0f, "Frame Bottom L"),
                ArPoint(0.6f, 1.20f, 0.0f, "Frame Bottom R"),
                ArPoint(-0.6f, 2.00f, 0.0f, "Frame Top L"),
                ArPoint(0.6f, 2.00f, 0.0f, "Frame Top R"),
                ArPoint(0.0f, 1.60f, 0.0f, "Center Core Anchor")
            ),
            wireframes = listOf(
                Pair(0, 1), Pair(1, 3), Pair(3, 2), Pair(2, 0)
            )
        )
    )
}

@Composable
fun CameraArView(
    selectedMode: MeasurementViewModel.MeasurementMode,
    tappedPoints: List<ArPoint>,
    selectedSceneIndex: Int,
    selectedUnit: AppUnit,
    onPointAdded: (ArPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeScene = SimulatedScenes.list[selectedSceneIndex % SimulatedScenes.list.size]
    
    var isScanning by remember(selectedSceneIndex) { mutableStateOf(true) }
    LaunchedEffect(selectedSceneIndex) {
        isScanning = true
        delay(1300)
        isScanning = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "surface_pulse")
    val gridOpacity by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "grid_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D121B))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val centerX = widthPx / 2f
            val centerY = heightPx / 2f + 40f
            val scale = widthPx / 3.0f

            fun project3D(point: ArPoint): Offset {
                val projX = centerX + point.x * scale - point.z * scale * 0.40f
                val projY = centerY - point.y * scale + point.z * scale * 0.25f
                return Offset(projX, projY)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedMode, activeScene) {
                        detectTapGestures { tapOffset ->
                            if (isScanning) return@detectTapGestures

                            var bestDistance = Float.MAX_VALUE
                            var closestNode: ArPoint? = null

                            for (node in activeScene.nodes) {
                                val proj = project3D(node)
                                val d = sqrt((tapOffset.x - proj.x).pow(2) + (tapOffset.y - proj.y).pow(2))
                                if (d < 45.dp.toPx() && d < bestDistance) {
                                    bestDistance = d
                                    closestNode = node
                                }
                            }

                            if (closestNode != null) {
                                onPointAdded(closestNode)
                            } else {
                                val customX = (tapOffset.x - centerX) / scale
                                val customY = -(tapOffset.y - centerY) / scale
                                onPointAdded(ArPoint(customX, customY, 0f, "Custom Tap Point"))
                            }
                        }
                    }
            ) {
                val lineCount = 14
                
                for (i in -lineCount..lineCount) {
                    val pLeftStart = project3D(ArPoint(-2.5f, 0.0f, i * 0.25f))
                    val pLeftEnd = project3D(ArPoint(2.5f, 0.0f, i * 0.25f))
                    drawLine(
                        color = Color(0xFFE05A10).copy(alpha = if (isScanning) 0.08f else gridOpacity * 0.7f),
                        start = pLeftStart,
                        end = pLeftEnd,
                        strokeWidth = 1f
                    )

                    val pDepthStart = project3D(ArPoint(i * 0.25f, 0.0f, -2.5f))
                    val pDepthEnd = project3D(ArPoint(i * 0.25f, 0.0f, 2.5f))
                    drawLine(
                        color = Color(0xFFE05A10).copy(alpha = if (isScanning) 0.08f else gridOpacity * 0.7f),
                        start = pDepthStart,
                        end = pDepthEnd,
                        strokeWidth = 1f
                    )
                }

                if (!isScanning) {
                    activeScene.wireframes.forEach { link ->
                        if (link.first < activeScene.nodes.size && link.second < activeScene.nodes.size) {
                            val startProj = project3D(activeScene.nodes[link.first])
                            val endProj = project3D(activeScene.nodes[link.second])
                            drawLine(
                                color = Color(0xFFF97316).copy(alpha = 0.35f),
                                start = startProj,
                                end = endProj,
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        }
                    }

                    if (tappedPoints.isNotEmpty()) {
                        val projectedPoints = tappedPoints.map { project3D(it) }

                        if (selectedMode == MeasurementViewModel.MeasurementMode.AREA && projectedPoints.size >= 3) {
                            val areaPath = Path().apply {
                                moveTo(projectedPoints[0].x, projectedPoints[0].y)
                                for (k in 1 until projectedPoints.size) {
                                    lineTo(projectedPoints[k].x, projectedPoints[k].y)
                                }
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                color = Color(0xFFF97316).copy(alpha = 0.22f)
                            )
                        }

                        for (idx in 0 until projectedPoints.size - 1) {
                            drawLine(
                                color = Color(0xFFEA580C),
                                start = projectedPoints[idx],
                                end = projectedPoints[idx + 1],
                                strokeWidth = 3.dp.toPx()
                            )

                            drawCircle(
                                color = Color.White,
                                center = projectedPoints[idx],
                                radius = 4.dp.toPx()
                            )
                            drawCircle(
                                color = Color(0xFFEA580C),
                                center = projectedPoints[idx],
                                radius = 7.dp.toPx(),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        if (selectedMode == MeasurementViewModel.MeasurementMode.AREA && projectedPoints.size >= 3) {
                            drawLine(
                                color = Color(0xFFEA580C),
                                start = projectedPoints.last(),
                                end = projectedPoints.first(),
                                strokeWidth = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        }

                        drawCircle(
                            color = Color.White,
                            center = projectedPoints.last(),
                            radius = 4.dp.toPx()
                        )
                        drawCircle(
                            color = Color(0xFFEA580C),
                            center = projectedPoints.last(),
                            radius = 7.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    activeScene.nodes.forEach { node ->
                        val nodeProj = project3D(node)
                        val isTapped = tappedPoints.contains(node)

                        if (!isTapped) {
                            drawCircle(
                                color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                center = nodeProj,
                                radius = 6.dp.toPx()
                            )
                            drawCircle(
                                color = Color(0xFFF97316),
                                center = nodeProj,
                                radius = 10.dp.toPx(),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }

            if (isScanning) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.5.dp, Color(0xFFEA580C).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val pDotsTransition = rememberInfiniteTransition(label = "pDots")
                            val pulsValue by pDotsTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                label = "dotsValue"
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFFEA580C).copy(alpha = pulsValue), CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning Area Surface...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Tilt and move slowly to improve tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                val projectedPoints = tappedPoints.map { project3D(it) }

                if (selectedMode == MeasurementViewModel.MeasurementMode.LENGTH && projectedPoints.size == 2) {
                    val midX = (projectedPoints[0].x + projectedPoints[1].x) / 2
                    val midY = (projectedPoints[0].y + projectedPoints[1].y) / 2
                    val distanceMeters = MeasurementUtils.calculateDistance3D(
                        tappedPoints[0].x, tappedPoints[0].y, tappedPoints[0].z,
                        tappedPoints[1].x, tappedPoints[1].y, tappedPoints[1].z
                    )

                    FloatingBadge(
                        text = MeasurementUtils.formatValue(distanceMeters, selectedUnit),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = with(LocalDensity.current) { (midX - 50.dp.toPx()).toDp() },
                                top = with(LocalDensity.current) { (midY - 26.dp.toPx()).toDp() }
                            )
                    )
                } else if (selectedMode == MeasurementViewModel.MeasurementMode.HEIGHT && projectedPoints.size == 2) {
                    val midX = (projectedPoints[0].x + projectedPoints[1].x) / 2
                    val midY = (projectedPoints[0].y + projectedPoints[1].y) / 2
                    val dy = abs(tappedPoints[1].y - tappedPoints[0].y).toDouble()

                    FloatingBadge(
                        text = "Height: " + MeasurementUtils.formatValue(dy, selectedUnit),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = with(LocalDensity.current) { (midX - 60.dp.toPx()).toDp() },
                                top = with(LocalDensity.current) { (midY - 26.dp.toPx()).toDp() }
                            )
                    )
                } else if (selectedMode == MeasurementViewModel.MeasurementMode.AREA && projectedPoints.size >= 3) {
                    val avgX = projectedPoints.map { it.x }.average().toFloat()
                    val avgY = projectedPoints.map { it.y }.average().toFloat()
                    val areaPoints = tappedPoints.map { Pair(it.x, it.z) }
                    val calculatedArea = MeasurementUtils.calculateArea2D(areaPoints)

                    FloatingBadge(
                        text = "Area: " + MeasurementUtils.formatAreaValue(calculatedArea, selectedUnit),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = with(LocalDensity.current) { (avgX - 60.dp.toPx()).toDp() },
                                top = with(LocalDensity.current) { (avgY - 26.dp.toPx()).toDp() }
                            )
                    )
                }

                activeScene.nodes.forEach { node ->
                    val isTapped = tappedPoints.contains(node)
                    if (isTapped) {
                        val nodeProj = project3D(node)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(
                                    start = with(LocalDensity.current) { (nodeProj.x + 12.dp.toPx()).toDp() },
                                    top = with(LocalDensity.current) { (nodeProj.y - 12.dp.toPx()).toDp() }
                                )
                                .background(Color(0xE11E293B).copy(alpha = 0.8f), MaterialTheme.shapes.small)
                                .border(0.5.dp, Color(0xFFEA580C).copy(alpha = 0.4f), MaterialTheme.shapes.small)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = node.label,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(4.dp)
                            .background(Color(0xFFEA580C), CircleShape)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color(0x80000000), MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Approximate AR Measure",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun FloatingBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFEA580C), MaterialTheme.shapes.medium)
            .border(1.dp, Color.White.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
