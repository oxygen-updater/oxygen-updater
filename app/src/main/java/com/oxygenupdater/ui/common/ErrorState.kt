package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun ErrorState(
    title: String,
    onClick: () -> Unit,
) = Column(Modifier.fillMaxHeight(), Arrangement.Center, Alignment.CenterHorizontally) {
    Text(
        title,
        Modifier.padding(16.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge
    )
    Icon(
        Icons.Rounded.ErrorOutline, stringResource(R.string.icon),
        Modifier.requiredSize(150.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    RichText(
        stringResource(R.string.error_maintenance_retry),
        Modifier.padding(16.dp),
        textAlign = TextAlign.Center,
    )

    OutlinedIconButton(onClick, Icons.Rounded.Refresh, textResId = R.string.download_error_retry)
}

@PreviewThemes
@Composable
fun PreviewErrorState() = PreviewAppTheme {
    ErrorState("Error title", onClick = {})
}
