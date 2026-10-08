package com.naury.chageun.core.domain.flags

import com.naury.chageun.core.model.FeatureFlags
import kotlinx.coroutines.flow.Flow

interface FeatureFlagRepository {
    val flags: Flow<FeatureFlags>
}
