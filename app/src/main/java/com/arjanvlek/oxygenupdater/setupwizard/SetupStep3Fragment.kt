package com.arjanvlek.oxygenupdater.setupwizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.Device
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.CustomDropdown
import java8.util.stream.StreamSupport

class SetupStep3Fragment : AbstractFragment() {

    private var rootView: View? = null
    private var settingsManager: SettingsManager? = null
    private var progressBar: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_setup_3, container, false)
        settingsManager = SettingsManager(getApplicationData())
        progressBar = rootView!!.findViewById(R.id.introduction_step_3_device_loading_bar)
        return rootView
    }

    fun fetchDevices() {
        getApplicationData().getServerConnector().getDevices(Consumer<List<Device>> { this.fillDeviceSettings(it) })
    }


    private fun fillDeviceSettings(devices: List<Device>) {
        if (activity == null || !isAdded) {
            return  // Do not load if app is in process of being exited when data arrives from server.
        }
        val spinner = rootView!!.findViewById<Spinner>(R.id.introduction_step_3_device_dropdown)

        val systemVersionProperties = (activity!!.application as ApplicationData).getSystemVersionProperties()

        val selectedIndex: Int
        val recommendedIndex: Int
        val deviceId = settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)

        recommendedIndex = StreamSupport.stream(devices)
                .filter { d -> d.productNames != null && d.productNames!!.contains(systemVersionProperties.oxygenDeviceName) }
                .mapToInt(ToIntFunction<Device> { devices.indexOf(it) })
                .findAny()
                .orElse(-1)

        selectedIndex = if (deviceId != -1L) {
            StreamSupport.stream(devices)
                    .filter { d -> d.id == deviceId }
                    .mapToInt(ToIntFunction<Device> { devices.indexOf(it) })
                    .findAny()
                    .orElse(-1)
        } else {
            recommendedIndex
        }

        val adapter = object : ArrayAdapter<Device>(activity!!, android.R.layout.simple_spinner_item, devices) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, devices, recommendedIndex, context)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return CustomDropdown.initCustomDeviceDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, devices, recommendedIndex, context)
            }
        }
        spinner.adapter = adapter

        if (selectedIndex != -1) {
            spinner.setSelection(selectedIndex)
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                val device = adapterView.getItemAtPosition(i) as Device
                settingsManager!!.savePreference(SettingsManager.PROPERTY_DEVICE, device.name)
                settingsManager!!.savePreference(SettingsManager.PROPERTY_DEVICE_ID, device.id)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {

            }

        }

        progressBar!!.visibility = View.GONE
    }
}
