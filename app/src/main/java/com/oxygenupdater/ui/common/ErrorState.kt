package com.oxygenupdater.ui.common

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.testTag
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
    onRefreshClick: (() -> Unit)?,
) = if (navType != NavType.BottomBar) Row(modifierMaxWidth.testTag(ErrorStateTestTag)) {
    Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.icon),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .requiredSize(192.dp)
            .testTag(ErrorState_IconTestTag)
    )

    Column(modifierDefaultPadding.verticalScroll(rememberScrollState())) {
        Text(
            text = stringResource(titleResId),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag(ErrorState_TitleTestTag)
        )

        if (rich) RichText(
            text = stringResource(textResId),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        ) else Text(
            text = stringResource(textResId),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 16.dp)
                .testTag(ErrorState_TextTestTag)
        )

        if (onRefreshClick != null) OutlinedIconButton(
            onClick = onRefreshClick,
            icon = Icons.Rounded.Refresh,
            textResId = R.string.download_error_retry,
        )
        ConditionalNavBarPadding(navType)
    }
} else Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifierMaxSize.testTag(ErrorStateTestTag)
) {
    Text(
        text = stringResource(titleResId),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifierDefaultPadding.testTag(ErrorState_TitleTestTag)
    )

    Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.icon),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .requiredSize(150.dp)
            .testTag(ErrorState_IconTestTag)
    )

    if (rich) RichText(
        text = stringResource(textResId),
        textAlign = TextAlign.Center,
        modifier = modifierDefaultPadding
    ) else Text(
        text = stringResource(textResId),
        textAlign = TextAlign.Center,
        modifier = modifierDefaultPadding.testTag(ErrorState_TextTestTag)
    )

    if (onRefreshClick != null) OutlinedIconButton(
        onClick = onRefreshClick,
        icon = Icons.Rounded.Refresh,
        textResId = R.string.download_error_retry,
    )
    ConditionalNavBarPadding(navType)
}

private const val TAG = "ErrorState"

@VisibleForTesting
const val ErrorStateTestTag = TAG

@VisibleForTesting
const val ErrorState_IconTestTag = TAG + "_Icon"

@VisibleForTesting
const val ErrorState_TitleTestTag = TAG + "_Title"

@VisibleForTesting
const val ErrorState_TextTestTag = TAG + "_Text"

@PreviewThemes
@Composable
fun PreviewErrorState() = PreviewAppTheme {
    ErrorState(
        navType = NavType.from(PreviewWindowSize.widthSizeClass),
        titleResId = R.string.error_maintenance,
        onRefreshClick = {},
    )
}
