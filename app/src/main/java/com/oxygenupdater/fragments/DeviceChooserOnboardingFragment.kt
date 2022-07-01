package com.oxygenupdater.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.view.isVisible
import com.oxygenupdater.R
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.viewmodels.OnboardingViewModel
import kotlinx.android.synthetic.main.fragment_onboarding_chooser.*
import kotlinx.android.synthetic.main.layout_error.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DeviceChooserOnboardingFragment : ChooserOnboardingFragment() {

    private val systemVersionProperties by inject<SystemVersionProperties>()
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
        onboardingViewModel.enabledDevices.observe(viewLifecycleOwner) {
            if (it == null) {
                inflateAndShowErrorState()
            } else {
                setupRecyclerView(it)
            }
        }
    }

    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    override fun setupRecyclerView(
        data: List<SelectableModel>,
        initialSelectedIndex: Int,
        onItemSelectedListener: KotlinCallback<SelectableModel>
    ) {
        hideErrorStateIfInflated()

        val data = data as List<Device>

        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L)

        val recommendedIndex = data.indexOfFirst { it.productNames.contains(systemVersionProperties.oxygenDeviceName) }
        val initialSelectedIndex = if (deviceId != -1L) data.indexOfFirst { it.id == deviceId } else recommendedIndex

        super.setupRecyclerView(data, initialSelectedIndex) {
            onboardingViewModel.updateSelectedDevice(it as Device)
        }
    }

    private fun inflateAndShowErrorState() {
        // Hide the loading shimmer since an error state can only be enabled after a load completes
        shimmerFrameLayout.isVisible = false

        // Show error layout
        if (errorLayoutStub?.parent != null) {
            errorLayoutStub.inflate()
        }
        errorLayout.isVisible = true
        errorActionButton.isVisible = false

        errorTitle.text = getString(R.string.device_chooser_error_title)
        // Make the links clickable
        errorText.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun hideErrorStateIfInflated() {
        // Stub is null only after it has been inflated, and
        // we need to hide the error state only if it has been inflated
        if (errorLayoutStub == null || errorLayoutStub.parent == null) {
            errorLayout.isVisible = false
        }
    }
}
