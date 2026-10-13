package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.time.TimeChangeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

/** [change]로 시각·시간대가 바뀐 척한다. [subscribers]로 지금 듣고 있는 곳이 있는지 확인한다. */
class FakeTimeChangeSource : TimeChangeSource {
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val changes: Flow<Unit> = events

    val subscribers: StateFlow<Int> = events.subscriptionCount

    fun change() {
        events.tryEmit(Unit)
    }
}
