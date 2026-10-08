package com.naury.chageun.flags

import com.naury.chageun.core.domain.flags.FeatureFlagRepository
import com.naury.chageun.core.model.FeatureFlags
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * V1은 Worker가 없어 앱 기본값만 쓴다(ADR-007). Worker가 생기면 서명을 확인한 공개 설정을 받아
 * 이 값 위에 덮어쓰는 구현으로 바꾼다.
 */
@Singleton
class DefaultFeatureFlagRepository @Inject constructor() : FeatureFlagRepository {
    override val flags: Flow<FeatureFlags> = flowOf(FeatureFlags())
}
