package com.silicio.copi

import com.silicio.copi.data.GeoPoint
import com.silicio.copi.core.RoutePath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePathTest {
    @Test
    fun `interpola a mitad de segmento`() {
        val path = RoutePath(listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.001)))
        val mid = path.pointAt(path.totalM / 2)
        assertNotNull(mid)
        assertTrue(mid!!.first.lon in 0.0004..0.0006)
        assertEquals(90.0, mid.second, 1.0)
    }

    @Test
    fun `encaja un punto cercano en la ruta`() {
        val path = RoutePath(
            listOf(
                GeoPoint(40.0, -3.0),
                GeoPoint(40.0, -2.99),
                GeoPoint(40.001, -2.99)
            )
        )
        val nearest = path.nearest(40.0002, -2.9901)
        assertNotNull(nearest)
        assertTrue(nearest!!.first > 0)
        assertTrue(nearest.second < 100.0)
    }
}
