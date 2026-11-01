package com.vibedev.app.gyms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.cos

/** Finds outdoor gyms and calisthenics parks in OpenStreetMap (free, no account). */
class OverpassGymSource(
    private val userAgent: String,
    private val endpoint: String,
) : GymSource {

    companion object {
        /** Public copies of the same OpenStreetMap database, tried in order. */
        val PUBLIC_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
        )
    }

    override suspend fun findGyms(area: SearchArea): List<Gym> = withContext(Dispatchers.IO) {
        try {
            parse(download(query(area)))
        } catch (e: Exception) {
            throw IllegalStateException("${URL(endpoint).host}: ${e.message ?: e.javaClass.simpleName}", e)
        }
    }

    /**
     * A box search on two narrow tags. Box searches are much faster than circle
     * searches, and a bare "sport=calisthenics" tag makes servers scan everything.
     */
    private fun query(area: SearchArea): String {
        val dLat = area.radiusMeters / 111_320.0
        val dLon = dLat / cos(Math.toRadians(area.center.lat))
        val bbox = "%.5f,%.5f,%.5f,%.5f".format(
            Locale.US,
            area.center.lat - dLat, area.center.lon - dLon,
            area.center.lat + dLat, area.center.lon + dLon,
        )
        return """
            [out:json][timeout:60][bbox:$bbox];
            (
              nwr["leisure"="fitness_station"];
              nwr["leisure"="pitch"]["sport"~"calisthenics|fitness"];
            );
            out center 2000;
        """.trimIndent()
    }

    private fun download(query: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 75_000
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray()) }
            if (conn.responseCode != 200) error("${URL(endpoint).host} answered ${conn.responseCode}")
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
            val kind = if (tags.optString("sport").contains("calisthenics") || tags.optString("leisure") == "pitch") GymKind.CALISTHENICS else GymKind.OUTDOOR_GYM
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
