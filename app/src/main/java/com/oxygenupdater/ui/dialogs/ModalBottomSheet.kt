package com.oxygenupdater.ui.dialogs

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
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ui.common.ItemDivider
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.backgroundVariant

/**
 * [hide] is automatically called when user presses back button.
 * See [androidx.compose.material3.ModalBottomSheetWindow.dispatchKeyEvent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NonRestartableComposable
fun ModalBottomSheet(hide: () -> Unit, content: @Composable ColumnScope.(() -> Unit) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    // TODO(compose/sheet): move common layouts out of *Sheet
    // TODO(compose/bug): content behind sheet not clickable if dismissed via swipe
    ModalBottomSheet(
        hide,
        sheetState = rememberModalBottomSheetState(true),
        containerColor = colorScheme.backgroundVariant, // match bottom nav bar
        contentColor = colorScheme.onSurface,
        windowInsets = WindowInsets.navigationBars, // draw behind status bar only, not navigation
        content = { content(hide) }
    )
}

@Composable
@NonRestartableComposable
fun SheetHeader(@StringRes titleResId: Int) = Text(
    stringResource(titleResId),
    Modifier.padding(start = 16.dp, end = 4.dp, bottom = 16.dp),
    MaterialTheme.colorScheme.primary,
    overflow = TextOverflow.Ellipsis, maxLines = 1,
    style = MaterialTheme.typography.titleMedium
)

@Composable
@NonRestartableComposable
fun SheetCaption(@StringRes captionResId: Int) {
    ItemDivider()
    Text(
        stringResource(captionResId),
        Modifier.padding(16.dp),
        MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewModalBottomSheet(content: @Composable ColumnScope.() -> Unit) = PreviewAppTheme {
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = SheetState(true, LocalDensity.current, SheetValue.Expanded),
        content = content
    )
}
