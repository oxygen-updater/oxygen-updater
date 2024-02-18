package com.oxygenupdater.ui.update

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.RichTextType
import com.oxygenupdater.ui.common.modifierDefaultPaddingTop
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.utils.UpdateDataVersionFormatter

@Composable
fun ChangelogContainer(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    updateData: UpdateData,
    isDifferentVersion: Boolean,
    showAdvancedModeTip: Boolean,
    getPrefStr: (key: String, default: String) -> String,
    content: (@Composable () -> Unit)? = null,
) = Column(modifier.testTag(Changelog_ContainerTestTag)) {
    if (isDifferentVersion) {
        val bodySmall = MaterialTheme.typography.bodySmall
        Text(
            text = stringResource(
                R.string.update_information_different_version_changelog_notice_base,
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData),
                getPrefStr(KeyUpdateMethod, "<UNKNOWN>"),
            ) + if (showAdvancedModeTip) stringResource(R.string.update_information_different_version_changelog_notice_advanced) else "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = bodySmall,
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp, bottom = 8.dp)
                .withPlaceholder(refreshing, bodySmall)
                .testTag(Changelog_DifferentVersionNoticeTestTag)
        )
    }

    updateData.Changelog(
        refreshing = refreshing,
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, bottom = 16.dp)
    )

    content?.invoke()
}

@Composable
fun UpdateData.Changelog(
    refreshing: Boolean,
    extraTextPadding: Boolean = false,
    modifier: Modifier,
) {
    val modifierWithPlaceholder = modifier.withPlaceholder(refreshing, MaterialTheme.typography.bodyMedium)

    if (!changelog.isNullOrBlank() && !description.isNullOrBlank()) RichText(
        text = description,
        type = RichTextType.Markdown,
        modifier = modifierWithPlaceholder
    ) else Text(
        text = stringResource(R.string.update_information_description_not_available),
        style = MaterialTheme.typography.bodyMedium,
        modifier = (if (extraTextPadding) modifierWithPlaceholder then modifierDefaultPaddingTop else modifierWithPlaceholder)
            .testTag(Changelog_PlaceholderTestTag),
    )
}

private const val TAG = "Changelog"

@VisibleForTesting
const val Changelog_ContainerTestTag = TAG + "_Container"

@VisibleForTesting
const val Changelog_DifferentVersionNoticeTestTag = TAG + "_DifferentVersionNotice"

@VisibleForTesting
const val Changelog_PlaceholderTestTag = TAG + "_Placeholder"
