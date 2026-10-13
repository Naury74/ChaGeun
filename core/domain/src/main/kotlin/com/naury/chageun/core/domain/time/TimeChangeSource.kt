package com.naury.chageun.core.domain.time

import kotlinx.coroutines.flow.Flow

/** 사용자가 기기 시각이나 시간대를 바꿨거나 날짜가 넘어갔을 때 알린다. 구독하는 동안만 듣는다. */
interface TimeChangeSource {
    val changes: Flow<Unit>
}
