package com.vibedev.app.gyms

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves every successful search on the phone. If a later search fails,
 * returns the saved results for that area instead.
 */
class CachingGymSource(
    private val delegate: GymSource,
    context: Context,
) : GymSource {
    private val prefs = context.getSharedPreferences("gym_cache", Context.MODE_PRIVATE)

    /** True when the last answer came from the saved copy. */
    var lastWasCached = false
        private set

    override suspend fun findGyms(area: SearchArea): List<Gym> {
        val key = keyFor(area)
        return try {
            delegate.findGyms(area).also {
                lastWasCached = false
                prefs.edit().putString(key, encode(it)).apply()
            }
        } catch (e: Exception) {
            val saved = prefs.getString(key, null) ?: throw e
            lastWasCached = true
            decode(saved)
        }
    }

    /** Areas are cached by name; "Near me" also by a ~1 km grid cell. */
    private fun keyFor(area: SearchArea) =
        "${area.label}:%.2f,%.2f".format(java.util.Locale.US, area.center.lat, area.center.lon)

    private fun encode(gyms: List<Gym>) = JSONArray().apply {
        gyms.forEach {
            put(
                JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind.name)
                    .put("lat", it.location.lat).put("lon", it.location.lon),
            )
        }
    }.toString()

    private fun decode(json: String): List<Gym> {
        val a = JSONArray(json)
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Gym(
                id = o.getString("id"),
                name = o.getString("name"),
                kind = GymKind.valueOf(o.getString("kind")),
                location = GeoPoint(o.getDouble("lat"), o.getDouble("lon")),
            )
        }
    }
}
