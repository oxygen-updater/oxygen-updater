package com.arjanvlek.oxygenupdater.setupwizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.CustomDropdown
import kotlinx.android.synthetic.main.fragment_setup_3.*

class SetupStep3Fragment : AbstractFragment() {
    private lateinit var rootView: View
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_setup_3, container, false)
        progressBar = rootView.findViewById(R.id.introduction_step_3_device_loading_bar)
        return rootView
    }

    fun fetchDevices() {
        applicationData?.serverConnector!!.getDevices(DeviceRequestFilter.ENABLED) { fillDeviceSettings(it) }
    }

    private fun fillDeviceSettings(devices: List<Device>) {
        if (activity == null || !isAdded) {
            return  // Do not load if app is in process of being exited when data arrives from server.
        }

        val systemVersionProperties = applicationData?.systemVersionProperties!!
        val deviceId = settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)

        val recommendedIndex = devices.indexOfFirst { it.productNames.contains(systemVersionProperties.oxygenDeviceName) }
        val selectedIndex = if (deviceId != -1L) devices.indexOfFirst { it.id == deviceId } else recommendedIndex

        introduction_step_3_device_dropdown.apply {
            adapter = object : ArrayAdapter<Device?>(context, android.R.layout.simple_spinner_item, devices) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, devices, recommendedIndex, context)!!
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return CustomDropdown.initCustomDeviceDropdown(
                        position,
                        convertView,
                        parent,
                        android.R.layout.simple_spinner_dropdown_item,
                        devices,
                        recommendedIndex,
                        context
                    )!!
                }
            }

            if (selectedIndex != -1) {
                setSelection(selectedIndex)
            }

            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                    val device = adapterView.getItemAtPosition(i) as Device
                    settingsManager!!.savePreference(SettingsManager.PROPERTY_DEVICE, device.name)
                    settingsManager!!.savePreference(SettingsManager.PROPERTY_DEVICE_ID, device.id)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        }

        progressBar.visibility = View.GONE
    }
}
