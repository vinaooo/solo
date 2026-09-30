package io.github.vinaooo.solo

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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

/** One banner for every screen: it lives in the app scaffold, under the navigation host. */
@Composable
fun SoloApp(adBanner: AdBannerProvider, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Scaffold(
        modifier = modifier,
        bottomBar = { adBanner.Banner(Modifier.navigationBarsPadding()) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = GameRoute,
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                // Only the bottom (banner + navigation bar) is handled here; screens pad for the status bar.
                .consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding())),
        ) {
            composable<GameRoute> {
                GameScreenRoute(
                    onOpenScores = { navController.navigate(ScoresRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
            composable<ScoresRoute> { ScoresScreenRoute(onBack = navController::popBackStack) }
            composable<SettingsRoute> { SettingsScreenRoute(onBack = navController::popBackStack) }
        }
    }
}
