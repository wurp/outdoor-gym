package com.vibedev.app.gyms

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Keeps the last results for each area on the phone. */
interface GymCache {
    fun load(area: SearchArea): List<Gym>?
    fun save(area: SearchArea, gyms: List<Gym>)
}

class SharedPrefsGymCache(context: Context) : GymCache {
    private val prefs = context.getSharedPreferences("gym_cache", Context.MODE_PRIVATE)

    override fun load(area: SearchArea): List<Gym>? =
        prefs.getString(keyFor(area), null)?.let { runCatching { decode(it) }.getOrNull() }

    override fun save(area: SearchArea, gyms: List<Gym>) {
        prefs.edit().putString(keyFor(area), encode(gyms)).apply()
    }

    /** Areas are keyed by name and center (to ~1 km), so "Near me" caches per place. */
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
