package com.vibedev.app.gyms

/**
 * OpenStreetMap often maps each pull-up bar or bench as its own point.
 * Merge points that sit close together into one gym.
 */
object GymClusterer {
    private const val MERGE_METERS = 60.0

    fun merge(gyms: List<Gym>): List<Gym> {
        // Named and calisthenics entries go first so they represent their cluster.
        val ordered = gyms.sortedWith(
            compareByDescending<Gym> { it.name != it.kind.label }.thenBy { it.kind.ordinal },
        )
        val kept = mutableListOf<Gym>()
        for (gym in ordered) {
            if (kept.none { it.location.distanceMetersTo(gym.location) < MERGE_METERS }) kept += gym
        }
        return kept
    }
}
