package com.vibedev.app.gyms

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lon: Double) {
    fun distanceMetersTo(other: GeoPoint): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(other.lat - lat)
        val dLon = Math.toRadians(other.lon - lon)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

enum class GymKind(val label: String) {
    CALISTHENICS("Calisthenics park"),
    OUTDOOR_GYM("Outdoor gym"),
}

data class Gym(
    val id: String,
    val name: String,
    val kind: GymKind,
    val location: GeoPoint,
)

/** A place to search. Valencia and Seattle are built in as quick shortcuts. */
data class SearchArea(val label: String, val center: GeoPoint, val radiusMeters: Int) {
    companion object {
        val Valencia = SearchArea("Valencia", GeoPoint(39.4699, -0.3763), 8_000)
        val Seattle = SearchArea("Seattle", GeoPoint(47.6062, -122.3321), 12_000)
        fun nearMe(here: GeoPoint) = SearchArea("Near me", here, 5_000)
    }
}
