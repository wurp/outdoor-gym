package com.vibedev.app.gyms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Finds outdoor gyms and calisthenics parks in OpenStreetMap (free, no account). */
class OverpassGymSource(
    private val userAgent: String,
    private val endpoint: String = "https://overpass-api.de/api/interpreter",
) : GymSource {

    override suspend fun findGyms(area: SearchArea): List<Gym> = withContext(Dispatchers.IO) {
        parse(download(query(area)))
    }

    private fun query(area: SearchArea): String {
        val around = "(around:${area.radiusMeters},${area.center.lat},${area.center.lon})"
        return """
            [out:json][timeout:25];
            (
              nwr["leisure"="fitness_station"]$around;
              nwr["sport"="calisthenics"]$around;
              nwr["leisure"="outdoor_fitness"]$around;
            );
            out center 1000;
        """.trimIndent()
    }

    private fun download(query: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray()) }
            if (conn.responseCode != 200) error("Map server answered ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: String): List<Gym> {
        val elements = JSONObject(json).getJSONArray("elements")
        val gyms = (0 until elements.length()).mapNotNull { i ->
            val e = elements.getJSONObject(i)
            val point = e.optJSONObject("center")
                ?.let { GeoPoint(it.getDouble("lat"), it.getDouble("lon")) }
                ?: if (e.has("lat")) GeoPoint(e.getDouble("lat"), e.getDouble("lon")) else null
            point ?: return@mapNotNull null
            val tags = e.optJSONObject("tags") ?: JSONObject()
            val kind = if (tags.optString("sport").contains("calisthenics")) GymKind.CALISTHENICS else GymKind.OUTDOOR_GYM
            Gym(
                id = "${e.getString("type")}/${e.getLong("id")}",
                name = tags.optString("name").ifBlank { kind.label },
                kind = kind,
                location = point,
            )
        }
        return GymClusterer.merge(gyms)
    }
}
