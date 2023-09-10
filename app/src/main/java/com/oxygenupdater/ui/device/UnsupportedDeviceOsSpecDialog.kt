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
fun UnsupportedDeviceOsSpecDialog(spec: DeviceOsSpec) {
    var ignore by remember { mutableStateOf(false) }
    var show by remember(spec) { mutableStateOf(spec != DeviceOsSpec.SUPPORTED_OXYGEN_OS) }
    if (show) AlertDialog({
        PrefManager.putBoolean(PrefManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, ignore)
        show = false
    }, R.string.unsupported_device_warning_title, stringResource(remember(spec) {
        when (spec) {
            DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS -> R.string.carrier_exclusive_device_warning_message
            DeviceOsSpec.UNSUPPORTED_OXYGEN_OS -> R.string.unsupported_device_warning_message
            DeviceOsSpec.UNSUPPORTED_OS -> R.string.unsupported_os_warning_message
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
    UnsupportedDeviceOsSpecDialog(DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS)
}

@PreviewThemes
@Composable
fun PreviewUnsupportedOxygenOsDialog() = PreviewAppTheme {
    UnsupportedDeviceOsSpecDialog(DeviceOsSpec.UNSUPPORTED_OXYGEN_OS)
}

@PreviewThemes
@Composable
fun PreviewUnsupportedOsDialog() = PreviewAppTheme {
    UnsupportedDeviceOsSpecDialog(DeviceOsSpec.UNSUPPORTED_OS)
}
