package com.vibedev.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibedev.app.gyms.GeoPoint
import com.vibedev.app.gyms.Gym
import com.vibedev.app.gyms.GymSource
import com.vibedev.app.gyms.SearchArea

/** Holds what the gym finder screen shows. Knows nothing about Android views. */
class GymFinderState(private val source: GymSource) {
    var area by mutableStateOf<SearchArea?>(null); private set
    var myLocation by mutableStateOf<GeoPoint?>(null)
    var gyms by mutableStateOf<List<Gym>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var selected by mutableStateOf<Gym?>(null)

    /** Where distances are measured from: you, if known, else the area's center. */
    val origin: GeoPoint? get() = myLocation ?: area?.center

    val gymsByDistance: List<Gym>
        get() = origin?.let { o -> gyms.sortedBy { it.location.distanceMetersTo(o) } } ?: gyms

    suspend fun search(newArea: SearchArea) {
        area = newArea
        selected = null
        loading = true
        error = null
        try {
            gyms = source.findGyms(newArea)
        } catch (e: Exception) {
            gyms = emptyList()
            error = "Couldn't load gyms. Check your internet and try again."
        } finally {
            loading = false
        }
    }

    fun showError(message: String) {
        error = message
    }
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m" else "%.1f km".format(meters / 1000)
