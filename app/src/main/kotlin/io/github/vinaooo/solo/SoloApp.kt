package io.github.vinaooo.solo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.vinaooo.solo.core.ads.AdBannerProvider
import io.github.vinaooo.solo.feature.game.ui.GameRoute as GameScreenRoute
import io.github.vinaooo.solo.feature.scores.ScoresRoute as ScoresScreenRoute
import io.github.vinaooo.solo.feature.settings.SettingsRoute as SettingsScreenRoute
import io.github.vinaooo.solo.navigation.GameRoute
import io.github.vinaooo.solo.navigation.ScoresRoute
import io.github.vinaooo.solo.navigation.SettingsRoute

/** The navigation host. Only the game screen carries the ad banner, at its bottom; Scores and Settings have none. */
@Composable
fun SoloApp(
    adBanner: AdBannerProvider,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = GameRoute,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        composable<GameRoute> {
            Column {
                // The banner pads for the navigation bar, so the game above it must not pad for it again.
                val navigationBar = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                Box(Modifier.weight(1f).consumeWindowInsets(navigationBar)) {
                    GameScreenRoute(
                        onOpenScores = { navController.navigate(ScoresRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                adBanner.Banner(Modifier.navigationBarsPadding())
            }
        }
        composable<ScoresRoute> { ScoresScreenRoute(onBack = navController::popBackStack) }
        composable<SettingsRoute> {
            SettingsScreenRoute(
                onBack = navController::popBackStack,
                privacyOptionsRequired = privacyOptionsRequired,
                onOpenPrivacyOptions = onOpenPrivacyOptions,
            )
        }
    }
}
