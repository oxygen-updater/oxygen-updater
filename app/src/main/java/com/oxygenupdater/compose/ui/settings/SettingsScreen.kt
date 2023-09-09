package com.oxygenupdater.compose.ui.settings

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.LogoNotification
import com.oxygenupdater.compose.ui.SettingsListWrapper
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.common.rememberSaveableState
import com.oxygenupdater.compose.ui.currentLocale
import com.oxygenupdater.compose.ui.dialogs.AdvancedModeSheet
import com.oxygenupdater.compose.ui.dialogs.ContributorSheet
import com.oxygenupdater.compose.ui.dialogs.LanguageSheet
import com.oxygenupdater.compose.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.compose.ui.dialogs.SelectableSheet
import com.oxygenupdater.compose.ui.dialogs.ThemeSheet
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.backgroundVariant
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.openAppLocalePage
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.putBoolean
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.NotifStatus
import com.oxygenupdater.utils.NotificationUtils

private var previousAdFreeConfig: Triple<Boolean, Int, (() -> Unit)?> = Triple(
    false, R.string.settings_buy_ad_free_label, null
)

@Composable
fun SettingsScreen(
    lists: SettingsListWrapper,
    initialDeviceIndex: Int,
    deviceChanged: (Device) -> Unit,
    initialMethodIndex: Int,
    methodChanged: (UpdateMethod) -> Unit,
    adFreePrice: String?,
    adFreeConfig: Triple<Boolean, Int, (() -> Unit)?>?,
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

        Item(Icons.Outlined.Paid, R.string.label_buy_ad_free, subtitle, enabled) {
            onClick?.invoke()
        }

        BecomeContributor()
    }
    //endregion

    //region Device
    Header(R.string.preference_header_device)
    DeviceChooser(lists.enabledDevices, initialDeviceIndex, deviceChanged)
    MethodChooser(lists.methodsForDevice, initialMethodIndex, methodChanged)
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
    Analytics()
    //endregion

    //region About
    Header(R.string.preference_header_about)

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    Item(
        Icons.Outlined.Policy,
        R.string.label_privacy_policy, stringResource(R.string.summary_privacy_policy),
        onClick = rememberCallback(context) {
            // Use Chrome Custom Tabs to open the privacy policy link
            customTabIntent.launch(context, "https://oxygenupdater.com/privacy/")
        }
    )

    Item(
        Icons.Rounded.StarOutline,
        R.string.label_rate_app, stringResource(R.string.summary_rate_app),
        onClick = rememberCallback(context, context::openPlayStorePage)
    )

    Item(
        CustomIcons.LogoNotification,
        R.string.app_name, "v${BuildConfig.VERSION_NAME}",
        onClick = openAboutScreen
    )
    //endregion
}

