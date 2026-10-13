package com.naury.chageun.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.naury.chageun.core.domain.time.HeaderTimeBoundaries
import com.naury.chageun.core.domain.time.TimeChangeSource
import com.naury.chageun.core.model.GeoPoint
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

/**
 * 홈 머리말의 하늘과 인사말을 다시 고를 때를 알린다. 구독하자마자 한 번, 그 뒤로는 해 뜸·노을·해 짐,
 * 인사말 시간대, 자정마다 알리고, 사용자가 시계나 시간대를 바꾸면 바로 알린다. 내는 값은 알린 시각이다.
 * 구독하는 동안만 기다리므로 홈이 보이지 않으면 멈춘다.
 */
class HomeClockTicks @Inject constructor(private val clock: Clock, private val timeChanges: TimeChangeSource) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun at(point: GeoPoint): Flow<Instant> =
        timeChanges.changes.onStart { emit(Unit) }.flatMapLatest { eachBoundary(point) }

    // 시계를 바꾸면 기다리던 시간이 틀어지므로 위에서 이 흐름을 새로 시작한다.
    private fun eachBoundary(point: GeoPoint): Flow<Instant> = flow {
        while (true) {
            emit(clock.instant())
            val wait = Duration.between(clock.instant(), HeaderTimeBoundaries.next(point, clock))
            delay(wait.toMillis().coerceAtLeast(0) + MARGIN_MILLIS)
        }
    }

    private companion object {
        // 경계 바로 앞에서 깨어 같은 장면을 다시 고르지 않도록 조금 늦게 깬다.
        const val MARGIN_MILLIS = 1_000L
    }
}

/** 기기 시각·시간대·날짜가 바뀌었다는 시스템 방송. 구독하는 동안만 리시버를 등록한다. */
internal class SystemTimeChangeSource @Inject constructor(@ApplicationContext private val context: Context) :
    TimeChangeSource {

    override val changes: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply { TIME_ACTIONS.forEach(::addAction) }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private companion object {
        val TIME_ACTIONS =
            listOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface HomeClockModule {
    @Binds
    fun bindTimeChangeSource(source: SystemTimeChangeSource): TimeChangeSource
}
