package com.example.ui.measurement

import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class AppUnit(val label: String, val suffix: String) {
    METERS("Meters", "m"),
    CENTIMETERS("Centimeters", "cm"),
    FEET("Feet", "ft"),
    INCHES("Inches", "in")
}

object MeasurementUtils {
    // Converts value in meters to target unit
    fun convertFromMeters(meters: Double, targetUnit: AppUnit): Double {
        return when (targetUnit) {
            AppUnit.METERS -> meters
            AppUnit.CENTIMETERS -> meters * 100.0
            AppUnit.FEET -> meters * 3.28084
            AppUnit.INCHES -> meters * 39.3701
        }
    }

    // Formats any meter value elegantly
    fun formatValue(meters: Double, unit: AppUnit): String {
        val converted = convertFromMeters(meters, unit)
        return when (unit) {
            AppUnit.METERS -> String.format(Locale.US, "%.2f m", converted)
            AppUnit.CENTIMETERS -> String.format(Locale.US, "%.1f cm", converted)
            AppUnit.FEET -> {
                val totalInches = meters * 39.3701
                val ft = (totalInches / 12).toInt()
                val inch = (totalInches % 12).roundToInt()
                if (ft > 0) "$ft' $inch\"" else "$inch\""
            }
            AppUnit.INCHES -> String.format(Locale.US, "%.1f in", converted)
        }
    }

    // Formats square area values
    fun formatAreaValue(squareMeters: Double, unit: AppUnit): String {
        return when (unit) {
            AppUnit.METERS -> String.format(Locale.US, "%.2f m²", squareMeters)
            AppUnit.CENTIMETERS -> String.format(Locale.US, "%.0f cm²", squareMeters * 10000.0)
            AppUnit.FEET -> String.format(Locale.US, "%.2f ft²", squareMeters * 10.7639)
            AppUnit.INCHES -> String.format(Locale.US, "%.1f in²", squareMeters * 1550.0)
        }
    }

    // 3D Distance formula
    fun calculateDistance3D(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float
    ): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    // Calculates area of a 2D polygon using Shoelace formula in square units
    fun calculateArea2D(points: List<Pair<Float, Float>>): Double {
        if (points.size < 3) return 0.0
        var area = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].first * points[j].second
            area -= points[j].first * points[i].second
        }
        return Math.abs(area) / 2.0
    }
}
