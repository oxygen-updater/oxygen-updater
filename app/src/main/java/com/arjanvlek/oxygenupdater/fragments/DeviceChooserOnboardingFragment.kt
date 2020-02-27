package com.arjanvlek.oxygenupdater.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.SelectableModel
import com.arjanvlek.oxygenupdater.viewmodels.OnboardingViewModel
import kotlinx.android.synthetic.main.fragment_onboarding_chooser.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DeviceChooserOnboardingFragment : ChooserOnboardingFragment() {

    private val onboardingViewModel by sharedViewModel<OnboardingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onboardingChooserCaption.setText(R.string.onboarding_page_2_caption)

        fetchData()
    }

    /**
     * `enabledDevices` is being posted in the associated [androidx.lifecycle.ViewModel],
     * right after the API call for `fetchAllDevices()` completes
     */
    override fun fetchData() {
        onboardingViewModel.enabledDevices.observe(
            viewLifecycleOwner,
            Observer { setupRecyclerView(it) }
        )
    }

    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    override fun setupRecyclerView(
        data: List<SelectableModel>,
        initialSelectedIndex: Int,
        onItemSelectedListener: KotlinCallback<SelectableModel>
    ) {
        val data = data as List<Device>

        val systemVersionProperties = application?.systemVersionProperties!!
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)

        val recommendedIndex = data.indexOfFirst { it.productNames.contains(systemVersionProperties.oxygenDeviceName) }
        val initialSelectedIndex = if (deviceId != -1L) data.indexOfFirst { it.id == deviceId } else recommendedIndex

        super.setupRecyclerView(data, initialSelectedIndex) {
            onboardingViewModel.updateSelectedDevice(it as Device)
        }
    }
}
