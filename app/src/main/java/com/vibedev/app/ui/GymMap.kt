package com.vibedev.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.vibedev.app.gyms.GeoPoint
import com.vibedev.app.gyms.Gym
import com.vibedev.app.gyms.GymMark
import com.vibedev.app.gyms.SearchArea
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint as OsmPoint

/** OpenStreetMap view with a pin per gym. Tapping a pin calls [onGymTapped]. */
@Composable
fun GymMap(
    area: SearchArea?,
    gyms: List<Gym>,
    myLocation: GeoPoint?,
    markOf: (Gym) -> GymMark?,
    onGymTapped: (Gym) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> createMapView(context) },
        update = { map ->
            if (area != null && map.tag != area) {
                map.tag = area
                map.controller.setZoom(zoomFor(area.radiusMeters))
                map.controller.setCenter(area.center.toOsm())
            }
            map.overlays.clear()
            gyms.forEach { gym ->
                map.overlays += Marker(map).apply {
                    position = gym.location.toOsm()
                    title = gym.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    markerColor(markOf(gym))?.let { color ->
                        icon = icon.mutate().apply { setTint(color) }
                    }
                    setOnMarkerClickListener { _, _ -> onGymTapped(gym); true }
                }
            }
            myLocation?.let { here ->
                map.overlays += Marker(map).apply {
                    position = here.toOsm()
                    title = "You are here"
                    icon = map.context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
            }
            map.invalidate()
        },
        onRelease = { it.onDetach() },
    )
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
    }
}

/** Gold for favorites, grey for "do not use", default blue otherwise. */
private fun markerColor(mark: GymMark?): Int? = when (mark) {
    GymMark.FAVORITE -> 0xFFFFB300.toInt()
    GymMark.AVOID -> 0xFF9E9E9E.toInt()
    null -> null
}

private fun zoomFor(radiusMeters: Int): Double = when {
    radiusMeters <= 5_000 -> 14.0
    radiusMeters <= 9_000 -> 13.0
    else -> 12.0
}

private fun GeoPoint.toOsm() = OsmPoint(lat, lon)
