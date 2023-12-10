package com.oxygenupdater.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.openAppLocalePage
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.putBoolean
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.SettingsListWrapper
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.AdvancedModeSheet
import com.oxygenupdater.ui.dialogs.ContributorSheet
import com.oxygenupdater.ui.dialogs.LanguageSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.SelectableSheet
import com.oxygenupdater.ui.dialogs.ThemeSheet
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.backgroundVariant
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.NotifStatus
import com.oxygenupdater.utils.NotificationUtils

private var previousAdFreeConfig = Triple<Boolean, Int, (() -> Unit)?>(
    false, R.string.settings_buy_ad_free_label, null
)

@Composable
fun SettingsScreen(
    navType: NavType,
    lists: SettingsListWrapper,
    initialDeviceIndex: Int,
    deviceChanged: (Device) -> Unit,
    initialMethodIndex: Int,
    methodChanged: (UpdateMethod) -> Unit,
    adFreePrice: String?,
    adFreeConfig: Triple<Boolean, Int, (() -> Unit)?>?,
    isPrivacyOptionsRequired: Boolean,
    showPrivacyOptionsForm: () -> Unit,
    openAboutScreen: () -> Unit,
) = Column(Modifier.verticalScroll(rememberScrollState())) {
    //region Support us
    Column(Modifier.background(MaterialTheme.colorScheme.backgroundVariant)) {
        Header(R.string.preference_header_support)

        val config = adFreeConfig ?: previousAdFreeConfig
        previousAdFreeConfig = config
        val (enabled, subtitleResId, onClick) = config
        val subtitle = if (onClick != null && adFreePrice != null) {
            stringResource(subtitleResId, adFreePrice)
        } else stringResource(subtitleResId)

        SettingsItem(
            onClick = { onClick?.invoke() },
            icon = Icons.Outlined.Paid,
            titleResId = R.string.label_buy_ad_free,
            subtitle = subtitle,
            enabled = enabled,
        )

        BecomeContributor()
    }
    //endregion

    //region Device
    Header(R.string.preference_header_device)

    DeviceChooser(
        enabledDevices = lists.enabledDevices,
        initialDeviceIndex = initialDeviceIndex,
        deviceChanged = deviceChanged,
    )

    MethodChooser(
        methodsForDevice = lists.methodsForDevice,
        initialMethodIndex = initialMethodIndex,
        methodChanged = methodChanged,
    )

    Notifications()
    //endregion

    //region UI
    Header(R.string.preference_header_ui)
    Theme()
    Language()
    //endregion

    //region Advanced
    Header(R.string.preference_header_advanced)
    AdvancedMode()
    SettingsAnalytics()
    PrivacyOptionsItem(
        isPrivacyOptionsRequired = isPrivacyOptionsRequired,
        showPrivacyOptionsForm = showPrivacyOptionsForm,
    )
    //endregion

    //region About
    Header(R.string.preference_header_about)

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    SettingsItem(
        // Use Chrome Custom Tabs to open the privacy policy link
        onClick = { customTabIntent.launch(context, "https://oxygenupdater.com/privacy/") },
        icon = Icons.Outlined.Policy,
        titleResId = R.string.label_privacy_policy,
        subtitle = stringResource(R.string.summary_privacy_policy),
    )

    SettingsItem(
        onClick = context::openPlayStorePage,
        icon = Icons.Rounded.StarOutline,
        titleResId = R.string.label_rate_app,
        subtitle = stringResource(R.string.summary_rate_app),
    )

    SettingsItem(
        onClick = openAboutScreen,
        icon = CustomIcons.LogoNotification,
        titleResId = R.string.app_name,
        subtitle = "v${BuildConfig.VERSION_NAME}",
    )
    //endregion

    ConditionalNavBarPadding(navType)
}

@Composable
private fun BecomeContributor() {
    var showSheet by rememberSaveableState("showContributorSheet", false)
    if (ContributorUtils.isAtLeastQAndPossiblyRooted) SettingsItem(
        onClick = { showSheet = true },
        icon = Icons.Outlined.GroupAdd,
        titleResId = R.string.contribute,
        subtitle = stringResource(R.string.settings_contribute_label),
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { ContributorSheet(it, true) }
}

@SuppressLint("PrivateResource")
@Composable
private fun DeviceChooser(
    enabledDevices: List<Device>,
    initialDeviceIndex: Int,
    deviceChanged: (Device) -> Unit,
) {
    var showSheet by rememberSaveableState("showDeviceSheet", false)

    val deviceSelectionEnabled = enabledDevices.isNotEmpty()
    val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (deviceSelectionEnabled) {
        PrefManager.getString(PrefManager.KeyDevice, notSelected) ?: notSelected
    } else stringResource(R.string.summary_please_wait)

    SettingsItem(
        onClick = { showSheet = true },
        icon = Icons.Rounded.PhoneAndroid,
        titleResId = R.string.settings_device,
        subtitle = subtitle,
        subtitleIsError = subtitle == notSelected,
        enabled = deviceSelectionEnabled,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            list = enabledDevices,
            initialIndex = initialDeviceIndex,
            titleResId = R.string.settings_device,
            captionResId = R.string.onboarding_device_chooser_caption,
            keyId = PrefManager.KeyDeviceId,
            onClick = deviceChanged,
        )
    }
}

