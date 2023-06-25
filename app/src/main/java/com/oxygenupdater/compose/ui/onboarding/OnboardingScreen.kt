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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.oxygenupdater.compose.ui.TopAppBarDefaults
import com.oxygenupdater.compose.ui.TopAppBarScrollBehavior
import com.oxygenupdater.compose.ui.common.CheckboxText
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.ListItemTextIndent
import com.oxygenupdater.compose.ui.common.OutlinedIconButton
import com.oxygenupdater.compose.ui.common.animatedClickable
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OnboardingScreen(
    scrollBehavior: TopAppBarScrollBehavior,
    enabledDevices: List<Device>,
    methodsForDevice: List<UpdateMethod>,
    deviceChanged: (Device) -> Unit,
    methodChanged: (UpdateMethod) -> Unit,
    initialIndices: Pair<Int, Int>,
    finish: () -> Unit,
    startApp: (Boolean, Boolean) -> Unit, // contribute, submitLogs
) {
    val scope = rememberCoroutineScope()
    val sheetState = defaultModalBottomSheetState()
    val listState = rememberLazyListState()
    var sheetType by remember { mutableStateOf(SheetType.None) }
    val hide: () -> Unit = remember(scope, sheetState) {
        {
            sheetType = SheetType.None
            // Action passed for clicking close button in the content
            scope.launch { sheetState.hide() }
        }
    }

    LaunchedEffect(Unit) { // run only on init
        // Hide empty sheet in case activity was recreated or config was changed
        if (sheetState.isVisible && sheetType == SheetType.None) sheetState.hide()
    }

    val (initialDeviceIndex, initialMethodIndex) = initialIndices

    val deviceSelectionEnabled = enabledDevices.isNotEmpty()
    val methodSelectionEnabled = methodsForDevice.isNotEmpty()
    val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)

    val typography = MaterialTheme.typography
    ModalBottomSheet({
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
    }, sheetState) {
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
                    scope.launch { sheetState.show() }
                }

                SheetOpener(
                    methodSelectionEnabled,
                    Icons.Outlined.CloudDownload, R.string.settings_update_method,
                    if (methodSelectionEnabled) PrefManager.getString(
                        PrefManager.PROPERTY_UPDATE_METHOD, notSelected
                    ) ?: notSelected else stringResource(R.string.summary_update_method),
                ) {
                    sheetType = SheetType.Method
                    scope.launch { sheetState.show() }
                }

                val body2 = typography.body2
                Text(
                    AnnotatedString(
                        stringResource(R.string.onboarding_page_1_text),
                        body2.toSpanStyle(),
                        body2.toParagraphStyle().copy(textIndent = ListItemTextIndent)
                    ),
                    Modifier.padding(16.dp),
                    style = body2
                )

                Text(
                    stringResource(R.string.onboarding_page_4_text),
                    Modifier.padding(16.dp),
                    style = body2
                )
            }

            ItemDivider()
            Text(
                stringResource(R.string.onboarding_page_1_caption),
                Modifier
                    .alpha(ContentAlpha.medium)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                style = typography.caption
            )

            val contribute = remember { mutableStateOf(true) }
            val runningInPreview = LocalInspectionMode.current
            if (runningInPreview || ContributorUtils.isAtLeastQAndPossiblyRooted) CheckboxText(
                contribute, R.string.contribute_agree,
                Modifier.padding(end = 4.dp), Modifier.padding(end = 16.dp),
            ) {
                IconButton({
                    sheetType = SheetType.Contributor
                    scope.launch { sheetState.show() }
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
    val colors = MaterialTheme.colors
    val typography = MaterialTheme.typography

    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else ContentAlpha.disabled)
            .animatedClickable(enabled, onClick)
            .padding(horizontal = 16.dp), // must be after 'clickable`
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, stringResource(R.string.icon), Modifier.padding(end = 16.dp), tint = colors.primary)

        Column(Modifier.padding(vertical = 16.dp)) {
            Text(stringResource(labelResId), style = typography.subtitle1)
            Text(selectedName, Modifier.alpha(ContentAlpha.medium), style = typography.body2)
        }
    }
}

@PreviewThemes
@Composable
fun PreviewOnboardingScreen() = PreviewAppTheme {
    OnboardingScreen(
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        enabledDevices = listOf(
            Device(
                id = 1,
                name = "OnePlus 7 Pro",
                productName = "OnePlus7Pro",
            ),
            Device(
                id = 2,
                name = "OnePlus 8T",
                productName = "OnePlus8T",
            ),
        ),
        methodsForDevice = listOf(
            UpdateMethod(
                id = 1,
                englishName = "Stable (full)",
                dutchName = "Stabiel (volledig)",
                recommended = false,
                recommendedForRootedDevice = true,
                recommendedForNonRootedDevice = false,
                supportsRootedDevice = true,
            ), UpdateMethod(
                id = 2,
                englishName = "Stable (incremental)",
                dutchName = "Stabiel (incrementeel)",
                recommended = true,
                recommendedForRootedDevice = false,
                recommendedForNonRootedDevice = true,
                supportsRootedDevice = false,
            )
        ),
        deviceChanged = {},
        methodChanged = {},
        initialIndices = 0 to 1,
        finish = {},
        startApp = { _, _ -> },
    )
}

const val NOT_SET = -1
const val NOT_SET_L = -1L
