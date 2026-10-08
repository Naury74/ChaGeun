package com.naury.chageun.update

import com.google.android.play.core.install.model.UpdateAvailability
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForcedUpdatePolicyTest {

    @Test
    fun forcesOnlyHighPriorityUpdates() {
        assertThat(shouldForceUpdate(UpdateAvailability.UPDATE_AVAILABLE, FORCE_PRIORITY, true)).isTrue()
        assertThat(shouldForceUpdate(UpdateAvailability.UPDATE_AVAILABLE, FORCE_PRIORITY - 1, true)).isFalse()
        assertThat(shouldForceUpdate(UpdateAvailability.UPDATE_NOT_AVAILABLE, 5, true)).isFalse()
    }

    @Test
    fun skips_whenPlayDoesNotAllowImmediateUpdate() {
        assertThat(shouldForceUpdate(UpdateAvailability.UPDATE_AVAILABLE, 5, false)).isFalse()
    }

    @Test
    fun resumesAnUpdateTheUserLeft() {
        assertThat(shouldForceUpdate(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS, 0, false)).isTrue()
    }
}
