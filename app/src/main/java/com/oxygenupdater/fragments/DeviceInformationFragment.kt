package com.oxygenupdater.fragments

import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import com.oxygenupdater.OxygenUpdater.Companion.NO_OXYGEN_OS
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.internal.DeviceInformationData
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_device_information.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DeviceInformationFragment : AbstractFragment(R.layout.fragment_device_information) {

    private val systemVersionProperties by inject<SystemVersionProperties>()
    private val mainViewModel by sharedViewModel<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        displayDeviceInformation()

        mainViewModel.allDevices.observe(viewLifecycleOwner) { devices ->
            val deviceOsSpec = Utils.checkDeviceOsSpec(devices)

            displayFormattedDeviceName(devices)

            if (!deviceOsSpec.isDeviceOsSpecSupported) {
                updateBannerText(deviceOsSpec)
            } else {
                bannerLayout.isVisible = false
            }
        }
    }

    private fun updateBannerText(deviceOsSpec: DeviceOsSpec) {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not update banner text!")
            return
        }

        bannerTextView.text = getString(
            when (deviceOsSpec) {
                DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS -> R.string.device_information_carrier_exclusive_oxygen_os
                DeviceOsSpec.UNSUPPORTED_OXYGEN_OS -> R.string.device_information_unsupported_oxygen_os
                DeviceOsSpec.UNSUPPORTED_OS -> R.string.device_information_unsupported_os
                else -> R.string.device_information_unsupported_os
            }
        )

        bannerLayout.isVisible = true

        bannerLayout.setOnClickListener { (activity as MainActivity?)?.displayUnsupportedDeviceOsSpecMessage(deviceOsSpec) }
    }

    private fun displayFormattedDeviceName(devices: List<Device>?) {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not display formatted device name!")
            return
        }

        devices?.find { device -> device.productNames.contains(systemVersionProperties.oxygenDeviceName) }
            ?.let { device -> device_information_header.text = device.name }
    }

    private fun displayDeviceInformation() {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not display device information!")
            return
        }

        // Device name (in top)
        device_information_header.text =
            getString(R.string.device_information_device_name, DeviceInformationData.deviceManufacturer, DeviceInformationData.deviceName)

        // SoC
        device_information_soc_field.text = DeviceInformationData.soc

        // CPU Frequency (if available)
        device_information_cpu_freq_field.apply {
            val cpuFreqString = DeviceInformationData.cpuFrequency

            text = if (cpuFreqString != DeviceInformationData.UNKNOWN) {
                getString(R.string.device_information_gigahertz, cpuFreqString)
            } else {
                getString(R.string.device_information_unknown)
            }
        }

        val totalMemoryBytes = try {
            val mi = ActivityManager.MemoryInfo()
            requireContext().getSystemService<ActivityManager>()?.getMemoryInfo(mi)

            // `mi.totalMem` is in bytes, but since we're using it in android.text.format.Formatter.formatFileSize,
            // we need to make sure to use SI units across all Android versions
            // The version check is required because `formatFileSize` uses SI units only in Oreo and above.
            // It uses IEC units pre-Oreo.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mi.totalMem
            } else {
                (mi.totalMem * 1.048576).toLong()
            }
        } catch (e: Exception) {
            logWarning(TAG, "Memory information is unavailable due to error", e)
            0L
        }

        // Total amount of RAM (if available)
        device_information_memory_field.apply {
            if (totalMemoryBytes != 0L) {
                text = Formatter.formatFileSize(context, totalMemoryBytes)
            } else {
                device_information_memory_label.isVisible = false
                isVisible = false
            }
        }

        // OxygenOS version (if available)
        device_information_oxygen_os_ver_field.apply {
            val oxygenOSVersion = systemVersionProperties.oxygenOSVersion

            if (oxygenOSVersion != NO_OXYGEN_OS) {
                text = oxygenOSVersion
            } else {
                device_information_oxygen_os_ver_label.isVisible = false
                isVisible = false
            }
        }

        // OxygenOS OTA version (if available)
        device_information_oxygen_os_ota_ver_field.apply {
            val oxygenOSOTAVersion = systemVersionProperties.oxygenOSOTAVersion

            if (oxygenOSOTAVersion != NO_OXYGEN_OS) {
                text = oxygenOSOTAVersion
            } else {
                isVisible = false
            }
        }

        // Android version
        device_information_os_ver_field.text = DeviceInformationData.osVersion

        // Incremental OS version
        device_information_incremental_os_ver_field.text = DeviceInformationData.incrementalOsVersion

        // Security Patch Date (if available)
        device_information_os_patch_level_field.apply {
            val securityPatchDate = systemVersionProperties.securityPatchDate

            if (securityPatchDate != NO_OXYGEN_OS) {
                text = securityPatchDate
            } else {
                device_information_os_patch_level_label.isVisible = false
                isVisible = false
            }
        }

        // Serial number (Android 7.1.2 and lower only)
        device_information_serial_number_field.apply {
            val serialNumber = DeviceInformationData.serialNumber

            if (serialNumber != DeviceInformationData.UNKNOWN) {
                text = serialNumber
            } else {
                device_information_serial_number_label.isVisible = false
                isVisible = false
            }
        }
    }

    companion object {
        private const val TAG = "DeviceInformationFragment"
    }
}
