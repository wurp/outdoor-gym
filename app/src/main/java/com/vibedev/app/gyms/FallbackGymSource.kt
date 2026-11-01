package com.vibedev.app.gyms

/** Tries each source in turn and returns the first answer. Fails only if all fail. */
class FallbackGymSource(private val sources: List<GymSource>) : GymSource {
    override suspend fun findGyms(area: SearchArea): List<Gym> {
        val errors = mutableListOf<String>()
        for (source in sources) {
            try {
                return source.findGyms(area)
            } catch (e: Exception) {
                errors += e.message ?: e.javaClass.simpleName
            }
        }
        throw IllegalStateException(errors.joinToString("; "))
    }
}
