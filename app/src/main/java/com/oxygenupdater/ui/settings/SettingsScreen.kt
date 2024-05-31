package com.oxygenupdater.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.openAppLocalePage
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyAdFree
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyShareAnalyticsAndLogs
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.Theme
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
import com.oxygenupdater.ui.theme.LocalTheme
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewGetPrefBool
import com.oxygenupdater.ui.theme.PreviewGetPrefStr
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.backgroundVariant
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.NotifStatus
import com.oxygenupdater.utils.NotifUtils
import com.oxygenupdater.utils.logInfo

@Composable
fun SettingsScreen(
    navType: NavType,
    adFreePrice: String?,
    adFreeConfig: Triple<Boolean, Int, (() -> Unit)?>?,
    onContributorEnrollmentChange: (Boolean) -> Unit,
    deviceConfig: SettingsListConfig<Device>,
    onDeviceSelect: (Device) -> Unit,
    methodConfig: SettingsListConfig<UpdateMethod>,
    onMethodSelect: (UpdateMethod) -> Unit,
    onThemeSelect: (Theme) -> Unit,
    advancedMode: Boolean,
    onAdvancedModeChange: (Boolean) -> Unit,
    isPrivacyOptionsRequired: Boolean,
    showPrivacyOptionsForm: () -> Unit,
    openAboutScreen: () -> Unit,
    getPrefStr: (key: String, default: String) -> String,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    persistBool: (key: String, value: Boolean) -> Unit,
) = Column(
    Modifier
        .verticalScroll(rememberScrollState())
        .testTag(SettingsScreenTestTag)
) {
    // region Support us
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

        BecomeContributor(
            onContributorEnrollmentChange = onContributorEnrollmentChange,
        )
    }
    // endregion

    // region Device
    Header(R.string.preference_header_device)

    DeviceChooser(config = deviceConfig, getPrefStr = getPrefStr, onSelect = onDeviceSelect)
    MethodChooser(config = methodConfig, getPrefStr = getPrefStr, onSelect = onMethodSelect)

    Notifications()
    // endregion

    // region UI
    Header(R.string.preference_header_ui)
    Theme(onSelect = onThemeSelect)
    Language()
    // endregion

    // region Advanced
    Header(R.string.preference_header_advanced)
    AdvancedMode(initial = advancedMode, onChange = onAdvancedModeChange)
    SettingsAnalytics(getPrefBool = getPrefBool, persistBool = persistBool)
    PrivacyOptionsItem(
        isPrivacyOptionsRequired = isPrivacyOptionsRequired,
        showPrivacyOptionsForm = showPrivacyOptionsForm,
        getPrefBool = getPrefBool,
    )
    // endregion

    // region About
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
    // endregion

    ConditionalNavBarPadding(navType)
}

@Composable
fun DeviceChooser(
    config: SettingsListConfig<Device>,
    getPrefStr: (key: String, default: String) -> String,
    onSelect: (Device) -> Unit,
) {
    var showSheet by rememberSaveableState("showDeviceSheet", false)

    val deviceSelectionEnabled = config.list.isNotEmpty()
    @SuppressLint("PrivateResource") val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (deviceSelectionEnabled) {
        getPrefStr(KeyDevice, notSelected)
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
            config = config,
            titleResId = R.string.settings_device,
            captionResId = R.string.onboarding_device_chooser_caption,
            onClick = onSelect,
        )
    }
}

@Composable
fun MethodChooser(
    config: SettingsListConfig<UpdateMethod>,
    getPrefStr: (key: String, default: String) -> String,
    onSelect: (UpdateMethod) -> Unit,
) {
    var showSheet by rememberSaveableState("showMethodSheet", false)

    val methodSelectionEnabled = config.list.isNotEmpty()
    @SuppressLint("PrivateResource") val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (methodSelectionEnabled) {
        getPrefStr(KeyUpdateMethod, notSelected)
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
            config = config,
            titleResId = R.string.settings_update_method,
            captionResId = R.string.onboarding_method_chooser_caption,
            onClick = onSelect,
        )
    }
}

@Composable
private fun BecomeContributor(
    onContributorEnrollmentChange: (Boolean) -> Unit,
) {
    var showSheet by rememberSaveableState("showContributorSheet", false)
    if (ContributorUtils.isAtLeastQAndPossiblyRooted) SettingsItem(
        onClick = { showSheet = true },
        icon = Icons.Outlined.GroupAdd,
        titleResId = R.string.contribute,
        subtitle = stringResource(R.string.settings_contribute_label),
    )


    if (showSheet) ModalBottomSheet({ showSheet = false }) {
        ContributorSheet(
            hide = it,
            confirm = onContributorEnrollmentChange,
        )
    }
}

