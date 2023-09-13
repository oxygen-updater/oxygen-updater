package com.oxygenupdater.ui.onboarding

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.dialogs.ContributorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.SelectableSheet
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.utils.ContributorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    lists: SettingsListWrapper,
    initialDeviceIndex: Int,
    deviceChanged: (Device) -> Unit,
    initialMethodIndex: Int,
    methodChanged: (UpdateMethod) -> Unit,
    startApp: (Pair<Boolean, Boolean>) -> Unit, // contribute, submitLogs
) = Column(
    Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .fillMaxHeight()
) {
    val typography = MaterialTheme.typography

    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        DeviceChooser(lists.enabledDevices, initialDeviceIndex, deviceChanged)
        MethodChooser(lists.methodsForDevice, initialMethodIndex, methodChanged)

        val bodyMedium = typography.bodyMedium
        Text(
            AnnotatedString(
                stringResource(R.string.onboarding_app_uses),
                bodyMedium.toSpanStyle(),
                bodyMedium.toParagraphStyle().copy(textIndent = ListItemTextIndent)
            ),
            Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            style = bodyMedium
        )

        Text(
            stringResource(R.string.onboarding_caption),
            Modifier.padding(16.dp),
            style = bodyMedium
        )
    }

    ItemDivider()
    Text(
        stringResource(R.string.onboarding_disclaimer),
        Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        MaterialTheme.colorScheme.onSurfaceVariant,
        style = typography.bodySmall
    )

    StartApp(startApp)
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
    SheetOpener(
        deviceSelectionEnabled,
        Icons.Rounded.PhoneAndroid, R.string.settings_device,
        if (deviceSelectionEnabled) PrefManager.getString(
            PrefManager.KeyDevice, notSelected
        ) ?: notSelected else stringResource(R.string.summary_please_wait),
    ) { showSheet = true }

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide,
            enabledDevices, initialDeviceIndex,
            R.string.settings_device, R.string.onboarding_device_chooser_caption,
            PrefManager.KeyDeviceId,
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
    SheetOpener(
        methodSelectionEnabled,
        Icons.Outlined.CloudDownload, R.string.settings_update_method,
        if (methodSelectionEnabled) PrefManager.getString(
            PrefManager.KeyUpdateMethod, notSelected
        ) ?: notSelected else stringResource(R.string.summary_update_method),
    ) { showSheet = true }

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide,
            methodsForDevice, initialMethodIndex,
            R.string.settings_update_method, R.string.onboarding_method_chooser_caption,
            PrefManager.KeyUpdateMethodId,
            methodChanged
        )
    }
}

@Composable
private fun StartApp(
    startApp: (Pair<Boolean, Boolean>) -> Unit, // contribute, submitLogs
) {
    var contribute by rememberSaveableState("contribute", true)
    if (LocalInspectionMode.current || ContributorUtils.isAtLeastQAndPossiblyRooted) {
        var showSheet by rememberSaveableState("showContributorSheet", false)
        CheckboxText(
            contribute, { contribute = it }, R.string.contribute_agree,
            Modifier.padding(end = 4.dp), Modifier.padding(end = 16.dp),
        ) {
            IconButton({ showSheet = true }) {
                Icon(CustomIcons.Info, stringResource(R.string.contribute_more_info))
            }
        }

        if (showSheet) ModalBottomSheet({ showSheet = false }) { ContributorSheet(it) }
    }

    var submitLogs by rememberSaveableState("submitLogs", true)
    CheckboxText(
        submitLogs, { submitLogs = it }, R.string.settings_upload_logs,
        Modifier.padding(end = 16.dp, bottom = 16.dp), Modifier.padding(end = 16.dp),
    ) {
        OutlinedIconButton({
            startApp(contribute to submitLogs)
        }, Icons.Rounded.DoneAll, R.string.onboarding_finished_button)
    }
}

@Composable
private fun SheetOpener(
    enabled: Boolean,
    icon: ImageVector, @StringRes labelResId: Int,
    selectedName: String,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .animatedClickable(enabled, onClick)
            .padding(horizontal = 16.dp), // must be after 'clickable`
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, stringResource(R.string.icon), Modifier.padding(end = 16.dp), tint = colorScheme.primary)

        Column(Modifier.padding(vertical = 16.dp)) {
            Text(stringResource(labelResId), style = typography.titleMedium)
            Text(
                selectedName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewThemes
@Composable
fun PreviewOnboardingScreen() = PreviewAppTheme {
    OnboardingScreen(
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        SettingsListWrapper(
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