@SuppressLint("PrivateResource")
@Composable
private fun MethodChooser(
    methodsForDevice: List<UpdateMethod>,
    initialMethodIndex: Int,
    methodChanged: (UpdateMethod) -> Unit,
) {
    var showSheet by rememberSaveableState("showMethodSheet", false)

    val methodSelectionEnabled = methodsForDevice.isNotEmpty()
    val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (methodSelectionEnabled) {
        PrefManager.getString(PrefManager.KeyUpdateMethod, notSelected) ?: notSelected
    } else stringResource(R.string.summary_update_method)

    SettingsItem(
        onClick = { showSheet = true },
        icon = Icons.Outlined.CloudDownload,
        titleResId = R.string.settings_update_method,
        subtitle = subtitle,
        subtitleIsError = subtitle == notSelected,
        enabled = methodSelectionEnabled,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            list = methodsForDevice,
            initialIndex = initialMethodIndex,
            titleResId = R.string.settings_update_method,
            captionResId = R.string.onboarding_method_chooser_caption,
            keyId = PrefManager.KeyUpdateMethodId,
            onClick = methodChanged,
        )
    }
}

@Composable
private fun Notifications() {
    val context = LocalContext.current
    val notificationUtils = remember { NotificationUtils(context) }

    val runningInPreview = LocalInspectionMode.current
    var notifStatus by remember {
        mutableStateOf(if (runningInPreview) NotifStatus() else notificationUtils.toNotifStatus())
    }

    if (!runningInPreview) LifecycleResumeEffect {
        // Check again in case user changed settings and came back to the app
        notifStatus = notificationUtils.toNotifStatus()
        onPauseOrDispose {}
    }

    val disabled = notifStatus.disabled
    val subtitleIsError: Boolean
    val notifSummary = if (disabled.isNullOrEmpty()) {
        subtitleIsError = true
        stringResource(R.string.summary_off)
    } else if (disabled.none { it }) {
        subtitleIsError = false
        stringResource(R.string.summary_on)
    } else {
        subtitleIsError = true
        val builder = StringBuilder()
        disabled.forEachIndexed { index, flag ->
            if (!flag) return@forEachIndexed
            when (index) {
                0 -> R.string.update_notification_channel_name
                1 -> R.string.news_notification_channel_name
                2 -> R.string.download_status_notification_channel_name
                else -> throw ArrayIndexOutOfBoundsException(index)
            }.let { builder.append("\n\u2022 " + stringResource(it)) }
        }
        stringResource(R.string.summary_important_notifications_disabled) + builder.toString()
    }

    SettingsItem(
        onClick = {
            val packageName = context.packageName
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            // Works only for API 21+ (Lollipop), which happens to be the min API
            else Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra("app_package", packageName).putExtra("app_uid", context.applicationInfo.uid)
            context.startActivity(intent)
        },
        icon = Icons.Rounded.NotificationsNone,
        titleResId = R.string.preference_header_notifications,
        subtitle = notifSummary,
        subtitleIsError = subtitleIsError,
    )
}

@Composable
private fun Theme() {
    var showSheet by rememberSaveableState("showThemeSheet", false)
    SettingsItem(
        onClick = { showSheet = true },
        icon = Icons.Outlined.Palette,
        titleResId = R.string.label_theme,
        subtitle = stringResource(PrefManager.theme.titleResId),
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        ThemeSheet(hide) { PrefManager.theme = it }
    }
}

@Composable
private fun Language() {
    val selectedLocale = currentLocale()
    val language = remember(selectedLocale) {
        selectedLocale.displayName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(selectedLocale) else it.toString()
        }
    }

    var showSheet by rememberSaveableState("showLanguageSheet", false)
    val context = LocalContext.current
    SettingsItem(
        onClick = {
            // Use our own sheet below Android 13
            showSheet = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true else try {
                // Otherwise delegate to system API on Android 13+ if possible
                context.openAppLocalePage()
                false
            } catch (e: Exception) {
                logError("SettingsScreen", "openAppLocalePage failed", e)
                true // fallback just in case
            }
        },
        icon = Icons.Outlined.Language,
        titleResId = R.string.label_language,
        subtitle = language,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { LanguageSheet(it, selectedLocale) }
}

@Composable
private fun AdvancedMode() {
    var showSheet by rememberSaveableState("showAdvancedModeSheet", false)

    var advancedMode by remember {
        mutableStateOf(PrefManager.getBoolean(PrefManager.KeyAdvancedMode, false))
    }
    SettingsSwitchItem(
        checked = advancedMode,
        onCheckedChange = {
            advancedMode = it
            putBoolean(PrefManager.KeyAdvancedMode, it)
        },
        icon = Icons.Rounded.LockOpen,
        titleResId = R.string.settings_advanced_mode,
        showWarning = { showSheet = true },
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        AdvancedModeSheet {
            putBoolean(PrefManager.KeyAdvancedMode, it)
            advancedMode = it
            hide()
        }
    }
}

