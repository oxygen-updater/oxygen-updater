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
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.ui.common.CheckboxText
import com.oxygenupdater.ui.dialogs.AlertDialog
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun IncorrectDeviceDialog(mismatchStatus: Triple<Boolean, String, String>) {
    var ignore by remember { mutableStateOf(false) }
    var show by remember(mismatchStatus) { mutableStateOf(mismatchStatus.first) }
    if (show) AlertDialog(
        {
            PrefManager.putBoolean(PrefManager.PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS, ignore)
            show = false
        },
        R.string.incorrect_device_warning_title,
        stringResource(R.string.incorrect_device_warning_message, mismatchStatus.second, mismatchStatus.third),
    ) {
        CheckboxText(
            ignore, { ignore = it },
            R.string.device_warning_checkbox_title,
            Modifier.offset((-12).dp), // bring in line with Text
            textColor = AlertDialogDefaults.textContentColor.copy(alpha = 0.87f),
        )
    }
}

@PreviewThemes
@Composable
fun PreviewIncorrectDeviceDialog() = PreviewAppTheme {
    IncorrectDeviceDialog(
        Triple(true, "OnePlus 7 Pro", "OnePlus 8T")
    )
}
