package com.naury.chageun.data.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.naury.chageun.core.domain.weather.ApproximateLocationProvider
import com.naury.chageun.core.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 플랫폼 LocationManager로 대략적인 위치만 구한다. Play 서비스 위치 API는 쓰지 않는다.
 * 최근 한 시간 안의 마지막 위치가 있으면 그걸 쓰고, 없을 때만 새로 한 번 묻는다.
 */
internal class PlatformApproximateLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) : ApproximateLocationProvider {

    private val locationManager: LocationManager? = context.getSystemService(LocationManager::class.java)

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override suspend fun currentLocation(): GeoPoint? {
        val manager = locationManager ?: return null
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val location = try {
            recentLastKnown(manager) ?: freshLocation(manager)
        } catch (_: SecurityException) {
            // 권한 확인과 호출 사이에 사용자가 권한을 거둔 경우
            null
        }
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private fun recentLastKnown(manager: LocationManager): Location? {
        val oldest = clock.millis() - RECENT_LOCATION.toMillis()
        return providers(manager)
            .mapNotNull { manager.getLastKnownLocation(it) }
            .filter { it.time >= oldest }
            .maxByOrNull { it.time }
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private suspend fun freshLocation(manager: LocationManager): Location? {
        val provider = providers(manager).firstOrNull() ?: return null
        return withTimeoutOrNull(FRESH_TIMEOUT.toMillis()) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                ) { location -> if (continuation.isActive) continuation.resume(location) }
            }
        }
    }

    /** 대략적인 위치를 줄 수 있는 켜진 제공자. 네트워크 위치를 먼저 본다. */
    private fun providers(manager: LocationManager): List<String> {
        val candidates = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        return candidates.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    }

    private companion object {
        val RECENT_LOCATION: Duration = Duration.ofHours(1)
        val FRESH_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
