package com.oxygenupdater.ui.onboarding

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.DoneAll
import com.oxygenupdater.icons.Info
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.common.CheckboxText
import com.oxygenupdater.ui.common.ListItemTextIndent
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStartTopEnd
import com.oxygenupdater.ui.common.modifierMaxSize
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.dialogs.ContributorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.settings.DeviceChooser
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.ui.settings.MethodChooser
import com.oxygenupdater.ui.settings.MethodSettingsListConfig
import com.oxygenupdater.ui.settings.SettingsAnalytics
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewGetPrefBool
import com.oxygenupdater.ui.theme.PreviewGetPrefStr
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
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
                .testTag(OnboardingScreen_MainColumnTestTag)
        ) {
            DeviceChooser(config = deviceConfig, getPrefStr = getPrefStr, onSelect = onDeviceSelect)
            MethodChooser(config = methodConfig, getPrefStr = getPrefStr, onSelect = onMethodSelect)

            SettingsAnalytics(getPrefBool = getPrefBool, persistBool = persistBool)
        }

        // Note: if moving this to the right side of the screen, leave space for 2/3-button nav bar in landscape mode (same as Switch)
        StartApp(onStartAppClick)
        Spacer(Modifier.navigationBarsPadding())
    }

    VerticalDivider()

    Column(Modifier.weight(1f)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .testTag(OnboardingScreen_SecondaryColumnTestTag)
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

        HorizontalDivider()
        Text(
            text = stringResource(R.string.onboarding_disclaimer),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = typography.bodySmall,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                .testTag(OnboardingScreen_DisclaimerTestTag)
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
            .testTag(OnboardingScreen_MainColumnTestTag)
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

    HorizontalDivider()
    Text(
        text = stringResource(R.string.onboarding_disclaimer),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = typography.bodySmall,
        modifier = Modifier
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            .testTag(OnboardingScreen_DisclaimerTestTag)
    )

    StartApp(onClick = onStartAppClick)
    Spacer(Modifier.navigationBarsPadding())
}

@Composable
private fun StartApp(onClick: (contribute: Boolean) -> Unit) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(end = 16.dp)
) {
    var contribute by rememberState(true)
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

        var showSheet by rememberState(false)
        IconButton(
            onClick = { showSheet = true },
            modifier = Modifier.testTag(OnboardingScreen_ContributorInfoButtonTestTag),
        ) {
            Icon(Symbols.Info, stringResource(R.string.contribute_more_info))
        }

        if (showSheet) ModalBottomSheet({ showSheet = false }) { ContributorSheet(it) }
    } else Spacer(Modifier.weight(1f)) // always right-align start button

    OutlinedIconButton(
        onClick = { onClick(contribute) },
        icon = Symbols.DoneAll,
        textResId = R.string.onboarding_finished_button,
    )
}

private const val TAG = "OnboardingScreen"

@VisibleForTesting
const val OnboardingScreen_MainColumnTestTag = TAG + "_MainColumn"

@VisibleForTesting
const val OnboardingScreen_SecondaryColumnTestTag = TAG + "_SecondaryColumn"

@VisibleForTesting
const val OnboardingScreen_ContributorInfoButtonTestTag = TAG + "_ContributorInfoButton"

@VisibleForTesting
const val OnboardingScreen_DisclaimerTestTag = TAG + "_Disclaimer"

@SuppressLint("VisibleForTests")
@OptIn(ExperimentalMaterial3Api::class)
@PreviewThemes
@Composable
fun PreviewOnboardingScreen() = PreviewAppTheme {
    OnboardingScreen(
        windowWidthSize = PreviewWindowSize.widthSizeClass,
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        deviceConfig = DeviceSettingsListConfig,
        onDeviceSelect = {},
        methodConfig = MethodSettingsListConfig,
        onMethodSelect = {},
        getPrefStr = PreviewGetPrefStr,
        getPrefBool = PreviewGetPrefBool,
        persistBool = { _, _ -> },
        onStartAppClick = {},
    )
}
