package com.oxygenupdater.compose.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.core.os.LocaleListCompat
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.LogoNotification
import com.oxygenupdater.compose.ui.SettingsListWrapper
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.dialogs.AdvancedModeSheet
import com.oxygenupdater.compose.ui.dialogs.ContributorSheet
import com.oxygenupdater.compose.ui.dialogs.LanguageSheet
import com.oxygenupdater.compose.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.compose.ui.dialogs.SelectableSheet
import com.oxygenupdater.compose.ui.dialogs.SheetType
import com.oxygenupdater.compose.ui.dialogs.ThemeSheet
import com.oxygenupdater.compose.ui.dialogs.defaultModalBottomSheetState
import com.oxygenupdater.compose.ui.dialogs.rememberSheetType
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
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.NEWS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.UPDATE_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationUtils
import java.util.Locale

private var previousAdFreeConfig: Triple<Boolean, Int, (() -> Unit)?> = Triple(
    false, R.string.settings_buy_ad_free_label, null
)

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val sheetState = defaultModalBottomSheetState()
    val listState = rememberLazyListState()
    var sheetType by rememberSheetType()
    val hide = rememberCallback { sheetType = SheetType.None }
    BackHandler(sheetState.isVisible, hide)

    val (enabledDevices, methodsForDevice) = lists
    val deviceSelectionEnabled = enabledDevices.isNotEmpty()
    val methodSelectionEnabled = methodsForDevice.isNotEmpty()
    val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)

    val selectedLocale = AppCompatDelegate.getApplicationLocales()[0] ?: LocaleListCompat.getDefault()[0] ?: Locale.getDefault()

    val advancedMode = remember {
        mutableStateOf(PrefManager.getBoolean(PrefManager.PROPERTY_ADVANCED_MODE, false))
    }

    if (sheetType != SheetType.None) ModalBottomSheet(hide, sheetState) {
        when (sheetType) {
            SheetType.Contributor -> ContributorSheet(hide, true)

            SheetType.Device -> SelectableSheet(
                hide,
                rememberLazyListState(), enabledDevices,
                initialDeviceIndex,
                R.string.settings_device, R.string.onboarding_page_2_caption,
                keyId = PrefManager.PROPERTY_DEVICE_ID, keyName = PrefManager.PROPERTY_DEVICE,
                deviceChanged
            )

            SheetType.Method -> SelectableSheet(
                hide,
                listState, methodsForDevice,
                initialMethodIndex,
                R.string.settings_update_method, R.string.onboarding_page_3_caption,
                keyId = PrefManager.PROPERTY_UPDATE_METHOD_ID, keyName = PrefManager.PROPERTY_UPDATE_METHOD,
                methodChanged
            )

            SheetType.Theme -> ThemeSheet(hide) { PrefManager.theme = it }
            SheetType.Language -> {
                // We're using this only on Android 12 & below
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return@ModalBottomSheet
                LanguageSheet(hide, selectedLocale)
            }

            SheetType.AdvancedMode -> AdvancedModeSheet(hide) {
                putBoolean(PrefManager.PROPERTY_ADVANCED_MODE, it)
                advancedMode.value = it
            }

            else -> {}
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
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
                if (onClick != null) onClick()
            }

            val runningInPreview = LocalInspectionMode.current
            if (runningInPreview || ContributorUtils.isAtLeastQAndPossiblyRooted) Item(
                Icons.Outlined.GroupAdd,
                R.string.contribute,
                stringResource(R.string.settings_contribute_label),
            ) {
                sheetType = SheetType.Contributor
            }
        }
        //endregion

        //region Device
        Header(R.string.preference_header_device)

        Item(
            Icons.Rounded.PhoneAndroid,
            R.string.settings_device,
            if (deviceSelectionEnabled) {
                PrefManager.getString(PrefManager.PROPERTY_DEVICE, notSelected) ?: notSelected
            } else stringResource(R.string.summary_please_wait),
            deviceSelectionEnabled
        ) {
            sheetType = SheetType.Device
        }

        Item(
            Icons.Outlined.CloudDownload, R.string.settings_update_method, if (methodSelectionEnabled) {
                PrefManager.getString(PrefManager.PROPERTY_UPDATE_METHOD, notSelected) ?: notSelected
            } else stringResource(R.string.summary_update_method), methodSelectionEnabled
        ) {
            sheetType = SheetType.Method
        }

        Notifications()
        //endregion

        //region UI
        Header(R.string.preference_header_ui)

        Item(Icons.Outlined.Palette, R.string.label_theme, PrefManager.theme.toString()) {
            sheetType = SheetType.Theme
        }

        val context = LocalContext.current
        Item(Icons.Outlined.Language, R.string.label_language, remember(selectedLocale) {
            selectedLocale.displayName.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(selectedLocale) else it.toString()
            }
        }) {
            // Delegate to system API on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.openAppLocalePage()
            // Otherwise use our own sheet
            else sheetType = SheetType.Language
        }
        //endregion

        //region Advanced
        Header(R.string.preference_header_advanced)

        SwitchItem(
            PrefManager.PROPERTY_ADVANCED_MODE, advancedMode,
            Icons.Rounded.LockOpen, R.string.settings_advanced_mode
        ) {
            sheetType = SheetType.AdvancedMode
        }

        SwitchItem(PrefManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS, remember {
            mutableStateOf(PrefManager.getBoolean(PrefManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS, true))
        }, Icons.Rounded.TrackChanges, R.string.settings_upload_logs)
        //endregion

        //region About
        Header(R.string.preference_header_about)

        val customTabIntent = rememberCustomTabsIntent()
        Item(Icons.Outlined.Policy, R.string.label_privacy_policy, stringResource(R.string.summary_privacy_policy)) {
            // Use Chrome Custom Tabs to open the privacy policy link
            customTabIntent.launch(context, "https://oxygenupdater.com/privacy/")
        }

        Item(Icons.Rounded.StarOutline, R.string.label_rate_app, stringResource(R.string.summary_rate_app)) {
            context.openPlayStorePage()
        }

        Item(
            CustomIcons.LogoNotification,
            R.string.app_name,
            "v${BuildConfig.VERSION_NAME}",
            onClick = openAboutScreen
        )
        //endregion
    }
}

