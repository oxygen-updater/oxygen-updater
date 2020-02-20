package com.arjanvlek.oxygenupdater.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.SelectableModel
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.NotificationTopicSubscriber
import com.crashlytics.android.Crashlytics
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_onboarding_chooser.*

class UpdateMethodChooserOnboardingFragment : ChooserOnboardingFragment() {

    private var rootMessageShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboardingChooserCaption.setText(R.string.onboarding_page_3_caption)
    }

    override fun fetchData() {
        if (!rootMessageShown) {
            try {
                MaterialAlertDialogBuilder(activity)
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
        } else {
            if (settingsManager!!.containsPreference(SettingsManager.PROPERTY_DEVICE_ID)) {
                applicationData?.serverConnector!!.getUpdateMethods(settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, 1L)) {
                    setupRecyclerView(it)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    override fun setupRecyclerView(data: List<SelectableModel>, initialSelectedIndex: Int, onItemSelectedListener: KotlinCallback<SelectableModel>) {
        val data = data as List<UpdateMethod>

        val updateMethodId = settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        val recommendedPositions = ArrayList<Int>()
        data.mapIndexedTo(recommendedPositions) { index, updateMethod -> if (updateMethod.recommended) index else -1 }
        recommendedPositions.retainAll { it != -1 }

        val initialSelectedIndex = when {
            updateMethodId != -1L -> data.indexOfFirst { it.id == updateMethodId }
            recommendedPositions.size > 0 -> recommendedPositions.last()
            else -> -1
        }

        super.setupRecyclerView(data, initialSelectedIndex) {
            settingsManager?.apply {
                savePreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, it.id)
                savePreference(SettingsManager.PROPERTY_UPDATE_METHOD, it.name)

                Crashlytics.setUserIdentifier(
                    "Device: " + getPreference(SettingsManager.PROPERTY_DEVICE, "<UNKNOWN>")
                            + ", Update Method: " + getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
                )
            }

            if (applicationData?.checkPlayServices(activity, false) == true) {
                // Subscribe to notifications for the newly selected device and update method
                NotificationTopicSubscriber.subscribe(applicationData!!)
            } else {
                Toast.makeText(applicationData, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show()
            }
        }
    }
}
