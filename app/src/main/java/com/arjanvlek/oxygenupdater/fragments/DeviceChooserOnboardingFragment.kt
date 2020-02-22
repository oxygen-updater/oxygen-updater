package com.arjanvlek.oxygenupdater.fragments

import android.os.Bundle
import android.view.View
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.SelectableModel
import kotlinx.android.synthetic.main.fragment_onboarding_chooser.*

class DeviceChooserOnboardingFragment : ChooserOnboardingFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboardingChooserCaption.setText(R.string.onboarding_page_2_caption)
    }

    override fun fetchData() = application?.serverConnector!!.getDevices(DeviceRequestFilter.ENABLED) { setupRecyclerView(it) }

    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    override fun setupRecyclerView(data: List<SelectableModel>, initialSelectedIndex: Int, onItemSelectedListener: KotlinCallback<SelectableModel>) {
        val data = data as List<Device>

        val systemVersionProperties = application?.systemVersionProperties!!
        val deviceId = settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)

        val recommendedIndex = data.indexOfFirst { it.productNames.contains(systemVersionProperties.oxygenDeviceName) }
        val initialSelectedIndex = if (deviceId != -1L) data.indexOfFirst { it.id == deviceId } else recommendedIndex

        super.setupRecyclerView(data, initialSelectedIndex) label@{
            settingsManager?.apply {
                savePreference(SettingsManager.PROPERTY_DEVICE_ID, it.id)
                savePreference(SettingsManager.PROPERTY_DEVICE, it.name)
            }
        }
    }
}
