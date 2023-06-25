package com.oxygenupdater.compose.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.ListItemTextIndent
import com.oxygenupdater.compose.ui.common.OutlinedIconButton
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.compose.ui.theme.positive

@Composable
fun ColumnScope.AdvancedModeSheet(
    hide: () -> Unit,
    onClick: (Boolean) -> Unit,
) {
    SheetHeader(R.string.settings_advanced_mode, hide)

    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
    ) {
        val body2 = MaterialTheme.typography.body2
        Text(
            stringResource(R.string.settings_advanced_mode_explanation),
            style = body2
        )

        Text(
            AnnotatedString(
                stringResource(R.string.settings_advanced_mode_uses),
                body2.toSpanStyle(),
                body2.toParagraphStyle().copy(textIndent = ListItemTextIndent)
            ),
            style = body2
        )
    }

    Text(
        stringResource(R.string.settings_advanced_mode_caption),
        Modifier
            .alpha(ContentAlpha.medium)
            .padding(horizontal = 16.dp),
        style = MaterialTheme.typography.caption
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
            hide()
        }, Modifier.padding(end = 8.dp)) {
            Text(stringResource(android.R.string.cancel))
        }

        OutlinedIconButton({
            onClick(true)
            hide()
        }, Icons.Rounded.CheckCircleOutline, R.string.enable, MaterialTheme.colors.positive)
    }
}

@PreviewThemes
@Composable
fun PreviewAdvancedModeSheet() = PreviewModalBottomSheet {
    AdvancedModeSheet(hide = {}) {}
}
