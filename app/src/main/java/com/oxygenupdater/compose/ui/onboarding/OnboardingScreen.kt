package com.oxygenupdater.compose.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.Info
import com.oxygenupdater.compose.ui.SettingsListWrapper
import com.oxygenupdater.compose.ui.common.CheckboxText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.ListItemTextIndent
import com.oxygenupdater.compose.ui.common.OutlinedIconButton
import com.oxygenupdater.compose.ui.common.animatedClickable
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.dialogs.ContributorSheet
import com.oxygenupdater.compose.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.compose.ui.dialogs.SelectableSheet
import com.oxygenupdater.compose.ui.dialogs.SheetType
import com.oxygenupdater.compose.ui.dialogs.defaultModalBottomSheetState
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
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
    finish: () -> Unit,
    startApp: (Boolean, Boolean) -> Unit, // contribute, submitLogs
) {
    val sheetState = defaultModalBottomSheetState()
    val listState = rememberLazyListState()
    var sheetType by remember { mutableStateOf(SheetType.None) }
    val hide = rememberCallback { sheetType = SheetType.None }

    val (enabledDevices, methodsForDevice) = lists
    val deviceSelectionEnabled = enabledDevices.isNotEmpty()
    val methodSelectionEnabled = methodsForDevice.isNotEmpty()
    val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)

    val typography = MaterialTheme.typography
    if (sheetType != SheetType.None) ModalBottomSheet(hide, sheetState) {
        when (sheetType) {
            SheetType.Device -> SelectableSheet(
                hide,
                listState, enabledDevices,
                initialDeviceIndex,
                R.string.settings_device, R.string.onboarding_page_2_caption,
                keyId = PrefManager.PROPERTY_DEVICE_ID, keyName = PrefManager.PROPERTY_DEVICE,
            ) {
                deviceChanged(it)
            }

            SheetType.Method -> SelectableSheet(
                hide,
                listState, methodsForDevice,
                initialMethodIndex,
                R.string.settings_update_method, R.string.onboarding_page_3_caption,
                keyId = PrefManager.PROPERTY_UPDATE_METHOD_ID, keyName = PrefManager.PROPERTY_UPDATE_METHOD,
            ) {
                methodChanged(it)
            }

            SheetType.Contributor -> ContributorSheet(hide)

            else -> {}
        }
    }

    Column(
        Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxHeight()
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SheetOpener(
                deviceSelectionEnabled,
                Icons.Rounded.PhoneAndroid, R.string.settings_device,
                if (deviceSelectionEnabled) PrefManager.getString(
                    PrefManager.PROPERTY_DEVICE, notSelected
                ) ?: notSelected else stringResource(R.string.summary_please_wait),
            ) {
                sheetType = SheetType.Device
            }

            SheetOpener(
                methodSelectionEnabled,
                Icons.Outlined.CloudDownload, R.string.settings_update_method,
                if (methodSelectionEnabled) PrefManager.getString(
                    PrefManager.PROPERTY_UPDATE_METHOD, notSelected
                ) ?: notSelected else stringResource(R.string.summary_update_method),
            ) {
                sheetType = SheetType.Method
            }

            val bodyMedium = typography.bodyMedium
            Text(
                AnnotatedString(
                    stringResource(R.string.onboarding_page_1_text),
                    bodyMedium.toSpanStyle(),
                    bodyMedium.toParagraphStyle().copy(textIndent = ListItemTextIndent)
                ),
                Modifier.padding(16.dp),
                style = bodyMedium
            )

            Text(
                stringResource(R.string.onboarding_page_4_text),
                Modifier.padding(16.dp),
                style = bodyMedium
            )
        }

        ItemDivider()
        Text(
            stringResource(R.string.onboarding_page_1_caption),
            Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            MaterialTheme.colorScheme.onSurfaceVariant,
            style = typography.bodySmall
        )

        val contribute = remember { mutableStateOf(true) }
        val runningInPreview = LocalInspectionMode.current
        if (runningInPreview || ContributorUtils.isAtLeastQAndPossiblyRooted) CheckboxText(
            contribute, R.string.contribute_agree,
            Modifier.padding(end = 4.dp), Modifier.padding(end = 16.dp),
        ) {
            IconButton({
                sheetType = SheetType.Contributor
            }) {
                Icon(CustomIcons.Info, stringResource(R.string.contribute_more_info))
            }
        }

        val submitLogs = remember { mutableStateOf(true) }
        CheckboxText(
            submitLogs, R.string.settings_upload_logs,
            Modifier.padding(end = 16.dp, bottom = 16.dp), Modifier.padding(end = 16.dp),
        ) {
            OutlinedIconButton({
                startApp(contribute.value, submitLogs.value)
            }, Icons.Rounded.DoneAll, R.string.onboarding_finished_button)
        }
    }

    BackHandler {
        if (sheetState.isVisible) hide()
        else finish()
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
                    englishName = "Stable (full)",
                    dutchName = "Stabiel (volledig)",
                    recommendedForRootedDevice = true,
                    recommendedForNonRootedDevice = false,
                    supportsRootedDevice = true,
                ),
                UpdateMethod(
                    id = 2,
                    englishName = "Stable (incremental)",
                    dutchName = "Stabiel (incrementeel)",
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
        finish = {},
        startApp = { _, _ -> },
    )
}

const val NOT_SET = -1
const val NOT_SET_L = -1L
