package com.pheno.collector.util

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * GPS定位管理器 - 获取拍照时的经纬度信息
 */
object LocationProvider {
    private var fusedClient: FusedLocationProviderClient? = null
    private var cachedLocation: Location? = null

    fun init(context: Context) {
        fusedClient = LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * 获取当前单次定位（快速模式）
     */
    suspend fun getCurrentLocation(): Location? {
        return try {
            fusedClient?.lastLocation?.let { task ->
                val result = com.google.android.gms.tasks.Tasks.await(task)
                result?.let { cachedLocation = it }
                result
            }
        } catch (e: Exception) {
            cachedLocation
        }
    }

    /**
     * 获取缓存的最近一次定位
     */
    fun getCachedLocation(): Location? = cachedLocation

    /**
     * 持续定位Flow（高精度模式）
     */
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // 5秒更新间隔
        ).apply {
            setMinUpdateIntervalMillis(2000)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    cachedLocation = location
                    trySend(location)
                }
            }
        }

        try {
            fusedClient?.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // 权限不足
        }

        awaitClose {
            fusedClient?.removeLocationUpdates(callback)
        }
    }
}
