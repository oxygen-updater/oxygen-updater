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
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.ui.common.CheckboxText
import com.oxygenupdater.ui.dialogs.AlertDialog
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun UnsupportedDeviceOsSpecDialog(show: Boolean, hide: () -> Unit, spec: DeviceOsSpec) {
    if (spec == DeviceOsSpec.SupportedOxygenOs) return

    var ignore by remember { mutableStateOf(false) }
    if (show) AlertDialog({
        PrefManager.putBoolean(PrefManager.KeyIgnoreUnsupportedDeviceWarnings, ignore)
        hide()
    }, R.string.unsupported_device_warning_title, stringResource(remember(spec) {
        when (spec) {
            DeviceOsSpec.CarrierExclusiveOxygenOs -> R.string.carrier_exclusive_device_warning_message
            DeviceOsSpec.UnsupportedOxygenOs -> R.string.unsupported_device_warning_message
            DeviceOsSpec.UnsupportedOs -> R.string.unsupported_os_warning_message
            else -> R.string.unsupported_os_warning_message
        }
    })) {
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
fun PreviewCarrierExclusiveDialog() = PreviewAppTheme {
    UnsupportedDeviceOsSpecDialog(true, {}, DeviceOsSpec.CarrierExclusiveOxygenOs)
}

@PreviewThemes
@Composable
fun PreviewUnsupportedOxygenOsDialog() = PreviewAppTheme {
    UnsupportedDeviceOsSpecDialog(true, {}, DeviceOsSpec.UnsupportedOxygenOs)
}

@PreviewThemes
@Composable
fun PreviewUnsupportedOsDialog() = PreviewAppTheme {
    UnsupportedDeviceOsSpecDialog(true, {}, DeviceOsSpec.UnsupportedOs)
}
