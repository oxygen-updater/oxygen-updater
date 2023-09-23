package com.oxygenupdater.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize

@Composable
fun ErrorState(
    navType: NavType,
    @StringRes titleResId: Int,
    icon: ImageVector = Icons.Rounded.ErrorOutline,
    @StringRes textResId: Int = R.string.error_maintenance_retry,
    rich: Boolean = true,
    refresh: (() -> Unit)?,
) = if (navType != NavType.BottomBar) Row(Modifier.fillMaxWidth()) {
    Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.icon),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.requiredSize(192.dp),
    )

    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(titleResId), style = MaterialTheme.typography.titleLarge)

        if (rich) RichText(stringResource(textResId), Modifier.padding(top = 4.dp, bottom = 16.dp))
        else Text(stringResource(textResId), Modifier.padding(top = 4.dp, bottom = 16.dp))

        if (refresh != null) OutlinedIconButton(refresh, Icons.Rounded.Refresh, R.string.download_error_retry)
        ConditionalNavBarPadding(navType)
    }
} else Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxSize()
) {
    Text(
        stringResource(titleResId),
        Modifier.padding(16.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge
    )

    Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.icon),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.requiredSize(150.dp),
    )

    if (rich) RichText(stringResource(textResId), Modifier.padding(16.dp), textAlign = TextAlign.Center)
    else RichText(stringResource(textResId), Modifier.padding(16.dp), textAlign = TextAlign.Center)

    if (refresh != null) OutlinedIconButton(refresh, Icons.Rounded.Refresh, R.string.download_error_retry)
    ConditionalNavBarPadding(navType)
}

@PreviewThemes
@Composable
fun PreviewErrorState() = PreviewAppTheme {
    ErrorState(
        navType = NavType.from(PreviewWindowSize.widthSizeClass),
        titleResId = R.string.error_maintenance,
        refresh = {},
    )
}
