package io.github.vinaooo.solo.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import javax.inject.Inject

/** Stand-in until AdMob is integrated: reserves the banner space so layouts are final from day one. */
class PlaceholderAdBanner @Inject constructor() : AdBannerProvider {
    @Composable
    override fun Banner(modifier: Modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = modifier
                .fillMaxWidth()
                .height(AdBannerProvider.BANNER_HEIGHT_DP.dp)
                .testTag(AdBannerProvider.TEST_TAG),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.ad_placeholder),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
