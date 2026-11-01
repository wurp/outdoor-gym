package com.vibedev.app.gyms

import android.content.Context
import org.json.JSONObject

enum class GymMark { FAVORITE, AVOID }

data class MarkedGym(val gym: Gym, val mark: GymMark)

/** Remembers which gyms you've marked. */
interface GymMarkStore {
    fun load(): Map<String, MarkedGym>
    fun save(gym: Gym, mark: GymMark?)
}

/** Keeps marks on the phone, including each gym's name and position so favorites show anywhere. */
class SharedPrefsGymMarkStore(context: Context) : GymMarkStore {
    private val prefs = context.getSharedPreferences("gym_marks", Context.MODE_PRIVATE)

    override fun load(): Map<String, MarkedGym> =
        prefs.all.mapNotNull { (id, value) ->
            runCatching { decode(id, JSONObject(value as String)) }.getOrNull()?.let { id to it }
        }.toMap()

    override fun save(gym: Gym, mark: GymMark?) {
        prefs.edit().apply {
            if (mark == null) remove(gym.id) else putString(gym.id, encode(MarkedGym(gym, mark)))
        }.apply()
    }

    private fun encode(m: MarkedGym) = JSONObject()
        .put("name", m.gym.name)
        .put("kind", m.gym.kind.name)
        .put("lat", m.gym.location.lat)
        .put("lon", m.gym.location.lon)
        .put("mark", m.mark.name)
        .toString()

    private fun decode(id: String, o: JSONObject) = MarkedGym(
        gym = Gym(
            id = id,
            name = o.getString("name"),
            kind = GymKind.valueOf(o.getString("kind")),
            location = GeoPoint(o.getDouble("lat"), o.getDouble("lon")),
        ),
        mark = GymMark.valueOf(o.getString("mark")),
    )
}
