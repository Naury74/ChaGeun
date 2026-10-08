package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.flags.FeatureFlagRepository
import com.naury.chageun.core.model.FeatureFlags
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFeatureFlagRepository(initial: FeatureFlags = FeatureFlags()) : FeatureFlagRepository {
    override val flags = MutableStateFlow(initial)
}
