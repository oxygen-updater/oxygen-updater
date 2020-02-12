package com.arjanvlek.oxygenupdater.deviceinformation

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NO_OXYGEN_OS
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.deviceinformation.DeviceInformationData.Companion.UNKNOWN
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.MainActivity
import kotlinx.android.synthetic.main.fragment_device_information.*

class DeviceInformationFragment : AbstractFragment(), MainActivity.DeviceOsSpecCheckedListener {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_device_information, container, false) as NestedScrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not create the view!")
            return
        }

        displayDeviceInformation()
        applicationData!!.serverConnector!!.getDevices(DeviceRequestFilter.ALL) { devices ->
            updateBannerText(Utils.checkDeviceOsSpec(applicationData!!.systemVersionProperties!!, devices))

            displayFormattedDeviceName(devices)
        }
    }

    override fun onDeviceOsSpecChecked(deviceOsSpec: DeviceOsSpec) {
        if (isAdded) {
            updateBannerText(deviceOsSpec)
        }
    }

    private fun updateBannerText(deviceOsSpec: DeviceOsSpec) {
        bannerTextView.text = getString(
            when (deviceOsSpec) {
                DeviceOsSpec.SUPPORTED_OXYGEN_OS -> R.string.device_information_supported_oxygen_os
                DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS -> R.string.device_information_carrier_exclusive_oxygen_os
                DeviceOsSpec.UNSUPPORTED_OXYGEN_OS -> R.string.device_information_unsupported_oxygen_os
                DeviceOsSpec.UNSUPPORTED_OS -> R.string.device_information_unsupported_os
            }
        )

        bannerLayout.isVisible = true

        if (!deviceOsSpec.isDeviceOsSpecSupported) {
            bannerLayout.setOnClickListener { (activity as MainActivity?)?.displayUnsupportedDeviceOsSpecMessage(deviceOsSpec) }
        }
    }

    private fun displayFormattedDeviceName(devices: List<Device>?) {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not display formatted device name!")
            return
        }

        devices?.find { device -> device.productNames.contains(applicationData?.systemVersionProperties?.oxygenDeviceName) }
            ?.let { device -> device_information_header.text = device.name }
    }

    private fun displayDeviceInformation() {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not display device information!")
            return
        }

        val deviceInformationData = DeviceInformationData()
        val systemVersionProperties = applicationData?.systemVersionProperties

        // Device name (in top)
        device_information_header.text =
            getString(R.string.device_information_device_name, deviceInformationData.deviceManufacturer, deviceInformationData.deviceName)

        // SoC
        device_information_soc_field.text = deviceInformationData.soc

        // CPU Frequency (if available)
        device_information_cpu_freq_field.apply {
            val cpuFreqString = deviceInformationData.cpuFrequency

            text = if (cpuFreqString != UNKNOWN) {
                getString(R.string.device_information_gigahertz, cpuFreqString)
            } else {
                getString(R.string.device_information_unknown)
            }
        }

        val totalMemory: Long = try {
            val mi = ActivityManager.MemoryInfo()
            val activityManager = Utils.getSystemService(applicationData, Context.ACTIVITY_SERVICE) as ActivityManager

            activityManager.getMemoryInfo(mi)
            mi.totalMem / 1048576L
        } catch (e: Exception) {
            Logger.logWarning("DeviceInformationFragment", "Memory information is unavailable due to error", e)
            0
        }

        // Total amount of RAM (if available)
        device_information_memory_field.apply {
            if (totalMemory != 0L) {
                text = getString(R.string.download_size_megabyte, totalMemory)
            } else {
                device_information_memory_label.isVisible = false
                isVisible = false
            }
        }

        // OxygenOS version (if available)
        device_information_oxygen_os_ver_field.apply {
            val oxygenOSVersion = systemVersionProperties?.oxygenOSVersion

            if (oxygenOSVersion != NO_OXYGEN_OS) {
                text = oxygenOSVersion
            } else {
                device_information_oxygen_os_ver_label.isVisible = false
                isVisible = false
            }
        }

        // OxygenOS OTA version (if available)
        device_information_oxygen_os_ota_ver_field.apply {
            val oxygenOSOTAVersion = systemVersionProperties?.oxygenOSOTAVersion

            if (oxygenOSOTAVersion != NO_OXYGEN_OS) {
                text = oxygenOSOTAVersion
            } else {
                isVisible = false
            }
        }

        // Android version
        device_information_os_ver_field.text = deviceInformationData.osVersion

        // Incremental OS version
        device_information_incremental_os_ver_field.text = deviceInformationData.incrementalOsVersion

        // Security Patch Date (if available)
        device_information_os_patch_level_field.apply {
            val securityPatchDate = systemVersionProperties?.securityPatchDate

            if (securityPatchDate != NO_OXYGEN_OS) {
                text = securityPatchDate
            } else {
                device_information_os_patch_level_label.isVisible = false
                isVisible = false
            }
        }

        // Serial number (Android 7.1.2 and lower only)
        device_information_serial_number_field.apply {
            val serialNumber = deviceInformationData.serialNumber

            if (serialNumber != UNKNOWN) {
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
