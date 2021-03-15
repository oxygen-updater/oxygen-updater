package com.oxygenupdater.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.oxygenupdater.R
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.SelectableModel
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.viewmodels.OnboardingViewModel
import kotlinx.android.synthetic.main.fragment_onboarding_chooser.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class UpdateMethodChooserOnboardingFragment : ChooserOnboardingFragment() {

    private var rootMessageShown = false

    private val onboardingViewModel by sharedViewModel<OnboardingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onboardingChooserCaption.setText(R.string.onboarding_page_3_caption)

        fetchData()
    }

    override fun fetchData() {
        if (!rootMessageShown) {
            try {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.root_check_title))
                    .setMessage(getString(R.string.root_check_message))
                    .setOnDismissListener {
                        rootMessageShown = true
                        fetchData()
                    }
                    .setPositiveButton(getString(R.string.download_error_close), null)
                    .show()
            } catch (e: Throwable) {
                logError("UpdateMethodChooserOnboardingFragment", "Failed to display root check dialog", e)
                rootMessageShown = true
                fetchData()
            }
        } else if (onboardingViewModel.selectedDevice.value != null) {
            fetchDataInternal(onboardingViewModel.selectedDevice.value!!.id)
        }

        // re-fetch update methods if selected device changes
        onboardingViewModel.selectedDevice.observe(viewLifecycleOwner) {
            if (rootMessageShown) {
                fetchDataInternal(it.id)
            }
        }

        onboardingViewModel.updateMethodsForDevice.observe(viewLifecycleOwner) {
            setupRecyclerView(it)
        }
    }

    private fun fetchDataInternal(deviceId: Long) {
        shimmerFrameLayout?.isVisible = true

        onboardingViewModel.fetchUpdateMethodsForDevice(deviceId)
    }

    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    override fun setupRecyclerView(
        data: List<SelectableModel>,
        initialSelectedIndex: Int,
        onItemSelectedListener: KotlinCallback<SelectableModel>
    ) {
        val data = data as List<UpdateMethod>

        val updateMethodId = SettingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        val recommendedPositions = ArrayList<Int>()
        data.mapIndexedTo(recommendedPositions) { index, updateMethod -> if (updateMethod.recommended) index else -1 }
        recommendedPositions.retainAll { it != -1 }

        val initialSelectedIndex = when {
            updateMethodId != -1L -> data.indexOfFirst { it.id == updateMethodId }
            recommendedPositions.size > 0 -> recommendedPositions.last()
            else -> -1
        }

        super.setupRecyclerView(data, initialSelectedIndex) {
            onboardingViewModel.updateSelectedUpdateMethod(it as UpdateMethod)

            if (checkPlayServices(requireActivity(), false)) {
                // Subscribe to notifications for the newly selected device and update method
                onboardingViewModel.subscribeToNotificationTopics()
            } else {
                Toast.makeText(context, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show()
            }
        }
    }
}
