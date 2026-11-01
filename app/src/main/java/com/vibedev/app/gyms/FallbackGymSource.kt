package com.vibedev.app.gyms

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Asks every source at once and returns the first answer. Fails only if all fail. */
class FallbackGymSource(private val sources: List<GymSource>) : GymSource {
    override suspend fun findGyms(area: SearchArea): List<Gym> = coroutineScope {
        val results = Channel<Result<List<Gym>>>(sources.size)
        val jobs = sources.map { source ->
            launch { results.send(runCatching { source.findGyms(area) }) }
        }
        val errors = mutableListOf<String>()
        repeat(sources.size) {
            val result = results.receive()
            result.onSuccess { gyms ->
                jobs.forEach { it.cancel() }
                return@coroutineScope gyms
            }
            result.onFailure { errors += it.message ?: it.javaClass.simpleName }
        }
        throw IllegalStateException(errors.joinToString("; "))
    }
}
