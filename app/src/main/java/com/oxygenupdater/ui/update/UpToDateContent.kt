package com.oxygenupdater.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Info
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.common.IconText
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.device.DeviceSoftwareInfo
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.positive
import com.oxygenupdater.utils.UpdateDataVersionFormatter

@Composable
fun UpToDate(
    refreshing: Boolean,
    updateData: UpdateData,
) = Column(
    Modifier
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
) {
    val currentOtaVersion = SystemVersionProperties.oxygenOSOTAVersion
    val method = PrefManager.getString(PrefManager.KeyUpdateMethod, "") ?: ""

    val incomingOtaVersion = updateData.otaVersionNumber
    val isDifferentVersion = incomingOtaVersion != currentOtaVersion
    val showAdvancedModeTip = isDifferentVersion // show advanced mode hint only if OTA versions don't match…
            // …and incoming is newer (older builds can't be installed due to standard Android security measures)
            && UpdateData.getBuildDate(incomingOtaVersion) >= UpdateData.getBuildDate(currentOtaVersion)
            // …and incoming is likely a full ZIP, or the selected update method is "full"
            // (incrementals are only for specific source/target version combos)
            && (updateData.downloadSize >= FullZipLikelyMinSize || method.endsWith("(full)") || method.endsWith("(volledig)"))
    if (showAdvancedModeTip) {
        IconText(
            Modifier.padding(16.dp),
            icon = CustomIcons.Info,
            text = stringResource(R.string.update_information_banner_advanced_mode_tip)
        )
        ItemDivider()
    }

    Box(Modifier.fillMaxWidth()) {
        val positive = MaterialTheme.colorScheme.positive
        IconText(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            Modifier.withPlaceholder(refreshing),
            icon = Icons.Rounded.CheckCircleOutline,
            text = stringResource(R.string.update_information_system_is_up_to_date),
            iconTint = positive,
            style = MaterialTheme.typography.titleMedium.copy(color = positive)
        )

        Icon(
            Icons.Rounded.DoneAll, stringResource(R.string.icon),
            Modifier
                .graphicsLayer(scaleX = 2f, scaleY = 2f, alpha = .1f)
                .align(Alignment.CenterEnd)
                .requiredSize(64.dp)
        )
    }

    ItemDivider(Modifier.padding(top = 2.dp))
    DeviceSoftwareInfo(false)
    ItemDivider(Modifier.padding(top = 16.dp))

    ExpandableChangelog(refreshing, updateData, isDifferentVersion, showAdvancedModeTip)

    ItemDivider()
}

@Composable
private fun ExpandableChangelog(
    refreshing: Boolean,
    updateData: UpdateData,
    isDifferentVersion: Boolean,
    showAdvancedModeTip: Boolean,
) {
    val expandEnabled = updateData.isUpdateInformationAvailable
    var expanded by rememberSaveableState("changelogExpanded", LocalInspectionMode.current)
    IconText(
        Modifier
            .fillMaxWidth()
            .alpha(if (!refreshing) 1f else 0.38f)
            .animatedClickable(!refreshing && expandEnabled) { expanded = !expanded }
            .padding(16.dp), // must be after `clickable`
        icon = if (!expandEnabled) Icons.Rounded.ErrorOutline else if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
        text = stringResource(
            if (!expandEnabled) R.string.update_information_no_update_data_available
            else R.string.update_information_view_update_information
        ),
        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary)
    )

    if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())

    AnimatedVisibility(
        expanded,
        enter = remember {
            expandVertically(
                spring(visibilityThreshold = IntSize.VisibilityThreshold)
            ) + fadeIn(initialAlpha = .3f)
        },
        exit = remember {
            shrinkVertically(
                spring(visibilityThreshold = IntSize.VisibilityThreshold)
            ) + fadeOut()
        },
    ) {
        val changelogModifier = Modifier
            .padding(start = 20.dp, end = 16.dp, bottom = 16.dp)
            .withPlaceholder(refreshing)
        if (isDifferentVersion) Column {
            Text(
                stringResource(
                    R.string.update_information_different_version_changelog_notice_base,
                    UpdateDataVersionFormatter.getFormattedVersionNumber(updateData),
                    PrefManager.getString(PrefManager.KeyUpdateMethod, "<UNKNOWN>") ?: "<UNKNOWN>"
                ) + if (showAdvancedModeTip) stringResource(R.string.update_information_different_version_changelog_notice_advanced) else "",
                Modifier
                    .padding(start = 20.dp, end = 16.dp, bottom = 8.dp)
                    .withPlaceholder(refreshing),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            updateData.Changelog(changelogModifier)
        } else updateData.Changelog(changelogModifier)
    }
}

@PreviewThemes
@Composable
fun PreviewUpToDate() = PreviewAppTheme {
    UpToDate(
        refreshing = false,
        updateData = """##Personalization
• Expands Omoji's functionality and library

##Health
• Adds a new TalkBack feature that recognizes and announces images in apps and Photos
• Adds the new Zen Space app, with two modes, Deep Zen and Light Zen, to help you focus on the present
• Improves Simple mode with a new helper widget and quick tutorials on the Home screen

##Gaming experience
• Adds the Championship mode to Game Assistant. This mode improves performance while also disabling notifications, calls, and other messages to give you a more immersive gaming experience
• Adds a music playback control to Game Assistant, so you can listen to and control music easily while gaming""".let { changelog ->
            UpdateData(
                id = 1,
                versionNumber = "KB2001_11_F.66",
                otaVersionNumber = "KB2001_11.F.66_2660_202305041648",
                changelog = changelog,
                description = """#KB2001_13.1.0.513(EX01)
##2023-06-10

A system update is available. The OxygenOS 13.1 update brings new Zen Space features, a new TalkBack feature to describe images, and better gaming performance and experience.

""" + changelog,
                downloadUrl = "https://gauss-componentotacostmanual-in.allawnofs.com/remove-a7779e2dc9b4b40458be6db38b226089/component-ota/23/03/15/4b70c7244ce7411994c97313e8ceb82d.zip",
                downloadSize = 4777312256,
                filename = "4b70c7244ce7411994c97313e8ceb82d.zip",
                md5sum = "0dc48e34ca895ae5653a32ef4daf2933",
                updateInformationAvailable = false,
                systemIsUpToDate = true,
            )
        },
    )
}

private const val FullZipLikelyMinSize = 1048576 * 2500L // 2.5 GB
