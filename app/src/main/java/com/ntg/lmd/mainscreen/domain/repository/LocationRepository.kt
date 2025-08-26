package com.ntg.lmd.mainscreen.domain.repository

import android.content.Context
import android.location.Location

interface LocationRepository {

    suspend fun getLastLocation(context: Context): Location?
    suspend fun getCurrentLocation(context: Context): Location?

}