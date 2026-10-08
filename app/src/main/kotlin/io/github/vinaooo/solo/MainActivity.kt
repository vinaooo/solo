package io.github.vinaooo.solo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.core.designsystem.theme.isDarkTheme
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.github.vinaooo.vinkit.ads.AdConsent
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var adBanner: AdBannerProvider

    @Inject lateinit var adConsent: AdConsent

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Once per launch, not again when the activity is recreated (rotation, theme change).
        if (savedInstanceState == null) adConsent.gather(this)
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = Settings())
            val darkTheme = isDarkTheme(settings.themeMode, isSystemInDarkTheme())
            LaunchedEffect(darkTheme) {
                // System bar icons follow the in-app theme choice, not only the system's.
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            SoloTheme(settings.themeMode, settings.dynamicColor, settings.themeColor) {
                val consent by adConsent.state.collectAsStateWithLifecycle()
                SoloApp(
                    adBanner = adBanner,
                    privacyOptionsRequired = consent.privacyOptionsRequired,
                    onOpenPrivacyOptions = { adConsent.showPrivacyOptions(this) },
                )
            }
        }
    }
}
