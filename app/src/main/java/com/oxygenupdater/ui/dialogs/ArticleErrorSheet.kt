package com.oxygenupdater.ui.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Autorenew
import com.oxygenupdater.icons.Symbols

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
        modifier = Modifier
            .padding(start = 16.dp, end = 8.dp)
            .testTag(BottomSheet_HeaderTestTag)
    )

    Text(
        text = stringResource(R.string.news_load_network_error),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
            .testTag(BottomSheet_ContentTestTag)
    )

    SheetButtons(
        dismissResId = R.string.download_error_close,
        onDismiss = hide,
        confirmIcon = Symbols.Autorenew,
        confirmResId = R.string.download_error_retry,
        onConfirm = { confirm(); hide() },
        modifier = Modifier.navigationBarsPadding()
    )
}
