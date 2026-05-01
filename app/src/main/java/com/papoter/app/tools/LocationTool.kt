package com.papoter.app.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

object LocationTool {
    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getLocation(context: Context): String = suspendCancellableCoroutine { cont ->
        if (!hasPermission(context)) {
            cont.resume("Location permission not granted.")
            return@suspendCancellableCoroutine
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000
        ).setWaitForAccurateLocation(false).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = result.lastLocation
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()
                        val place = address?.let {
                            listOfNotNull(it.locality, it.adminArea, it.countryName).joinToString(", ")
                        } ?: "${location.latitude}, ${location.longitude}"
                        cont.resume("User is currently near $place.")
                    } catch (e: Exception) {
                        cont.resume("User is at ${location.latitude}, ${location.longitude}.")
                    }
                } else {
                    cont.resume("Could not determine location.")
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            cont.resume("Location permission not granted.")
        }

        cont.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
