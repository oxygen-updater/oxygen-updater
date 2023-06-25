package com.oxygenupdater.compose.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
fun ModalBottomSheet(
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: ModalBottomSheetState,
    content: @Composable () -> Unit,
) = ModalBottomSheetLayout({
    Box(
        Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(16.dp))
            .requiredSize(32.dp, 4.dp)
            .background(MaterialTheme.colors.onSurface.copy(alpha = .04f))
    )

    sheetContent()
}, sheetState = sheetState, sheetElevation = 0.dp, scrimColor = Color.Black.copy(alpha = .32f), content = content)

@Suppress("UnusedReceiverParameter")
@Composable
@NonRestartableComposable
fun ColumnScope.SheetHeader(
    @StringRes titleResId: Int,
    hide: () -> Unit,
) = Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(
        stringResource(titleResId),
        Modifier.weight(1f),
        color = MaterialTheme.colors.primary,
        overflow = TextOverflow.Ellipsis, maxLines = 1,
        style = MaterialTheme.typography.subtitle1
    )

    IconButton(hide) {
        Icon(Icons.Rounded.Close, stringResource(androidx.compose.ui.R.string.close_sheet))
    }
}

@Suppress("UnusedReceiverParameter")
@Composable
@NonRestartableComposable
fun ColumnScope.SheetCaption(@StringRes captionResId: Int) {
    ItemDivider()
    Text(
        stringResource(captionResId),
        Modifier
            .alpha(ContentAlpha.medium)
            .padding(16.dp),
        style = MaterialTheme.typography.caption
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
fun defaultModalBottomSheetState() = rememberModalBottomSheetState(
    ModalBottomSheetValue.Hidden, skipHalfExpanded = true
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PreviewModalBottomSheet(
    sheetContent: @Composable ColumnScope.() -> Unit,
) = PreviewAppTheme {
    ModalBottomSheet(sheetContent, rememberModalBottomSheetState(
        ModalBottomSheetValue.Expanded, skipHalfExpanded = true
    ), content = {})
}
