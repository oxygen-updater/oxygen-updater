package com.oxygenupdater.ui.onboarding

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Info
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.SettingsListWrapper
import com.oxygenupdater.ui.common.CheckboxText
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.ListItemTextIndent
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierMaxSize
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.dialogs.ContributorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.SelectableSheet
import com.oxygenupdater.ui.settings.SettingsAnalytics
import com.oxygenupdater.ui.settings.SettingsItem
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
import com.oxygenupdater.ui.theme.backgroundVariant
import com.oxygenupdater.utils.ContributorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    windowWidthSize: WindowWidthSizeClass,
    scrollBehavior: TopAppBarScrollBehavior,
    lists: SettingsListWrapper,
    initialDeviceIndex: Int,
    deviceChanged: (Device) -> Unit,
    initialMethodIndex: Int,
    methodChanged: (UpdateMethod) -> Unit,
    startApp: (contribute: Boolean) -> Unit,
) = if (windowWidthSize == WindowWidthSizeClass.Expanded) Row(
    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) then modifierMaxWidth
) {
    val typography = MaterialTheme.typography
    Column(Modifier.weight(1f)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
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

            SettingsAnalytics()
        }

        // Note: if moving this to the right side of the screen, leave space for 2/3-button nav bar in landscape mode (same as Switch)
        StartApp(startApp)
        Spacer(Modifier.navigationBarsPadding())
    }

    VerticalDivider(color = MaterialTheme.colorScheme.backgroundVariant)

    Column(Modifier.weight(1f)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            val bodyMedium = typography.bodyMedium
            Text(
                text = AnnotatedString(
                    stringResource(R.string.onboarding_app_uses),
                    bodyMedium.toSpanStyle(),
                    bodyMedium.toParagraphStyle().copy(textIndent = ListItemTextIndent)
                ),
                style = bodyMedium,
                modifier = modifierDefaultPaddingStartTopEnd
            )

            Text(
                text = stringResource(R.string.onboarding_caption),
                style = bodyMedium,
                modifier = modifierDefaultPadding
            )
        }

        ItemDivider()
        Text(
            text = stringResource(R.string.onboarding_disclaimer),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        Spacer(Modifier.navigationBarsPadding())
    }
} else Column(
    Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .then(modifierMaxSize)
) {
    val typography = MaterialTheme.typography

    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
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

        SettingsAnalytics()

        val bodyMedium = typography.bodyMedium
        Text(
            text = AnnotatedString(
                stringResource(R.string.onboarding_app_uses),
                bodyMedium.toSpanStyle(),
                bodyMedium.toParagraphStyle().copy(textIndent = ListItemTextIndent)
            ),
            style = bodyMedium,
            modifier = modifierDefaultPaddingStartTopEnd
        )

        Text(
            text = stringResource(R.string.onboarding_caption),
            style = bodyMedium,
            modifier = modifierDefaultPadding
        )
    }

    ItemDivider()
    Text(
        text = stringResource(R.string.onboarding_disclaimer),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
    )

    StartApp(startApp)
    Spacer(Modifier.navigationBarsPadding())
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
        { showSheet = true },
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
        { showSheet = true },
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
            onClick = methodChanged
        )
    }
}

@Composable
private fun StartApp(startApp: (contribute: Boolean) -> Unit) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(end = 16.dp)
) {
    var contribute by rememberSaveableState("contribute", true)
    if (LocalInspectionMode.current || ContributorUtils.isAtLeastQAndPossiblyRooted) {
        CheckboxText(
            checked = contribute,
            onCheckedChange = { contribute = it },
            textResId = R.string.contribute_agree,
            textModifier = Modifier.padding(end = 16.dp),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 16.dp)
        )

        var showSheet by rememberSaveableState("showContributorSheet", false)
        IconButton({ showSheet = true }) {
            Icon(CustomIcons.Info, stringResource(R.string.contribute_more_info))
        }

        if (showSheet) ModalBottomSheet({ showSheet = false }) { ContributorSheet(it) }
    } else Spacer(Modifier.weight(1f)) // always right-align start button

    OutlinedIconButton(
        onClick = { startApp(contribute) },
        icon = Icons.Rounded.DoneAll,
        textResId = R.string.onboarding_finished_button,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewThemes
@Composable
fun PreviewOnboardingScreen() = PreviewAppTheme {
    OnboardingScreen(
        windowWidthSize = PreviewWindowSize.widthSizeClass,
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
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
        startApp = {},
    )
}

const val NotSet = -1
const val NotSetL = -1L
