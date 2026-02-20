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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.icons.Error
import com.oxygenupdater.icons.FullCoverage
import com.oxygenupdater.icons.Refresh
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.PreviewWindowSize

@Composable
fun ErrorState(
    navType: NavType,
    @StringRes titleResId: Int,
    icon: ImageVector = Symbols.Error,
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
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        ) else Text(
            text = stringResource(textResId),
            textAlign = TextAlign.Justify,
            modifier = Modifier
                .padding(top = 4.dp, bottom = 16.dp)
                .testTag(ErrorState_TextTestTag)
        )

        if (onRefreshClick != null) IconTextButton(
            onClick = onRefreshClick,
            icon = Symbols.Refresh,
            textResId = R.string.download_error_retry,
        )
        ConditionalNavBarPadding(navType)
    }
} else Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifierMaxSize.testTag(ErrorStateTestTag)
) {
    // Referential equality is faster, we only check for the default param value
    val error = icon === Symbols.Error

    Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.icon),
        tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .requiredSize(150.dp)
            .testTag(ErrorState_IconTestTag)
    )

    Text(
        text = stringResource(titleResId),
        color = if (error) MaterialTheme.colorScheme.error else Color.Unspecified,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifierDefaultPadding.testTag(ErrorState_TitleTestTag)
    )

    if (rich) RichText(
        text = stringResource(textResId),
        textAlign = TextAlign.Justify,
        modifier = modifierDefaultPadding
    ) else Text(
        text = stringResource(textResId),
        textAlign = TextAlign.Justify,
        modifier = modifierDefaultPadding.testTag(ErrorState_TextTestTag)
    )

    if (onRefreshClick != null) IconTextButton(
        onClick = onRefreshClick,
        icon = Symbols.Refresh,
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

@PreviewThemes
@Composable
fun PreviewErrorStateUpdateScreen() = PreviewAppTheme {
    ErrorState(
        navType = NavType.from(PreviewWindowSize.widthSizeClass),
        titleResId = R.string.update_information_error_title,
        onRefreshClick = {},
    )
}

@PreviewThemes
@Composable
fun PreviewErrorStateNewsListScreen1() = PreviewAppTheme {
    ErrorState(
        navType = NavType.from(PreviewWindowSize.widthSizeClass),
        titleResId = R.string.news_empty_state_all_read_header,
        icon = Symbols.FullCoverage,
        textResId = R.string.news_empty_state_all_read_text,
        rich = false,
        onRefreshClick = null,
    )
}

@PreviewThemes
@Composable
fun PreviewErrorStateNewsListScreen2() = PreviewAppTheme {
    ErrorState(
        navType = NavType.from(PreviewWindowSize.widthSizeClass),
        titleResId = R.string.news_empty_state_none_available_header,
        icon = Symbols.FullCoverage,
        textResId = R.string.news_empty_state_none_available_text,
        rich = false,
        onRefreshClick = {},
    )
}
