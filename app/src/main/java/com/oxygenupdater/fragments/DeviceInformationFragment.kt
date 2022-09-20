package com.oxygenupdater.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import coil.load
import com.oxygenupdater.OxygenUpdater.Companion.NO_OXYGEN_OS
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.databinding.FragmentDeviceInformationBinding
import com.oxygenupdater.databinding.LayoutDeviceInformationHardwareBinding
import com.oxygenupdater.databinding.LayoutDeviceInformationSoftwareBinding
import com.oxygenupdater.internal.DeviceInformationData
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.Logger
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.UpdateDataVersionFormatter.getFormattedOxygenOsVersion
import com.oxygenupdater.viewmodels.MainViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import kotlin.math.roundToLong

class DeviceInformationFragment : Fragment() {

    private val systemVersionProperties by inject<SystemVersionProperties>()
    private val mainViewModel by sharedViewModel<MainViewModel>()

    /** Only valid between `onCreateView` and `onDestroyView` */
    private var binding: FragmentDeviceInformationBinding? = null
    private var bindingSoftware: LayoutDeviceInformationSoftwareBinding? = null
    private var bindingHardware: LayoutDeviceInformationHardwareBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = FragmentDeviceInformationBinding.inflate(inflater, container, false).run {
        binding = this
        root
    }.also {
        bindingSoftware = LayoutDeviceInformationSoftwareBinding.bind(it)
        bindingHardware = LayoutDeviceInformationHardwareBinding.bind(it)
    }

    override fun onDestroyView() = super.onDestroyView().also {
        binding = null
        bindingSoftware = null
        bindingHardware = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        displaySoftwareInfo()
        displayHardwareInfo()

        mainViewModel.settingsChanged.observe(viewLifecycleOwner) {
            if (it == PrefManager.PROPERTY_DEVICE_ID) {
                updateDeviceMismatchStatusBanner()
            }
        }

        mainViewModel.allDevices.observe(viewLifecycleOwner) { devices ->
            val deviceName = devices?.find { device ->
                device.productNames.contains(systemVersionProperties.oxygenDeviceName)
            }?.name ?: getString(
                R.string.device_information_device_name,
                DeviceInformationData.deviceManufacturer,
                DeviceInformationData.deviceName
            )

            updateBanner(deviceName)
        }
    }

    private fun updateBanner(deviceName: String) {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not update banner text!")
            return
        }

        binding?.deviceNameTextView?.apply {
            isVisible = true
            text = deviceName
        }
        binding?.modelTextView?.apply {
            isVisible = true
            text = DeviceInformationData.model
        }
        binding?.deviceSupportStatus?.apply {
            isVisible = true
            text = getString(
                when (mainViewModel.deviceOsSpec) {
                    DeviceOsSpec.SUPPORTED_OXYGEN_OS -> R.string.device_information_supported_oxygen_os
                    DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS -> R.string.device_information_carrier_exclusive_oxygen_os
                    DeviceOsSpec.UNSUPPORTED_OXYGEN_OS -> R.string.device_information_unsupported_oxygen_os
                    DeviceOsSpec.UNSUPPORTED_OS -> R.string.device_information_unsupported_os
                    else -> R.string.device_information_unsupported_os
                }
            )
        }

        binding?.deviceImage?.load(Device.constructImageUrl(deviceName)) {
            placeholder(R.drawable.oneplus7pro)
            error(R.drawable.oneplus7pro)
        }

        val notSupported = mainViewModel.deviceOsSpec?.isDeviceOsSpecSupported == false
        binding?.deviceImageOverlay?.isVisible = notSupported
        binding?.deviceImageOverlayIcon?.isVisible = notSupported
        binding?.deviceImageLayout?.setOnClickListener(
            if (notSupported) View.OnClickListener {
                (activity as MainActivity?)?.displayUnsupportedDeviceOsSpecMessage()
            } else null
        )

