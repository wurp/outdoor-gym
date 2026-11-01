package com.vibedev.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vibedev.app.gyms.Gym

/** Opens Google Maps with directions to a gym. */
object MapsLauncher {
    fun navigateTo(context: Context, gym: Gym) {
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${gym.location.lat},${gym.location.lon}",
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
