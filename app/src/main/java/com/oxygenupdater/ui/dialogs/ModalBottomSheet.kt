package com.oxygenupdater.ui.dialogs

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ui.common.OutlinedIconButton
import com.oxygenupdater.ui.common.modifierDefaultPadding
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.theme.PreviewAppTheme

/**
 * [hide] is automatically called when user presses back button.
 * See [androidx.compose.material3.ModalBottomSheetWindow.dispatchKeyEvent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NonRestartableComposable
fun ModalBottomSheet(
    hide: () -> Unit,
    content: @Composable ColumnScope.(() -> Unit) -> Unit,
) = ModalBottomSheet(
    onDismissRequest = hide,
    sheetState = rememberModalBottomSheetState(true),
    modifier = Modifier.testTag(BottomSheetTestTag)
) { content(hide) }

@Composable
@NonRestartableComposable
fun SheetHeader(@StringRes titleResId: Int) = Text(
    text = stringResource(titleResId),
    color = MaterialTheme.colorScheme.primary,
    maxLines = 1,
    style = MaterialTheme.typography.titleMedium,
    modifier = Modifier
        .basicMarquee()
        .padding(start = 16.dp, end = 4.dp, bottom = 16.dp)
        .testTag(BottomSheet_HeaderTestTag)
)

@Composable
fun SheetButtons(
    @StringRes dismissResId: Int,
    onDismiss: () -> Unit,
    confirmIcon: ImageVector,
    @StringRes confirmResId: Int,
    onConfirm: (() -> Unit)?,
    modifier: Modifier = Modifier,
) = Row(
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
    modifier = (modifier then modifierMaxWidth).padding(
        start = 16.dp,
        top = 12.dp,
        end = 16.dp,
        bottom = 16.dp,
    )
) {
    TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
            .padding(end = 8.dp)
            .testTag(BottomSheet_DismissButtonTestTag),
    ) {
        Text(stringResource(dismissResId))
    }

    if (onConfirm != null) OutlinedIconButton(
        onClick = onConfirm,
        icon = confirmIcon,
        textResId = confirmResId,
    )
}

@Composable
@NonRestartableComposable
fun SheetCaption(@StringRes captionResId: Int) {
    HorizontalDivider()
    Text(
        text = stringResource(captionResId),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifierDefaultPadding.testTag(BottomSheet_CaptionTestTag)
    )
}

private const val TAG = "ModalBottomSheet"

@VisibleForTesting
const val BottomSheetTestTag = TAG

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_HeaderTestTag = TAG + "_Header"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_ContentTestTag = TAG + "_Content"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_CaptionTestTag = TAG + "_Caption"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_DismissButtonTestTag = TAG + "_DismissButton"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_ItemRowTestTag = TAG + "_ItemRow"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewModalBottomSheet(content: @Composable ColumnScope.() -> Unit) = PreviewAppTheme {
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = SheetState(true, LocalDensity.current, SheetValue.Expanded),
        content = content
    )
}
