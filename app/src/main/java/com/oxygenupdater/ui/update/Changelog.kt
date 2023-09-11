package com.oxygenupdater.ui.update

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.RichTextType

@Composable
fun UpdateData.Changelog(
    modifier: Modifier = Modifier,
    extraTextPadding: Boolean = false,
) = if (!changelog.isNullOrBlank() && !description.isNullOrBlank()) RichText(
    description, modifier, type = RichTextType.Markdown
) else Text(
    stringResource(R.string.update_information_description_not_available),
    if (extraTextPadding) modifier.padding(top = 16.dp) else modifier,
    style = MaterialTheme.typography.bodyMedium
)
