package com.vibedev.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.vibedev.app.gyms.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Gets the phone's current position. Caller must already hold location permission. */
class LocationFinder(context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = context.mainExecutor

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint? {
        val providers = manager.getProviders(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstOrNull { it in providers }
            if (provider != null) {
                val fresh = withTimeoutOrNull(15_000) {
                    suspendCancellableCoroutine { cont ->
                        manager.getCurrentLocation(provider, null, executor) { cont.resume(it) }
                    }
                }
                if (fresh != null) return GeoPoint(fresh.latitude, fresh.longitude)
            }
        }
        return providers
            .mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { GeoPoint(it.latitude, it.longitude) }
    }
}
