package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun ColumnScope.ArticleErrorSheet(
    hide: () -> Unit,
    title: String,
    confirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        title,
        Modifier.padding(start = 16.dp, end = 8.dp),
        color = colorScheme.primary,
        overflow = TextOverflow.Ellipsis, maxLines = 1,
        style = MaterialTheme.typography.titleMedium
    )

    Text(
        stringResource(R.string.news_load_network_error),
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState()),
        style = MaterialTheme.typography.bodyMedium
    )

    Row(
        Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            hide,
            Modifier.padding(end = 8.dp),
            colors = textButtonColors(contentColor = colorScheme.error)
        ) {
            Text(stringResource(R.string.download_error_close))
        }

        OutlinedIconButton({
            confirm()
            hide()
        }, Icons.Rounded.Autorenew, R.string.download_error_retry)
    }
}
