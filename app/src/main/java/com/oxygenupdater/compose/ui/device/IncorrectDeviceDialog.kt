package com.oxygenupdater.compose.ui.device

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oxygenupdater.R
import com.oxygenupdater.compose.ui.common.CheckboxText
import com.oxygenupdater.compose.ui.dialogs.AlertDialog
import com.oxygenupdater.compose.ui.theme.PreviewAppTheme
import com.oxygenupdater.compose.ui.theme.PreviewThemes
import com.oxygenupdater.internal.settings.PrefManager

@Composable
fun IncorrectDeviceDialog(mismatchStatus: Triple<Boolean, String, String>) {
    val ignore = remember { mutableStateOf(false) }
    AlertDialog(
        remember(mismatchStatus) { mutableStateOf(mismatchStatus.first) },
        titleResId = R.string.incorrect_device_warning_title,
        text = stringResource(
            R.string.incorrect_device_warning_message,
            mismatchStatus.second,
            mismatchStatus.third
        ),
        content = {
            CheckboxText(
                ignore,
                R.string.device_warning_checkbox_title,
                Modifier
                    .padding(top = 8.dp)
                    .offset((-12).dp), // bring in line with Text
                textColor = AlertDialogDefaults.textContentColor.copy(alpha = 0.87f),
            )
        },
    ) {
        PrefManager.putBoolean(PrefManager.PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS, ignore.value)
    }
}

@PreviewThemes
@Composable
fun PreviewIncorrectDeviceDialog() = PreviewAppTheme {
    IncorrectDeviceDialog(
        Triple(true, "OnePlus 7 Pro", "OnePlus 8T")
    )
}