@Composable
fun SettingsAnalytics() {
    var shareLogs by rememberSaveableState(
        "shareLogs", PrefManager.getBoolean(PrefManager.KeyShareAnalyticsAndLogs, true)
    )
    SettingsSwitchItem(
        checked = shareLogs,
        onCheckedChange = {
            shareLogs = it
            putBoolean(PrefManager.KeyShareAnalyticsAndLogs, it)
        },
        icon = Icons.Rounded.TrackChanges,
        titleResId = R.string.settings_upload_logs,
    )
}

@Composable
fun PrivacyOptionsItem(
    isPrivacyOptionsRequired: Boolean,
    showPrivacyOptionsForm: () -> Unit,
) {
    if (!isPrivacyOptionsRequired) return
    if (PrefManager.getBoolean(PrefManager.KeyAdFree, false)) return

    SettingsItem(
        onClick = showPrivacyOptionsForm,
        icon = Icons.Rounded.AdsClick,
        titleResId = R.string.settings_ad_privacy,
        subtitle = stringResource(R.string.settings_ad_privacy_subtitle),
    )
}

@Composable
fun SettingsSwitchItem(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    icon: ImageVector,
    @StringRes titleResId: Int,
    showWarning: (() -> Unit)? = null,
) {
    val checkedChange by rememberUpdatedState<(checked: Boolean) -> Unit> {
        // Handoff to BottomSheet if necessary, pref will update based on user choice (Cancel/Enable)
        if (it && showWarning != null) showWarning() else {
            onCheckedChange(it)
        }
    }

    SettingsItem(
        onClick = { checkedChange(!checked) },
        icon = icon,
        titleResId = titleResId,
        subtitle = stringResource(if (checked) R.string.summary_on else R.string.summary_off),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = checkedChange,
            thumbContent = {
                if (!checked) return@Switch
                Icon(Icons.Rounded.Done, null, Modifier.size(SwitchDefaults.IconSize))
            },
            modifier = Modifier.windowInsetsPadding(
                // Leave space for 2/3-button nav bar in landscape mode
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
        )
    }
}

@Composable
@NonRestartableComposable
private fun Header(@StringRes textResId: Int) {
    ItemDivider()
    Text(
        text = stringResource(textResId),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifierDefaultPaddingStartTopEnd
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@NonRestartableComposable
fun SettingsItem(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes titleResId: Int,
    subtitle: String?,
    subtitleIsError: Boolean = false,
    enabled: Boolean = true,
    content: @Composable (RowScope.() -> Unit)? = null,
) = Row(
    modifierMaxWidth
        .alpha(if (enabled) 1f else 0.38f)
        .animatedClickable(enabled, onClick)
        .then(modifierDefaultPadding), // must be after `clickable`
    verticalAlignment = Alignment.CenterVertically
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Icon(icon, stringResource(R.string.icon), tint = colorScheme.primary)

    Column(
        Modifier
            .weight(1f)
            .padding(start = 16.dp, end = if (content != null) 16.dp else 0.dp)
    ) {
        Text(
            text = stringResource(titleResId),
            style = typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )

        if (subtitle != null) Text(
            text = subtitle,
            color = if (subtitleIsError) colorScheme.error else colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis, maxLines = 10,
            style = typography.bodyMedium,
        )
    }

    // Extra content if callers want to re-use the same RowScope
    if (content != null) content()
}

@PreviewThemes
@Composable
fun PreviewSettingsScreen() = PreviewAppTheme {
    SettingsScreen(
        navType = NavType.BottomBar,
        lists = SettingsListWrapper(
            listOf(
                Device(
                    id = 1,
                    name = "OnePlus 7 Pro",
                    productNames = listOf("OnePlus7Pro"),
                    enabled = true,
                ),
                Device(
                    id = 2,
                    name = "OnePlus 8T",
                    productNames = listOf("OnePlus8T"),
                    enabled = true,
                ),
            ),
            listOf(
                UpdateMethod(
                    id = 1,
                    name = "Stable (full)",
                    recommendedForRootedDevice = true,
                    recommendedForNonRootedDevice = false,
                    supportsRootedDevice = true,
                ),
                UpdateMethod(
                    id = 2,
                    name = "Stable (incremental)",
                    recommendedForRootedDevice = false,
                    recommendedForNonRootedDevice = true,
                    supportsRootedDevice = false,
                )
            ),
        ),
        initialDeviceIndex = 1,
        deviceChanged = {},
        initialMethodIndex = 1,
        methodChanged = {},
        adFreePrice = null,
        adFreeConfig = previousAdFreeConfig,
        isPrivacyOptionsRequired = true,
        showPrivacyOptionsForm = {},
        openAboutScreen = {},
    )
}