        updateDeviceMismatchStatusBanner()
    }

    private fun updateDeviceMismatchStatusBanner() = binding?.deviceMismatchStatus?.run {
        if (mainViewModel.deviceMismatchStatus?.first == true) {
            isVisible = true
            binding?.contentDivider?.isVisible = true
            text = getString(
                R.string.incorrect_device_warning_message,
                mainViewModel.deviceMismatchStatus!!.second,
                mainViewModel.deviceMismatchStatus!!.third
            )
        } else {
            isVisible = false
            binding?.contentDivider?.isVisible = false
        }
    }

    private fun displaySoftwareInfo() {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not display software information!")
            return
        }

        // Android version
        bindingSoftware?.osVersionField?.text = DeviceInformationData.osVersion

        // OxygenOS version (if available)
        bindingSoftware?.oxygenOsVersionField?.run {
            val oxygenOSVersion = getFormattedOxygenOsVersion(systemVersionProperties.oxygenOSVersion)

            if (oxygenOSVersion != NO_OXYGEN_OS) {
                text = oxygenOSVersion
            } else {
                bindingSoftware?.oxygenOsVersionLabel?.isVisible = false
                isVisible = false
            }
        }

        // OxygenOS OTA version (if available)
        bindingSoftware?.otaVersionField?.run {
            val oxygenOSOTAVersion = systemVersionProperties.oxygenOSOTAVersion

            if (oxygenOSOTAVersion != NO_OXYGEN_OS) {
                text = oxygenOSOTAVersion
            } else {
                bindingSoftware?.otaVersionLabel?.isVisible = false
                isVisible = false
            }
        }

        // Incremental OS version
        bindingSoftware?.incrementalOsVersionField?.text = DeviceInformationData.incrementalOsVersion

        // Security Patch Date (if available)
        bindingSoftware?.securityPatchField?.run {
            val securityPatchDate = systemVersionProperties.securityPatchDate

            if (securityPatchDate != NO_OXYGEN_OS) {
                text = securityPatchDate
            } else {
                bindingSoftware?.securityPatchLabel?.isVisible = false
                isVisible = false
            }
        }
    }

    private fun displayHardwareInfo() {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not display hardware information!")
            return
        }

        // RAM (if available)
        bindingHardware?.ramField?.run {
            val ramBytes = try {
                val mi = ActivityManager.MemoryInfo()
                requireContext().getSystemService<ActivityManager>()?.getMemoryInfo(mi)

                // Round up
                var approxRam = (mi.totalMem / 1000000000.toDouble()).roundToLong()
                // RAM can never be an odd number in OP devices
                if (approxRam % 2 == 1L) {
                    approxRam++
                }

                // `mi.totalMem` is in bytes, but since we're using it in android.text.format.Formatter.formatShortFileSize,
                // we need to make sure to use SI units across all Android versions
                // The version check is required because `formatShortFileSize` uses SI units only in Oreo and above.
                // It uses IEC units pre-Oreo.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    approxRam * 1000000000L
                } else {
                    approxRam * 1073741824
                }
            } catch (e: Exception) {
                Logger.logWarning(TAG, "Memory information is unavailable due to error", e)
                0L
            }

            @SuppressLint("SetTextI18n")
            if (ramBytes != 0L) {
                text = Formatter.formatShortFileSize(context, ramBytes)
            } else {
                bindingHardware?.ramLabel?.isVisible = false
                isVisible = false
            }
        }

        // SoC
        bindingHardware?.socField?.text = DeviceInformationData.soc

        // CPU Frequency (if available)
        bindingHardware?.freqField?.run {
            val cpuFreqString = DeviceInformationData.cpuFrequency

            text = if (cpuFreqString != DeviceInformationData.UNKNOWN) {
                getString(R.string.device_information_gigahertz, cpuFreqString)
            } else {
                getString(R.string.device_information_unknown)
            }
        }

        // Serial number (Android 7.1.2 and lower only)
        bindingHardware?.serialField?.run {
            val serialNumber = DeviceInformationData.serialNumber

            if (serialNumber != DeviceInformationData.UNKNOWN) {
                text = serialNumber
            } else {
                bindingHardware?.serialLabel?.isVisible = false
                isVisible = false
            }
        }
    }

    companion object {
        private const val TAG = "DeviceInformationFragment"
    }
}
