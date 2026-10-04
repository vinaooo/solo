package io.github.vinaooo.solo.core.ads

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
import javax.inject.Inject

/**
 * An adaptive AdMob banner capped at [MAX_HEIGHT_DP]: full width on a phone in portrait; in landscape, where height is
 * short, or on a tablet, at most [MAX_WIDTH_DP] wide and [LANDSCAPE_MAX_HEIGHT_DP] tall in landscape. Its full-width
 * slot always takes that height, even before consent or while no ad is loaded, so the board above never jumps; a
 * smaller ad sits centered in it.
 */
class AdMobBanner @Inject constructor(private val config: AdsConfig, private val consent: AdConsent) :
    AdBannerProvider {
    @Composable
    override fun Banner(modifier: Modifier) {
        val state by consent.state.collectAsStateWithLifecycle()
        BoxWithConstraints(modifier.fillMaxWidth().testTag(AdBannerProvider.TEST_TAG)) {
            val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val fullWidth = maxWidth.value.toInt()
            val capped = landscape || fullWidth >= TABLET_WIDTH_DP
            val widthDp = if (capped) fullWidth.coerceAtMost(MAX_WIDTH_DP) else fullWidth
            val heightDp = if (landscape) LANDSCAPE_MAX_HEIGHT_DP else MAX_HEIGHT_DP
            // The anchored sizes are about 130dp tall on a phone; an inline adaptive size takes a height cap.
            val size = remember(widthDp, heightDp) { AdSize.getInlineAdaptiveBannerAdSize(widthDp, heightDp) }
            Surface(
                // The game's table color, so the slot reads as part of the board rather than a separate strip.
                color = SoloThemeExtras.cardColors.table,
                modifier = Modifier.fillMaxWidth().height(heightDp.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // A new size (rotation, window resize) needs a new ad view sized for it.
                    if (state.canRequestAds) key(size) { BannerView(config.bannerUnitId, size, widthDp.dp) }
                }
            }
        }
    }
}

@Composable
private fun BannerView(adUnitId: String, size: AdSize, width: Dp) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val adView = remember {
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(size)
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }
    AndroidView(factory = { adView }, modifier = Modifier.width(width))
}

/** Height cap of the banner, chosen with the user: a classic phone banner height. */
internal const val MAX_HEIGHT_DP = 60

/** Height cap in landscape: the standard banner's height, leaving the board as much as it can. */
internal const val LANDSCAPE_MAX_HEIGHT_DP = 50

/** Width cap, the standard banner's, chosen with the user: wider ads looked too big on a tablet. */
internal const val MAX_WIDTH_DP = 320

/** From this width (Material's medium window), a portrait screen is a tablet's and its banner is capped too. */
internal const val TABLET_WIDTH_DP = 600
