package com.oxygenupdater.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Info
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.SettingsListConfig
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
import com.oxygenupdater.ui.settings.DeviceChooser
import com.oxygenupdater.ui.settings.MethodChooser
import com.oxygenupdater.ui.settings.SettingsAnalytics
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewGetPrefBool
import com.oxygenupdater.ui.theme.PreviewGetPrefStr
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
import com.oxygenupdater.ui.theme.backgroundVariant
import com.oxygenupdater.utils.ContributorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    windowWidthSize: WindowWidthSizeClass,
    scrollBehavior: TopAppBarScrollBehavior,
    deviceConfig: SettingsListConfig<Device>,
    onDeviceSelect: (Device) -> Unit,
    methodConfig: SettingsListConfig<UpdateMethod>,
    onMethodSelect: (UpdateMethod) -> Unit,
    getPrefStr: (key: String, default: String) -> String,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    persistBool: (key: String, value: Boolean) -> Unit,
    onStartAppClick: (contribute: Boolean) -> Unit,
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
            DeviceChooser(config = deviceConfig, getPrefStr = getPrefStr, onSelect = onDeviceSelect)
            MethodChooser(config = methodConfig, getPrefStr = getPrefStr, onSelect = onMethodSelect)

            SettingsAnalytics(getPrefBool = getPrefBool, persistBool = persistBool)
        }

        // Note: if moving this to the right side of the screen, leave space for 2/3-button nav bar in landscape mode (same as Switch)
        StartApp(onStartAppClick)
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
        DeviceChooser(config = deviceConfig, getPrefStr = getPrefStr, onSelect = onDeviceSelect)
        MethodChooser(config = methodConfig, getPrefStr = getPrefStr, onSelect = onMethodSelect)

        SettingsAnalytics(getPrefBool = getPrefBool, persistBool = persistBool)

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

    StartApp(onClick = onStartAppClick)
    Spacer(Modifier.navigationBarsPadding())
}

@Composable
private fun StartApp(onClick: (contribute: Boolean) -> Unit) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(end = 16.dp)
) {
    var contribute by rememberSaveableState("contribute", true)
    if (ContributorUtils.isAtLeastQAndPossiblyRooted) {
        CheckboxText(
            checked = contribute,
            onCheckedChange = { contribute = it },
            textResId = R.string.contribute_agree,
            textModifier = Modifier.padding(end = 16.dp),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )

        var showSheet by rememberSaveableState("showContributorSheet", false)
        IconButton({ showSheet = true }) {
            Icon(CustomIcons.Info, stringResource(R.string.contribute_more_info))
        }

        if (showSheet) ModalBottomSheet({ showSheet = false }) { ContributorSheet(it) }
    } else Spacer(Modifier.weight(1f)) // always right-align start button

    OutlinedIconButton(
        onClick = { onClick(contribute) },
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
        deviceConfig = SettingsListConfig(
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
        ),
        onDeviceSelect = {},
        methodConfig = SettingsListConfig(
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
        ),
        onMethodSelect = {},
        getPrefStr = PreviewGetPrefStr,
        getPrefBool = PreviewGetPrefBool,
        persistBool = { _, _ -> },
        onStartAppClick = {},
    )
}