@Composable
private fun Notifications() {
    val context = LocalContext.current
    val notificationUtils = remember { NotificationUtils(context) }

    val runningInPreview = LocalInspectionMode.current
    val notifSummary = if (runningInPreview) stringResource(R.string.summary_on)
    else if (notificationUtils.isDisabled) stringResource(R.string.summary_off)
    else {
        val disabled1 = notificationUtils.isDisabled(UPDATE_NOTIFICATION_CHANNEL_ID)
        val disabled2 = notificationUtils.isDisabled(NEWS_NOTIFICATION_CHANNEL_ID)
        val disabled3 = notificationUtils.isDisabled(DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID)
        val disabled = arrayOf(disabled1, disabled2, disabled3)
        if (disabled.none { it }) stringResource(R.string.summary_on) else {
            val builder = StringBuilder()
            disabled.forEachIndexed { index, flag ->
                if (index > 0) builder.append("\", \"")
                if (flag) when (index) {
                    0 -> R.string.update_notification_channel_name
                    1 -> R.string.news_notification_channel_name
                    2 -> R.string.download_and_installation_notifications_group_name
                    else -> throw ArrayIndexOutOfBoundsException(index)
                }.let {
                    builder.append(stringResource(it))
                }
            }
            stringResource(R.string.summary_important_notifications_disabled, builder.toString())
        }
    }

    Item(
        Icons.Rounded.NotificationsNone,
        R.string.preference_header_notifications,
        notifSummary,
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
private fun SwitchItem(
    key: String,
    checked: MutableState<Boolean>,
    icon: ImageVector,
    @StringRes titleResId: Int,
    showWarning: (() -> Unit)? = null,
) {
    val onCheckedChange by rememberUpdatedState<(Boolean) -> Unit> {
        // Handoff to BottomSheet if necessary, pref will update based on user choice (Cancel/Enable)
        if (it && showWarning != null) showWarning() else {
            checked.value = it
            putBoolean(key, it)
        }
    }

    Item(icon, titleResId, stringResource(if (checked.value) R.string.summary_on else R.string.summary_off), content = {
        Switch(checked.value, onCheckedChange, thumbContent = {
            if (!checked.value) return@Switch
            Icon(Icons.Rounded.Done, null, Modifier.size(SwitchDefaults.IconSize))
        })
    }) {
        onCheckedChange(!checked.value)
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
    Icon(icon, stringResource(R.string.icon), tint = MaterialTheme.colorScheme.primary)

    Column(Modifier.padding(start = 16.dp)) {
        Text(
            stringResource(titleResId),
            Modifier.basicMarquee(), maxLines = 1,
            style = MaterialTheme.typography.titleMedium
        )

        if (subtitle != null) Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis, maxLines = 10,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    // Extra content if callers want to re-use the same RowScope
    if (content != null) {
        Spacer(Modifier.weight(1f))
        content()
    }
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