@Composable
private fun Notifications() {
    val context = LocalContext.current
    val runningInPreview = LocalInspectionMode.current
    var notifStatus by remember {
        mutableStateOf(if (runningInPreview) NotifStatus() else NotifUtils.toNotifStatus(context))
    }

    if (!runningInPreview) LifecycleResumeEffect(context) {
        // Check again in case user changed settings and came back to the app
        notifStatus = NotifUtils.toNotifStatus(context)
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
    } else stringResource(R.string.summary_important_notifications_disabled) + buildString {
        disabled.forEachIndexed { index, flag ->
            if (!flag) return@forEachIndexed
            when (index) {
                0 -> R.string.update_notification_channel_name
                1 -> R.string.news_notification_channel_name
                2 -> R.string.download_status_notification_channel_name
                else -> throw ArrayIndexOutOfBoundsException(index)
            }.let { append("\n• " + stringResource(it)) }
        }
    }.also {
        subtitleIsError = true
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
private fun Theme(onSelect: (Theme) -> Unit) {
    var showSheet by rememberSaveableState("showThemeSheet", false)
    SettingsItem(
        onClick = { showSheet = true },
        icon = Icons.Outlined.Palette,
        titleResId = R.string.label_theme,
        subtitle = stringResource(LocalTheme.current.titleResId),
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        ThemeSheet {
            onSelect(it)
            hide()
        }
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
                logInfo("SettingsScreen", "openAppLocalePage failed", e)
                true // fallback just in case
            }
        },
        icon = Icons.Outlined.Language,
        titleResId = R.string.label_language,
        subtitle = language,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        LanguageSheet({
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
            hide()
        }, selectedLocale)
    }
}

@Composable
private fun AdvancedMode(
    initial: Boolean,
    onChange: (Boolean) -> Unit,
) {
    var showSheet by rememberSaveableState("showAdvancedModeSheet", false)

    // Maintain state if user leaves this screen, then comes back to it
    var advancedMode by rememberSaveableState("advancedMode", initial)
    SettingsSwitchItem(
        checked = advancedMode,
        onCheckedChange = {
            onChange(it)
            advancedMode = it
        },
        icon = Icons.Rounded.LockOpen,
        titleResId = R.string.settings_advanced_mode,
        showWarning = { showSheet = true },
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        AdvancedModeSheet {
            onChange(it)
            advancedMode = it
            hide()
        }
    }
}

@Composable
fun SettingsAnalytics(
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    persistBool: (key: String, value: Boolean) -> Unit,
) {
    var shareLogs by rememberSaveableState(
        "shareLogs", getPrefBool(KeyShareAnalyticsAndLogs, true)
    )
    SettingsSwitchItem(
        checked = shareLogs,
        onCheckedChange = {
            shareLogs = it
            persistBool(KeyShareAnalyticsAndLogs, it)
        },
        icon = Icons.Rounded.TrackChanges,
        titleResId = R.string.settings_upload_logs,
    )
}

@Composable
fun PrivacyOptionsItem(
    isPrivacyOptionsRequired: Boolean,
    showPrivacyOptionsForm: () -> Unit,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
) {
    if (!isPrivacyOptionsRequired) return
    if (getPrefBool(KeyAdFree, false)) return

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
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifierMaxWidth
        .alpha(if (enabled) 1f else 0.38f)
        .animatedClickable(enabled, onClick)
        .then(modifierDefaultPadding) // must be after `clickable`
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

@VisibleForTesting
const val SettingsScreenTestTag = "SettingsScreen"

@VisibleForTesting
val DeviceSettingsListConfig = SettingsListConfig(
    list = listOf(
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
    initialIndex = 1,
    selectedId = NotSetL,
)

@VisibleForTesting
val MethodSettingsListConfig = SettingsListConfig(
    list = listOf(
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
    initialIndex = 1,
    selectedId = NotSetL,
)

@VisibleForTesting
var previousAdFreeConfig = Triple<Boolean, Int, (() -> Unit)?>(
    false, R.string.settings_buy_ad_free_label, null
)

@PreviewThemes
@Composable
fun PreviewSettingsScreen() = PreviewAppTheme {
    SettingsScreen(
        navType = NavType.BottomBar,
        adFreePrice = null,
        adFreeConfig = previousAdFreeConfig,
        onContributorEnrollmentChange = {},
        deviceConfig = DeviceSettingsListConfig,
        onDeviceSelect = {},
        methodConfig = MethodSettingsListConfig,
        onMethodSelect = {},
        onThemeSelect = {},
        advancedMode = false,
        onAdvancedModeChange = {},
        isPrivacyOptionsRequired = true,
        showPrivacyOptionsForm = {},
        openAboutScreen = {},
        getPrefStr = PreviewGetPrefStr,
        getPrefBool = PreviewGetPrefBool,
        persistBool = { _, _ -> },
    )
}