@Composable
private fun BecomeContributor() {
    var showSheet by rememberSaveableState("showContributorSheet", false)
    val runningInPreview = LocalInspectionMode.current
    if (runningInPreview || ContributorUtils.isAtLeastQAndPossiblyRooted) Item(
        Icons.Outlined.GroupAdd,
        R.string.contribute, stringResource(R.string.settings_contribute_label),
    ) { showSheet = true }

    val hide = rememberCallback { showSheet = false }
    if (showSheet) ModalBottomSheet(hide) {
        ContributorSheet(hide, true)
    }
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
        PrefManager.getString(PrefManager.PROPERTY_DEVICE, notSelected) ?: notSelected
    } else stringResource(R.string.summary_please_wait)
    Item(
        Icons.Rounded.PhoneAndroid,
        R.string.settings_device, subtitle,
        deviceSelectionEnabled,
        subtitleIsError = subtitle == notSelected
    ) { showSheet = true }

    val hide = rememberCallback { showSheet = false }
    if (showSheet) ModalBottomSheet(hide) {
        SelectableSheet(
            hide,
            enabledDevices, initialDeviceIndex,
            R.string.settings_device, R.string.onboarding_device_chooser_caption,
            PrefManager.PROPERTY_DEVICE_ID,
            deviceChanged
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
        PrefManager.getString(PrefManager.PROPERTY_UPDATE_METHOD, notSelected) ?: notSelected
    } else stringResource(R.string.summary_update_method)
    Item(
        Icons.Outlined.CloudDownload,
        R.string.settings_update_method, subtitle,
        methodSelectionEnabled,
        subtitleIsError = subtitle == notSelected
    ) { showSheet = true }

    val hide = rememberCallback { showSheet = false }
    if (showSheet) ModalBottomSheet(hide) {
        SelectableSheet(
            hide,
            methodsForDevice, initialMethodIndex,
            R.string.settings_update_method, R.string.onboarding_method_chooser_caption,
            PrefManager.PROPERTY_UPDATE_METHOD_ID,
            methodChanged
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

    if (!runningInPreview) {
        // Re-run above onResume
        val observer = remember {
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

                notifStatus = notificationUtils.toNotifStatus()
            }
        }

        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, observer) {
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
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

    Item(
        Icons.Rounded.NotificationsNone,
        R.string.preference_header_notifications, notifSummary,
        subtitleIsError = subtitleIsError
    ) {
        val packageName = context.packageName
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Intent(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        // Works only for API 21+ (Lollipop), which happens to be the min API
        else Intent(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ).putExtra("app_package", packageName).putExtra("app_uid", context.applicationInfo.uid)
        context.startActivity(intent)
    }
}

@Composable
private fun Theme() {
    var showSheet by rememberSaveableState("showThemeSheet", false)
    Item(Icons.Outlined.Palette, R.string.label_theme, PrefManager.theme.toString()) {
        showSheet = true
    }

    val hide = rememberCallback { showSheet = false }
    if (showSheet) ModalBottomSheet(hide) {
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Delegate to system API on Android 13+
        val context = LocalContext.current
        Item(
            Icons.Outlined.Language, R.string.label_language, language,
            onClick = rememberCallback(context, context::openAppLocalePage)
        )
    } else {
        // Otherwise use our own sheet
        var showSheet by rememberSaveableState("showLanguageSheet", false)
        Item(Icons.Outlined.Language, R.string.label_language, language) { showSheet = true }

        val hide = rememberCallback { showSheet = false }
        if (showSheet) ModalBottomSheet(hide) {
            LanguageSheet(hide, selectedLocale)
        }
    }
}

@Composable
private fun AdvancedMode() {
    var showSheet by rememberSaveableState("showAdvancedModeSheet", false)

    var advancedMode by remember {
        mutableStateOf(PrefManager.getBoolean(PrefManager.PROPERTY_ADVANCED_MODE, false))
    }
    SwitchItem(advancedMode, {
        advancedMode = it
        putBoolean(PrefManager.PROPERTY_ADVANCED_MODE, it)
    }, Icons.Rounded.LockOpen, R.string.settings_advanced_mode) {
        showSheet = true
    }

    if (showSheet) ModalBottomSheet({ showSheet = false }) {
        AdvancedModeSheet {
            putBoolean(PrefManager.PROPERTY_ADVANCED_MODE, it)
            advancedMode = it
            showSheet = false
        }
    }
}

@Composable
private fun Analytics() {
    var shareLogs by rememberSaveableState(
        "shareLogs", PrefManager.getBoolean(PrefManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS, true)
    )
    SwitchItem(shareLogs, {
        shareLogs = it
        putBoolean(PrefManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS, it)
    }, Icons.Rounded.TrackChanges, R.string.settings_upload_logs)
}

@Composable
private fun SwitchItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    @StringRes titleResId: Int,
    showWarning: (() -> Unit)? = null,
) {
    val checkedChange by rememberUpdatedState<(Boolean) -> Unit> {
        // Handoff to BottomSheet if necessary, pref will update based on user choice (Cancel/Enable)
        if (it && showWarning != null) showWarning() else {
            onCheckedChange(it)
        }
    }

    Item(icon, titleResId, stringResource(if (checked) R.string.summary_on else R.string.summary_off), content = {
        Switch(checked, checkedChange, thumbContent = {
            if (!checked) return@Switch
            Icon(Icons.Rounded.Done, null, Modifier.size(SwitchDefaults.IconSize))
        })
    }) {
        checkedChange(!checked)
    }
}

@Composable
private fun Header(@StringRes textResId: Int) {
    ItemDivider(Modifier.padding(bottom = 16.dp))
    Text(
        stringResource(textResId),
        Modifier.padding(horizontal = 16.dp),
        MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Item(
    icon: ImageVector,
    @StringRes titleResId: Int, subtitle: String?,
    enabled: Boolean = true,
    subtitleIsError: Boolean = false,
    content: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
) = Row(
    Modifier
        .fillMaxWidth()
        .alpha(if (enabled) 1f else 0.38f)
        .animatedClickable(enabled, onClick)
        .padding(16.dp), // must be after `clickable`
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
            stringResource(titleResId),
            Modifier.basicMarquee(), maxLines = 1,
            style = typography.titleMedium
        )

        if (subtitle != null) Text(
            subtitle,
            color = if (subtitleIsError) colorScheme.error else colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis, maxLines = 10,
            style = typography.bodyMedium
        )
    }

    // Extra content if callers want to re-use the same RowScope
    if (content != null) content()
}

@PreviewThemes
@Composable
fun PreviewSettingsScreen() = PreviewAppTheme {
    SettingsScreen(
        SettingsListWrapper(
            listOf(
                Device(
                    id = 1,
                    name = "OnePlus 7 Pro",
                    productNamesCsv = "OnePlus7Pro",
                    enabled = true,
                ),
                Device(
                    id = 2,
                    name = "OnePlus 8T",
                    productNamesCsv = "OnePlus8T",
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
        openAboutScreen = {},
    )
}
