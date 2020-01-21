package com.arjanvlek.oxygenupdater.setupwizard

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Toast
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.CustomDropdown
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.fragment_setup_4.*

class SetupStep4Fragment : AbstractFragment() {
    private lateinit var rootView: View
    private lateinit var progressBar: ProgressBar

    private var rootMessageShown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_setup_4, container, false)

        if (activity == null) {
            throw RuntimeException("SetupStep4Fragment: Can not initialize: not called from Activity")
        }

        progressBar = rootView.findViewById(R.id.introduction_step_4_update_method_progress_bar)
        return rootView
    }

    fun fetchUpdateMethods() {
        if (!rootMessageShown) {
            try {
                AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.root_check_title))
                    .setMessage(getString(R.string.root_check_message))
                    .setOnDismissListener {
                        rootMessageShown = true
                        fetchUpdateMethods()
                    }
                    .setPositiveButton(getString(R.string.download_error_close)) { _: DialogInterface?, _: Int ->
                        rootMessageShown = true
                        fetchUpdateMethods()
                    }
                    .show()
            } catch (e: Throwable) {
                logError("SetupStep4", "Failed to display root check dialog", e)
                rootMessageShown = true
                fetchUpdateMethods()
            }
        } else {
            if (settingsManager!!.containsPreference(SettingsManager.PROPERTY_DEVICE_ID)) {
                progressBar.visibility = View.VISIBLE

                applicationData?.serverConnector!!.getUpdateMethods(settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, 1L)) {
                    fillUpdateMethodSettings(it)
                }
            }
        }
    }

    private fun fillUpdateMethodSettings(updateMethods: List<UpdateMethod>) {
        val updateMethodId = settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        val recommendedPositions = ArrayList<Int>()
        updateMethods.mapIndexedTo(recommendedPositions) { index, updateMethod -> if (updateMethod.recommended) index else -1 }
        recommendedPositions.retainAll { it != -1 }

        val selectedPosition = if (updateMethodId != -1L) updateMethods.indexOfFirst { it.id == updateMethodId } else recommendedPositions[recommendedPositions.size - 1]

        if (activity != null) {
            introduction_step_4_update_method_dropdown.apply {
                adapter = object : ArrayAdapter<UpdateMethod?>(activity!!, android.R.layout.simple_spinner_item, updateMethods) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        return CustomDropdown.initCustomUpdateMethodDropdown(
                            position,
                            convertView,
                            parent,
                            android.R.layout.simple_spinner_item,
                            updateMethods,
                            recommendedPositions,
                            context
                        )!!
                    }

                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        return CustomDropdown.initCustomUpdateMethodDropdown(
                            position,
                            convertView,
                            parent,
                            android.R.layout.simple_spinner_dropdown_item,
                            updateMethods,
                            recommendedPositions,
                            context
                        )!!
                    }
                }

                if (selectedPosition != -1) {
                    setSelection(selectedPosition)
                }

                onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                        val (id, englishName) = adapterView.getItemAtPosition(i) as UpdateMethod

                        //Set update method in preferences.
                        settingsManager?.apply {
                            savePreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, id)
                            savePreference(SettingsManager.PROPERTY_UPDATE_METHOD, englishName)
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

                    override fun onNothingSelected(adapterView: AdapterView<*>?) {}
                }
            }

            progressBar.visibility = View.GONE
        }
    }
}
