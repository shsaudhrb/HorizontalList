package com.ntg.lmd.mainscreen.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ntg.lmd.mainscreen.domain.repository.LocationRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationRepositoryImpl : LocationRepository {
    private fun client(context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(context: Context): Location? {
        val c = client(context)
        return suspendCancellableCoroutine { cont ->
            c.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(context: Context): Location? {
        val c = client(context)
        return suspendCancellableCoroutine { cont ->
            c
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }
}
