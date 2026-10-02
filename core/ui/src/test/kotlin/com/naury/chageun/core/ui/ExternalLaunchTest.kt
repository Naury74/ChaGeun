package com.naury.chageun.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.compose.ui.platform.UriHandler
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class ExternalLaunchTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun missingActivity_showsNotice_insteadOfCrashing() {
        context.launchExternal { throw ActivityNotFoundException("no document picker") }
        ShadowLooper.idleMainLooper()

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("No app on this device can open this.")
    }

    @Test
    fun uriWithoutBrowser_showsNotice() {
        val handler = object : UriHandler {
            override fun openUri(uri: String) = throw IllegalArgumentException("Can't open $uri.")
        }

        handler.openUriSafely(context, "https://www.car.go.kr")
        ShadowLooper.idleMainLooper()

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("No app on this device can open this.")
    }

    @Test
    fun successfulLaunch_showsNothing() {
        var launched = false

        context.launchExternal { launched = true }

        assertThat(launched).isTrue()
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
    }
}
