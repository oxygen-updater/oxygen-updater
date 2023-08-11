package com.oxygenupdater.compose.ui.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheet(
    hide: () -> Unit,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit,
) = ModalBottomSheet(
    hide,
    sheetState = sheetState,
    windowInsets = WindowInsets.navigationBars, // allow scrim over status bar
    content = content
)

@Composable
fun SheetHeader(
    @StringRes titleResId: Int,
) = Text(
    stringResource(titleResId),
    Modifier.padding(start = 16.dp, end = 4.dp, bottom = 16.dp),
    MaterialTheme.colorScheme.primary,
    overflow = TextOverflow.Ellipsis, maxLines = 1,
    style = MaterialTheme.typography.titleMedium
)

@Composable
fun SheetCaption(@StringRes captionResId: Int) {
    ItemDivider()
    Text(
        stringResource(captionResId),
        Modifier.padding(16.dp),
        MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun defaultModalBottomSheetState() = rememberModalBottomSheetState(true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewModalBottomSheet(
    content: @Composable ColumnScope.() -> Unit,
) = PreviewAppTheme {
    val density = LocalDensity.current
    ModalBottomSheet({}, remember {
        SheetState(true, density, SheetValue.Expanded)
    }, content = content)
}
