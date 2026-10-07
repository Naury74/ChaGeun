package com.naury.chageun.core.ads

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/** 광고 노출 자격(기획서 17.3)을 만족할 때만 true. MainActivity가 [AdGate] 결과로 제공한다. */
val LocalAdsEnabled = compositionLocalOf { false }

/**
 * 허용된 위치(기획서 17.2)에만 두는 Native 광고 한 칸.
 * 불러오는 동안이나 실패하면 아무것도 그리지 않아 빈 영역이나 Skeleton이 남지 않는다 (17.4).
 */
// AndroidView가 레이아웃 파라미터를 직접 지정하므로 inflate할 부모 View가 없다.
@SuppressLint("InflateParams")
@Composable
fun NativeAdSlot(modifier: Modifier = Modifier) {
    if (!LocalAdsEnabled.current) return
    val context = LocalContext.current
    val adUnitId = stringResource(R.string.admob_native_unit_id)
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var disposed = false
        AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad -> if (disposed) ad.destroy() else nativeAd = ad }
            .build()
            .loadAd(AdRequest.Builder().build())
        onDispose {
            disposed = true
            nativeAd?.destroy()
        }
    }
    val ad = nativeAd ?: return
    val contentColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        AndroidView(
            factory = {
                (LayoutInflater.from(it).inflate(R.layout.native_ad_card, null) as NativeAdView).apply {
                    // 광고는 View 쪽 초점 탐색에 따로 잡혀, Tab 이동이 화면 끝에서 맨 위로 돌아가지 못하고
                    // 광고 안을 맴돈다. 키보드 이동 순서에서만 빼고 터치와 TalkBack으로는 그대로 누를 수 있다.
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    isFocusable = false
                }
            },
            update = { view -> view.bind(ad, contentColor, subtleColor) },
        )
    }
}

private fun NativeAdView.bind(ad: NativeAd, contentColor: Int, subtleColor: Int) {
    val headline = findViewById<TextView>(R.id.ad_headline).apply {
        text = ad.headline
        setTextColor(contentColor)
    }
    findViewById<TextView>(R.id.ad_badge).setTextColor(subtleColor)
    val body = findViewById<TextView>(R.id.ad_body).apply {
        text = ad.body
        setTextColor(subtleColor)
        visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    val callToAction = findViewById<Button>(R.id.ad_call_to_action).apply {
        text = ad.callToAction
        visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    }
    val icon = findViewById<ImageView>(R.id.ad_icon).apply {
        setImageDrawable(ad.icon?.drawable)
        visibility = if (ad.icon == null) View.GONE else View.VISIBLE
    }
    headlineView = headline
    bodyView = body
    callToActionView = callToAction
    iconView = icon
    setNativeAd(ad)
}
