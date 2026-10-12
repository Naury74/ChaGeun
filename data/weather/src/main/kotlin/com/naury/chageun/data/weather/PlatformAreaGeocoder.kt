package com.naury.chageun.data.weather

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Duration
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal interface AreaGeocoder {
    /** 찾지 못하면 null이다. [point]는 이미 반올림한 값이어야 한다. */
    suspend fun lookup(point: GeoPoint, locale: Locale): AreaAddress?
}

/**
 * 기기의 [Geocoder]로 동네 이름을 찾는다. 지오코딩은 기기 쪽 서비스(대개 Google Play 서비스)가 처리하며
 * 반올림한 좌표가 그 서비스로 전달될 수 있다. 지오코더가 없는 기기(일부 AOSP·중국 기기)에서는 찾지 않는다.
 */
internal class PlatformAreaGeocoder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AreaGeocoder {

    override suspend fun lookup(point: GeoPoint, locale: Locale): AreaAddress? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, locale)
        val address = withTimeoutOrNull(TIMEOUT.toMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lookupAsync(geocoder, point)
                } else {
                    lookupBlocking(geocoder, point)
                }
            } catch (_: IOException) {
                // 네트워크가 없거나 서비스가 응답하지 않는다.
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        return address?.toAreaAddress()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun lookupAsync(geocoder: Geocoder, point: GeoPoint): Address? =
        suspendCancellableCoroutine { continuation ->
            val listener = object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                }

                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
            geocoder.getFromLocation(point.latitude, point.longitude, 1, listener)
        }

    // API 33 아래에는 비동기 호출이 없어 IO 스레드에서 기다린다.
    private suspend fun lookupBlocking(geocoder: Geocoder, point: GeoPoint): Address? = withContext(ioDispatcher) {
        geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()
    }

    private fun Address.toAreaAddress() = AreaAddress(
        countryCode = countryCode,
        adminArea = adminArea,
        subAdminArea = subAdminArea,
        locality = locality,
        subLocality = subLocality,
    )

    private companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(8)
    }
}
