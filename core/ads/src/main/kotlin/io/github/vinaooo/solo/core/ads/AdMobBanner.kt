package io.github.vinaooo.solo.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import javax.inject.Inject

/**
 * A full-width adaptive AdMob banner capped at [MAX_HEIGHT_DP]. Its slot always takes that height, even before
 * consent or while no ad is loaded, so the board above never jumps; a shorter ad sits centered in it.
 */
class AdMobBanner @Inject constructor(private val config: AdsConfig, private val consent: AdConsent) :
    AdBannerProvider {
    @Composable
    override fun Banner(modifier: Modifier) {
        val state by consent.state.collectAsStateWithLifecycle()
        BoxWithConstraints(modifier.fillMaxWidth().testTag(AdBannerProvider.TEST_TAG)) {
            val widthDp = maxWidth.value.toInt()
            // The anchored sizes are about 130dp tall on a phone; an inline adaptive size takes a height cap.
            val size = remember(widthDp) { AdSize.getInlineAdaptiveBannerAdSize(widthDp, MAX_HEIGHT_DP) }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().height(MAX_HEIGHT_DP.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // A new width (rotation, window resize) needs a new ad view sized for it.
                    if (state.canRequestAds) key(size) { BannerView(config.bannerUnitId, size) }
                }
            }
        }
    }
}

@Composable
private fun BannerView(adUnitId: String, size: AdSize) {
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
    AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
}

/** Height cap of the banner, chosen with the user: a classic phone banner height. */
internal const val MAX_HEIGHT_DP = 60
