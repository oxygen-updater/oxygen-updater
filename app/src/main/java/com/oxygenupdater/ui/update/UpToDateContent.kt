package com.oxygenupdater.ui.update

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Info
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.ExpandCollapse
import com.oxygenupdater.ui.common.IconText
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierDefaultPaddingStart
import com.oxygenupdater.ui.common.modifierDefaultPaddingTop
import com.oxygenupdater.ui.common.modifierMaxSize
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.device.DeviceSoftwareInfo
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewGetPrefStr
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize
import com.oxygenupdater.ui.theme.positive

@Composable
fun UpToDate(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    refreshing: Boolean,
    updateData: UpdateData,
    getPrefStr: (key: String, default: String) -> String,
) {
    val currentOtaVersion = SystemVersionProperties.otaVersion
    val method = getPrefStr(KeyUpdateMethod, "")

    val incomingOtaVersion = updateData.otaVersionNumber
    val isDifferentVersion = incomingOtaVersion != currentOtaVersion
    val showAdvancedModeTip = isDifferentVersion // show advanced mode hint only if OTA versions don't match…
            // …and incoming is newer (older builds can't be installed due to standard Android security measures)
            && UpdateData.getBuildDate(incomingOtaVersion) >= UpdateData.getBuildDate(currentOtaVersion)
            // …and incoming is likely a full ZIP, or the selected update method is "full"
            // (incrementals are only for specific source/target version combos)
            && (updateData.downloadSize >= FullZipLikelyMinSize || method.endsWith("(full)") || method.endsWith("(volledig)"))

    if (windowWidthSize == WindowWidthSizeClass.Expanded) Column(
        Modifier.testTag(UpToDateContentTestTag)
    ) {
        AdvancedModeTip(showAdvancedModeTip, getPrefStr)

        Row(modifierMaxWidth) {
            Column(
                Modifier
                    .width(IntrinsicSize.Max)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .testTag(UpToDateContent_InfoColumnTestTag)
            ) {
                val positive = MaterialTheme.colorScheme.positive
                val titleMedium = MaterialTheme.typography.titleMedium.copy(color = positive)
                IconText(
                    icon = Icons.Rounded.CheckCircleOutline,
                    text = stringResource(R.string.update_information_system_is_up_to_date),
                    iconTint = positive,
                    style = titleMedium,
                    textModifier = Modifier.withPlaceholder(refreshing, titleMedium),
                    modifier = modifierDefaultPadding
                )

                HorizontalDivider()
                DeviceSoftwareInfo(showHeader = false)
                ConditionalNavBarPadding(navType)
            }

            VerticalDivider()

            ChangelogContainer(
                refreshing = refreshing,
                updateData = updateData,
                isDifferentVersion = isDifferentVersion,
                showAdvancedModeTip = showAdvancedModeTip,
                getPrefStr = getPrefStr,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .then(modifierDefaultPaddingTop) // must be after `verticalScroll`
            ) {
                ConditionalNavBarPadding(navType)
            }
        }
    } else Column(
        modifierMaxSize
            .verticalScroll(rememberScrollState())
            .testTag(UpToDateContentTestTag)
    ) {
        AdvancedModeTip(showAdvancedModeTip, getPrefStr)

        Box(modifierMaxWidth) {
            val positive = MaterialTheme.colorScheme.positive
            val titleMedium = MaterialTheme.typography.titleMedium.copy(color = positive)
            IconText(
                icon = Icons.Rounded.CheckCircleOutline,
                text = stringResource(R.string.update_information_system_is_up_to_date),
                iconTint = positive,
                style = titleMedium,
                textModifier = Modifier.withPlaceholder(refreshing, titleMedium),
                modifier = Modifier.align(Alignment.CenterStart) then modifierDefaultPaddingStart
            )

            Icon(
                imageVector = Icons.Rounded.DoneAll,
                contentDescription = stringResource(R.string.icon),
                modifier = Modifier
                    .graphicsLayer(scaleX = 2f, scaleY = 2f, alpha = .1f)
                    .align(Alignment.CenterEnd)
                    .requiredSize(64.dp)
            )
        }

        HorizontalDivider(Modifier.padding(top = 2.dp))
        DeviceSoftwareInfo(showHeader = false)
        HorizontalDivider(modifierDefaultPaddingTop)

        ExpandableChangelog(
            refreshing = refreshing,
            updateData = updateData,
            isDifferentVersion = isDifferentVersion,
            showAdvancedModeTip = showAdvancedModeTip,
            getPrefStr = getPrefStr,
        )

        HorizontalDivider()
        ConditionalNavBarPadding(navType)
    }
}

@Composable
private inline fun AdvancedModeTip(
    show: Boolean,
    getPrefStr: (key: String, default: String) -> String,
) {
    if (!show) return

    IconText(
        icon = CustomIcons.Info,
        text = stringResource(
            R.string.update_information_banner_advanced_mode_tip,
            getPrefStr(KeyUpdateMethod, "<UNKNOWN>"),
        ),
        modifier = modifierDefaultPadding
    )
    HorizontalDivider()
}

@Composable
private fun ExpandableChangelog(
    refreshing: Boolean,
    updateData: UpdateData,
    isDifferentVersion: Boolean,
    showAdvancedModeTip: Boolean,
    getPrefStr: (key: String, default: String) -> String,
) {
    val expandEnabled = updateData.isUpdateInformationAvailable
    var expanded by rememberSaveableState("changelogExpanded", false)
    IconText(
        icon = if (!expandEnabled) Icons.Rounded.ErrorOutline else if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
        text = stringResource(
            if (!expandEnabled) R.string.update_information_no_update_data_available
            else R.string.update_information_view_update_information
        ),
        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
        modifier = modifierMaxWidth
            .alpha(if (!refreshing) 1f else 0.38f)
            .animatedClickable(!refreshing && expandEnabled) { expanded = !expanded }
            .then(modifierDefaultPadding) // must be after `clickable`
    )

    if (refreshing) LinearProgressIndicator(modifierMaxWidth)

    ExpandCollapse(visible = expanded) {
        ChangelogContainer(
            refreshing = refreshing,
            updateData = updateData,
            isDifferentVersion = isDifferentVersion,
            showAdvancedModeTip = showAdvancedModeTip,
            getPrefStr = getPrefStr,
        )
    }
}

private const val FullZipLikelyMinSize = 1048576 * 2500L // 2.5 GB

private const val TAG = "UpToDateContent"

@VisibleForTesting
const val UpToDateContentTestTag = TAG

@VisibleForTesting
const val UpToDateContent_InfoColumnTestTag = TAG + "_InfoColumnTestTag"

@PreviewThemes
@Composable
fun PreviewUpToDate() = PreviewAppTheme {
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    UpToDate(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        refreshing = false,
        updateData = PreviewUpdateData,
        getPrefStr = PreviewGetPrefStr,
    )
}
