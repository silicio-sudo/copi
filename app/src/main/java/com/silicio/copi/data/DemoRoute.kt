package com.silicio.copi.data

import com.silicio.copi.core.offsetPoint
import kotlin.math.ceil

/**
 * Ruta sintética para el tramo de demostración: rectas, curvas de distinto
 * radio y una horquilla. Permite probar la app (y Android Auto) sin coche.
 */
object DemoRoute {
    fun build(): List<GeoPoint> {
        val start = GeoPoint(40.84310, -3.94870)
        // (rumbo, longitud en metros)
        val legs = listOf(
            0.0 to 300.0,
            25.0 to 90.0,
            70.0 to 60.0,
            110.0 to 70.0,
            65.0 to 80.0,
            10.0 to 55.0,
            330.0 to 50.0,
            285.0 to 60.0,
            250.0 to 80.0,
            290.0 to 40.0,
            355.0 to 70.0,
            45.0 to 90.0,
            100.0 to 60.0,
            150.0 to 70.0,
            205.0 to 60.0,
            260.0 to 50.0,
            300.0 to 45.0,
            335.0 to 120.0,
            20.0 to 80.0,
            75.0 to 60.0,
            120.0 to 50.0,
            300.0 to 60.0,
            0.0 to 260.0
        )
        val points = mutableListOf(start)
        var current = start
        for ((bearing, length) in legs) {
            val steps = ceil(length / 20.0).toInt().coerceAtLeast(2)
            val step = length / steps
            repeat(steps) {
                current = offsetPoint(current, bearing, step)
                points.add(current)
            }
        }
        return points
    }

    fun roadbook(): Roadbook = PacenoteGenerator.fromRoute(
        points = build(),
        name = "Tramo demo · Puerto sintético",
        source = "demo",
        id = "demo-puerto",
        createdAt = 1_700_000_000_000L
    )
}
