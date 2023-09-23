package com.oxygenupdater.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.RichTextType
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.utils.UpdateDataVersionFormatter

@Composable
fun ChangelogContainer(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    updateData: UpdateData,
    isDifferentVersion: Boolean,
    showAdvancedModeTip: Boolean,
    content: (@Composable () -> Unit)? = null,
) = Column(modifier) {
    if (isDifferentVersion) {
        val bodySmall = MaterialTheme.typography.bodySmall
        Text(
            stringResource(
                R.string.update_information_different_version_changelog_notice_base,
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData),
                PrefManager.getString(PrefManager.KeyUpdateMethod, "<UNKNOWN>") ?: "<UNKNOWN>"
            ) + if (showAdvancedModeTip) stringResource(R.string.update_information_different_version_changelog_notice_advanced) else "",
            Modifier
                .padding(start = 20.dp, end = 16.dp, bottom = 8.dp)
                .withPlaceholder(refreshing, bodySmall),
            MaterialTheme.colorScheme.onSurfaceVariant,
            style = bodySmall
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
        description, modifierWithPlaceholder, type = RichTextType.Markdown
    ) else Text(
        stringResource(R.string.update_information_description_not_available),
        if (extraTextPadding) modifierWithPlaceholder.padding(top = 16.dp) else modifierWithPlaceholder,
        style = MaterialTheme.typography.bodyMedium
    )
}
