package io.github.vinaooo.solo.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.settings.NewGameConfirmDialog
import io.github.vinaooo.vinkit.settings.SettingsScreen as VinkitSettingsScreen
import io.github.vinaooo.vinkit.settings.SettingsSection

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val pendingChange by viewModel.pendingChange.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    SettingsScreen(
        settings = settings,
        appSettings = appSettings,
        onChange = viewModel::onChange,
        onAppChange = viewModel::onAppChange,
        onBack = onBack,
        modifier = modifier,
        privacyOptionsRequired = privacyOptionsRequired,
        onOpenPrivacyOptions = onOpenPrivacyOptions,
        onOpenPrivacyPolicy = { uriHandler.openUri(privacyPolicyUrl) },
    )
    if (pendingChange != null) {
        NewGameConfirmDialog(
            viewModel::confirmChange,
            viewModel::dismissChange,
            stringResource(R.string.new_game_confirm_text),
        )
    }
}

/** vinkit's Settings screen with Solo's [GameSection] first. */
@Composable
fun SettingsScreen(
    settings: Settings,
    appSettings: AppSettings,
    onChange: (SettingsChange) -> Unit,
    onAppChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
) {
    VinkitSettingsScreen(
        settings = appSettings,
        onChange = onAppChange,
        onBack = onBack,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        modifier = modifier,
        gameSections = listOf(
            SettingsSection(stringResource(R.string.section_game)) { GameSection(settings, onChange) },
        ),
        privacyOptionsRequired = privacyOptionsRequired,
        onOpenPrivacyOptions = onOpenPrivacyOptions,
    )
}
