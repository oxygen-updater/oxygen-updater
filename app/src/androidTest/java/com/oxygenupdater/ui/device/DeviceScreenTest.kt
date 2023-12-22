package com.oxygenupdater.ui.device

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.ui.dialogs.AlertDialogTestTag
import com.oxygenupdater.ui.main.NavType
import org.junit.Before
import org.junit.Test

class DeviceScreenTest : ComposeBaseTest() {

    private var spec by mutableStateOf(DeviceOsSpec.SupportedOxygenOs)
    private var mismatchStatus by mutableStateOf<Triple<Boolean, String, String>?>(null)

    @Before
    fun setup() {
        spec = DeviceOsSpec.SupportedOxygenOs
        mismatchStatus = null
    }

    @Test
    fun deviceScreen_expanded() = common(WindowWidthSizeClass.Expanded)

    @Test
    fun deviceScreen_compact() = common(WindowWidthSizeClass.Compact)

    private fun common(windowWidthSize: WindowWidthSizeClass) {
        setContent {
            DeviceScreen(
                navType = NavType.from(windowWidthSize),
                windowWidthSize = windowWidthSize,
                deviceName = DefaultDeviceName,
                deviceOsSpec = spec,
                deviceMismatchStatus = mismatchStatus,
            )
        }

        rule[DeviceScreen_ScrollableColumnTestTag].assertHasScrollAction()

        rule[DeviceScreen_NameTestTag].assertHasTextExactly(DefaultDeviceName)
        rule[DeviceScreen_ModelTestTag].run {
            isSelectable()
            assertHasTextExactly(Build.MODEL)
        }

        // First we test for the initial value of spec (supported)
        rule[DeviceScreen_NotSupportedIconTestTag].assertDoesNotExist()
        rule[DeviceScreen_ImageTestTag].run {
            assertExists()
            onParent().assertHasNoClickAction()
        }
        rule[AlertDialogTestTag].assertDoesNotExist()
        rule[DeviceScreen_SupportStatusTestTag].run {
            assertHasTextExactly(R.string.device_information_supported_oxygen_os)
        }

        // Then for carrier-exclusive OxygenOS devices
        spec = DeviceOsSpec.CarrierExclusiveOxygenOs
        validateForNotSupportedSpecs(R.string.device_information_carrier_exclusive_oxygen_os)

        // Then for new devices we don't yet support
        spec = DeviceOsSpec.UnsupportedOxygenOs
        validateForNotSupportedSpecs(R.string.device_information_unsupported_oxygen_os)

        // Then for devices not running OxygenOS
        spec = DeviceOsSpec.UnsupportedOs
        validateForNotSupportedSpecs(R.string.device_information_unsupported_os)

        // First we test for the initial null value of deviceMismatchStatus
        rule[DeviceScreen_MismatchTextTestTag].assertDoesNotExist()

        // Then for false
        mismatchStatus = Triple(false, DefaultDeviceName, DefaultDeviceName)
        rule[DeviceScreen_MismatchTextTestTag].assertDoesNotExist()

        // Then for true
        mismatchStatus = Triple(true, "incorrect", "correct")
        rule[DeviceScreen_MismatchTextTestTag].assertHasTextExactly(
            activity.getString(R.string.incorrect_device_warning_message, mismatchStatus!!.second, mismatchStatus!!.third),
        )
    }

    private fun validateForNotSupportedSpecs(@StringRes supportStatusResId: Int) {
        rule[DeviceScreen_NotSupportedIconTestTag, true].assertExists()
        rule[DeviceScreen_ImageTestTag, true].run {
            assertExists()
            val parent = onParent()
            parent.assertHasClickAction(); parent.performClick() // show dialog
        }
        rule[AlertDialogTestTag].assertExists() // dialog should exist
        rule[DeviceScreen_SupportStatusTestTag].assertHasTextExactly(supportStatusResId)
    }
}
