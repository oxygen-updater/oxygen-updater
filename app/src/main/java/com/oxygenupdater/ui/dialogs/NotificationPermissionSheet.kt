package com.oxygenupdater.ui.dialogs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.CheckboxText
import com.oxygenupdater.ui.theme.PreviewThemes

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ColumnScope.NotificationPermissionSheet(
    hide: (Boolean) -> Unit,
    launchPermissionRequest: () -> Unit,
) {
    SheetHeader(R.string.notification_permission_title)

    Text(
        text = stringResource(R.string.notification_permission_text),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
            .testTag(BottomSheet_ContentTestTag)
    )

    var ignore by remember { mutableStateOf(false) }
    CheckboxText(
        checked = ignore, onCheckedChange = { ignore = it },
        textResId = R.string.do_not_show_again_checkbox,
        textColor = AlertDialogDefaults.textContentColor.copy(alpha = 0.87f),
        modifier = Modifier.offset(4.dp) // bring in line with Text
    )

    SheetButtons(
        dismissResId = android.R.string.cancel,
        onDismiss = { hide(ignore) },
        confirmIcon = Icons.Rounded.NotificationsNone,
        confirmResId = android.R.string.ok,
        /** Don't hide sheet here, [com.oxygenupdater.ui.main.NotificationPermission] will do it once granted */
        onConfirm = launchPermissionRequest,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@PreviewThemes
@Composable
fun PreviewNotificationPermissionSheet() = PreviewModalBottomSheet {
    NotificationPermissionSheet(hide = {}, launchPermissionRequest = {})
}
