package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.ListItemTextIndent
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun ColumnScope.AdvancedModeSheet(onClick: (Boolean) -> Unit) {
    SheetHeader(R.string.settings_advanced_mode)

    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
    ) {
        val bodyMedium = MaterialTheme.typography.bodyMedium
        Text(
            stringResource(R.string.settings_advanced_mode_explanation),
            style = bodyMedium
        )

        Text(
            AnnotatedString(
                stringResource(R.string.settings_advanced_mode_uses),
                bodyMedium.toSpanStyle(),
                bodyMedium.toParagraphStyle().copy(textIndent = ListItemTextIndent)
            ),
            style = bodyMedium
        )
    }

    val colorScheme = MaterialTheme.colorScheme
    Text(
        stringResource(R.string.settings_advanced_mode_caption),
        Modifier.padding(horizontal = 16.dp),
        colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton({
            onClick(false)
        }, Modifier.padding(end = 8.dp), colors = textButtonColors(contentColor = colorScheme.error)) {
            Text(stringResource(android.R.string.cancel))
        }

        OutlinedIconButton({
            onClick(true)
        }, Icons.Rounded.CheckCircleOutline, R.string.enable)
    }
}

@PreviewThemes
@Composable
fun PreviewAdvancedModeSheet() = PreviewModalBottomSheet {
    AdvancedModeSheet {}
}
