package com.vibedev.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibedev.app.gyms.GeoPoint
import com.vibedev.app.gyms.Gym
import com.vibedev.app.gyms.GymMark
import com.vibedev.app.gyms.GymMarkStore
import com.vibedev.app.gyms.GymSource
import com.vibedev.app.gyms.MarkedGym
import com.vibedev.app.gyms.SearchArea

/** Holds what the gym finder screen shows. Knows nothing about Android views. */
class GymFinderState(
    private val source: GymSource,
    private val markStore: GymMarkStore,
) {
    var area by mutableStateOf<SearchArea?>(null); private set
    var myLocation by mutableStateOf<GeoPoint?>(null)
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var selected by mutableStateOf<Gym?>(null)
    var showAvoided by mutableStateOf(false)
    var favoritesOnly by mutableStateOf(false)

    private var found by mutableStateOf<List<Gym>>(emptyList())
    private var marks by mutableStateOf(markStore.load())

    fun markOf(gym: Gym): GymMark? = marks[gym.id]?.mark

    /** Where distances are measured from: you, if known, else the area's center. */
    val origin: GeoPoint? get() = myLocation ?: area?.center

    val avoidedCount: Int get() = allGyms().count { markOf(it) == GymMark.AVOID }

    /** Gyms to show: favorites first, then by distance. Hidden ones only if asked. */
    val visibleGyms: List<Gym>
        get() {
            val o = origin
            return allGyms()
                .filter { showAvoided || markOf(it) != GymMark.AVOID }
                .filter { !favoritesOnly || markOf(it) == GymMark.FAVORITE }
                .sortedWith(
                    compareBy<Gym> { markOf(it)?.let { m -> if (m == GymMark.FAVORITE) 0 else 2 } ?: 1 }
                        .thenBy { g -> o?.let { g.location.distanceMetersTo(it) } ?: 0.0 },
                )
        }

    /** Search results plus every favorite, so favorites are always on the map. */
    private fun allGyms(): List<Gym> {
        val ids = found.mapTo(HashSet()) { it.id }
        val extraFavorites = marks.values
            .filter { it.mark == GymMark.FAVORITE && it.gym.id !in ids }
            .map(MarkedGym::gym)
        return found + extraFavorites
    }

    fun toggle(gym: Gym, mark: GymMark) {
        val newMark = if (markOf(gym) == mark) null else mark
        markStore.save(gym, newMark)
        marks = markStore.load()
        if (newMark == GymMark.AVOID && !showAvoided) selected = null
    }

    suspend fun search(newArea: SearchArea) {
        area = newArea
        selected = null
        loading = true
        error = null
        try {
            found = source.findGyms(newArea)
        } catch (e: Exception) {
            found = emptyList()
            error = "Couldn't load gyms. Tap the area again to retry.\nDetails: ${e.message}"
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
