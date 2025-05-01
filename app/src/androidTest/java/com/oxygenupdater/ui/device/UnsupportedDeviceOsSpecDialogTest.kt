package com.oxygenupdater.ui.device

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onParent
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.get
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.ui.dialogs.AlertDialogTestTag
import com.oxygenupdater.ui.dialogs.AlertDialog_TextTestTag
import org.junit.Test

class UnsupportedDeviceOsSpecDialogTest : ComposeBaseTest() {

    @Test
    fun unsupportedDeviceOsSpecDialog() {
        var show by mutableStateOf(false)
        var spec by mutableStateOf(DeviceOsSpec.SupportedDeviceAndOs)
        setContent {
            UnsupportedDeviceOsSpecDialog(
                show = show,
                hide = { show = it },
                spec = spec,
            )
        }

        // First we test for the initial value of spec (supported)
        rule[AlertDialogTestTag].assertDoesNotExist()
        show = true // should be shown now?
        // No, because spec is still "supported"
        rule[AlertDialogTestTag].assertDoesNotExist()

        // Then for carrier-exclusive OxygenOS devices
        spec = DeviceOsSpec.CarrierExclusiveOxygenOs
        validateForNotSupportedSpecs(R.string.carrier_exclusive_device_warning_message)

        // Then for new devices we don't yet support
        spec = DeviceOsSpec.UnsupportedDevice
        validateForNotSupportedSpecs(R.string.unsupported_device_warning_message, Build.BRAND)

        // Then for devices not running OxygenOS/ColorOS
        spec = DeviceOsSpec.UnsupportedDeviceAndOs
        validateForNotSupportedSpecs(R.string.unsupported_os_warning_message)
    }

    private fun validateForNotSupportedSpecs(
        @StringRes warningMessageResId: Int,
        vararg formatArgs: String,
    ) {
        rule[AlertDialogTestTag].run {
            assertExists()
            onParent().assert(isDialog())
        }

        rule[AlertDialog_TextTestTag].assertHasTextExactly(
            if (formatArgs.isEmpty()) warningMessageResId else activity.getString(warningMessageResId, *formatArgs)
        )
    }
}
