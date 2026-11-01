package com.vibedev.app.gyms

/** Anything that can find gyms in an area. */
fun interface GymSource {
    suspend fun findGyms(area: SearchArea): List<Gym>
}
