package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.common.modifierMaxWidth

@Composable
fun ColumnScope.ArticleErrorSheet(
    hide: () -> Unit,
    title: String,
    confirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = title,
        color = colorScheme.primary,
        overflow = TextOverflow.Ellipsis, maxLines = 1,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 8.dp)
    )

    Text(
        text = stringResource(R.string.news_load_network_error),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
    )

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .navigationBarsPadding()
            .then(modifierMaxWidth)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        TextButton(
            onClick = hide,
            colors = textButtonColors(contentColor = colorScheme.error),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(stringResource(R.string.download_error_close))
        }

        OutlinedIconButton(
            onClick = {
                confirm()
                hide()
            },
            icon = Icons.Rounded.Autorenew,
            textResId = R.string.download_error_retry,
        )
    }
}
