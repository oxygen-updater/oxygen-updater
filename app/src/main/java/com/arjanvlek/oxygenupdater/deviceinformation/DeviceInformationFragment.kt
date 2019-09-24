package com.arjanvlek.oxygenupdater.deviceinformation

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NO_OXYGEN_OS
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.Device
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import java8.util.stream.StreamSupport

class DeviceInformationFragment : AbstractFragment() {
    private var rootView: NestedScrollView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        //Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_device_information, container, false) as NestedScrollView
        return rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) {
            logError(TAG, OxygenUpdaterException("Fragment not added. Can not create the view!"))
            return
        }

        displayDeviceInformation()
        getApplicationData().getServerConnector().getDevices(java8.util.function.Consumer { devices -> this@DeviceInformationFragment.displayFormattedDeviceName(devices) }
        )
    }

    private fun displayFormattedDeviceName(devices: List<Device>?) {
        if (!isAdded) {
            logError(TAG, OxygenUpdaterException("Fragment not added. Can not display formatted device name!"))
            return
        }

        val deviceNameView = rootView!!.findViewById<TextView>(R.id.device_information_header)
        val systemVersionProperties = getApplicationData().getSystemVersionProperties()

        if (devices != null) {
            StreamSupport.stream(devices)
                    .filter { device -> device.productNames != null && device.productNames!!.contains(systemVersionProperties.oxygenDeviceName) }
                    .findAny()
                    .ifPresent { device -> deviceNameView.text = device.name }
        }
    }

    private fun displayDeviceInformation() {
        if (!isAdded) {
            logError(TAG, OxygenUpdaterException("Fragment not added. Can not display device information!"))
            return
        }

        val deviceInformationData = DeviceInformationData()
        val systemVersionProperties = getApplicationData().getSystemVersionProperties()

        // Device name (in top)
        val deviceNameView = rootView!!.findViewById<TextView>(R.id.device_information_header)
        deviceNameView.text = String.format(getString(R.string.device_information_device_name), deviceInformationData.deviceManufacturer, deviceInformationData.deviceName)

        // SoC
        val socView = rootView!!.findViewById<TextView>(R.id.device_information_soc_field)
        socView.text = deviceInformationData.soc

        // CPU Frequency (if available)
        val cpuFreqString = deviceInformationData.cpuFrequency
        val cpuFreqView = rootView!!.findViewById<TextView>(R.id.device_information_cpu_freq_field)

        if (cpuFreqString != DeviceInformationData.UNKNOWN) {
            cpuFreqView.text = String.format(getString(R.string.device_information_gigahertz), deviceInformationData.cpuFrequency)
        } else {
            cpuFreqView.text = getString(R.string.device_information_unknown)
        }

        val totalMemory: Long = try {
            val mi = ActivityManager.MemoryInfo()
            val activityManager = Utils.getSystemService(getApplicationData(), Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            mi.totalMem / 1048576L
        } catch (e: Exception) {
            logWarning("DeviceInformationFragment", "Memory information is unavailable due to error", e)
            0
        }

        // Total amount of RAM (if available)
        val memoryView = rootView!!.findViewById<TextView>(R.id.device_information_memory_field)

        if (totalMemory != 0L) {
            memoryView.text = String.format(getString(R.string.download_size_megabyte), totalMemory)
        } else {
            val memoryLabel = rootView!!.findViewById<View>(R.id.device_information_memory_label)
            memoryLabel.visibility = View.GONE
            memoryView.visibility = View.GONE
        }

        // Oxygen OS version (if available)
        val oxygenOsVersionView = rootView!!.findViewById<TextView>(R.id.device_information_oxygen_os_ver_field)

        if (systemVersionProperties?.oxygenOSVersion != NO_OXYGEN_OS) {
            oxygenOsVersionView.text = systemVersionProperties.oxygenOSVersion

        } else {
            val oxygenOsVersionLabel = rootView!!.findViewById<TextView>(R.id.device_information_oxygen_os_ver_label)
            oxygenOsVersionLabel.visibility = View.GONE
            oxygenOsVersionView.visibility = View.GONE
        }

        // Oxygen OS OTA version (if available)
        val oxygenOsOtaVersionView = rootView!!.findViewById<TextView>(R.id.device_information_oxygen_os_ota_ver_field)

        if (systemVersionProperties?.oxygenOSOTAVersion != NO_OXYGEN_OS) {
            oxygenOsOtaVersionView.text = systemVersionProperties?.oxygenOSOTAVersion
        } else {
            oxygenOsOtaVersionView.visibility = View.GONE
        }

        // Android version
        val osVerView = rootView!!.findViewById<TextView>(R.id.device_information_os_ver_field)
        osVerView.text = deviceInformationData.osVersion

        // Incremental OS version
        val osIncrementalView = rootView!!.findViewById<TextView>(R.id.device_information_incremental_os_ver_field)
        osIncrementalView.text = deviceInformationData.incrementalOsVersion

        // Security Patch Date (if available)
        val osPatchDateView = rootView!!.findViewById<TextView>(R.id.device_information_os_patch_level_field)
        val securityPatchDate = systemVersionProperties.securityPatchDate

        if (securityPatchDate != NO_OXYGEN_OS) {
            osPatchDateView.text = securityPatchDate
        } else {
            val osPatchDateLabel = rootView!!.findViewById<TextView>(R.id.device_information_os_patch_level_label)
            osPatchDateLabel.visibility = View.GONE
            osPatchDateView.visibility = View.GONE
        }

        // Serial number (Android 7.1.2 and lower only)
        val serialNumberView = rootView!!.findViewById<TextView>(R.id.device_information_serial_number_field)
        val serialNumber = deviceInformationData.serialNumber

        if (serialNumber != null && serialNumber != DeviceInformationData.UNKNOWN) {
            serialNumberView.text = deviceInformationData.serialNumber
        } else {
            val serialNumberLabel = rootView!!.findViewById<View>(R.id.device_information_serial_number_label)
            serialNumberLabel.visibility = View.GONE
            serialNumberView.visibility = View.GONE
        }
    }

    companion object {
        private val TAG = "DeviceInformationFragment"
    }

}
