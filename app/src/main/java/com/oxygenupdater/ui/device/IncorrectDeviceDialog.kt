package com.oxygenupdater.ui.device

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.ui.common.CheckboxText
import com.oxygenupdater.ui.dialogs.AlertDialog
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun IncorrectDeviceDialog(
    hide: (Boolean) -> Unit,
    mismatchStatus: Triple<Boolean, String, String>,
) {
    var ignore by remember { mutableStateOf(false) }
    var show by remember(mismatchStatus) { mutableStateOf(mismatchStatus.first) }
    if (show) AlertDialog(
        action = {
            hide(ignore)
            show = false
        },
        titleResId = R.string.incorrect_device_warning_title,
        text = stringResource(R.string.incorrect_device_warning_message, mismatchStatus.second, mismatchStatus.third),
    ) {
        CheckboxText(
            checked = ignore, onCheckedChange = { ignore = it },
            textResId = R.string.do_not_show_again_checkbox,
            textColor = AlertDialogDefaults.textContentColor.copy(alpha = 0.87f),
            modifier = Modifier.offset((-12).dp) // bring in line with Text
        )
    }
}

@PreviewThemes
@Composable
fun PreviewIncorrectDeviceDialog() = PreviewAppTheme {
    IncorrectDeviceDialog(
        hide = {},
        mismatchStatus = Triple(true, "OnePlus 7 Pro", "OnePlus 8T"),
    )
}
