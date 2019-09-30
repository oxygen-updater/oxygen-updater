package com.arjanvlek.oxygenupdater.setupwizard

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.UpdateMethod
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD_ID
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.CustomDropdown
import com.crashlytics.android.Crashlytics
import java8.util.function.Consumer
import java8.util.stream.StreamSupport

class SetupStep4Fragment : AbstractFragment() {

    private var rootView: View? = null
    private var mSettingsManager: SettingsManager? = null
    private var progressBar: ProgressBar? = null
    private var rootMessageShown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_setup_4, container, false)

        if (activity == null) {
            throw RuntimeException("SetupStep4Fragment: Can not initialize: not called from Activity")
        }

        mSettingsManager = SettingsManager(activity!!.applicationContext)
        progressBar = rootView!!.findViewById(R.id.introduction_step_4_update_method_progress_bar)


        return rootView
    }


    fun fetchUpdateMethods() {
        if (!rootMessageShown) {
            try {
                val builder = AlertDialog.Builder(activity)
                builder.setTitle(getString(R.string.root_check_title))
                builder.setMessage(getString(R.string.root_check_message))
                builder.setOnDismissListener {
                    rootMessageShown = true
                    fetchUpdateMethods()
                }
                builder.setPositiveButton(getString(R.string.download_error_close)) { _, _ ->
                    rootMessageShown = true
                    fetchUpdateMethods()
                }
                builder.show()
            } catch (e: Throwable) {
                logError("SetupStep4", "Failed to display root check dialog", e)
                rootMessageShown = true
                fetchUpdateMethods()
            }

        } else {
            if (mSettingsManager!!.containsPreference(PROPERTY_DEVICE_ID)) {
                progressBar!!.visibility = View.VISIBLE

                getApplicationData().getServerConnector()
                        .getUpdateMethods(mSettingsManager!!.getPreference(PROPERTY_DEVICE_ID, 1L), Consumer { this.fillUpdateMethodSettings(it) })
            }
        }
    }

    private fun fillUpdateMethodSettings(updateMethods: List<UpdateMethod>) {
        val spinner = rootView!!.findViewById<Spinner>(R.id.introduction_step_4_update_method_dropdown)

        val recommendedPositions = StreamSupport
                .stream(updateMethods)
                .filter { it.isRecommended }
                .mapToInt { updateMethods.indexOf(it) }
                .toArray()

        var selectedPosition = -1
        val updateMethodId = mSettingsManager!!.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L)

        if (updateMethodId != -1L) {
            selectedPosition = StreamSupport
                    .stream(updateMethods)
                    .filter { updateMethod -> updateMethod.id == updateMethodId }
                    .mapToInt { updateMethods.indexOf(it) }
                    .findAny()
                    .orElse(-1)
        } else if (recommendedPositions.isNotEmpty()) {
            selectedPosition = recommendedPositions[recommendedPositions.size - 1]
        }

        if (activity != null) {
            val adapter = object : ArrayAdapter<UpdateMethod>(activity!!, android.R.layout.simple_spinner_item, updateMethods) {

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_item, updateMethods, recommendedPositions, context)
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return CustomDropdown.initCustomUpdateMethodDropdown(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item, updateMethods, recommendedPositions, context)
                }
            }
            spinner.adapter = adapter

            if (selectedPosition != -1) {
                spinner.setSelection(selectedPosition)
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                    val updateMethod = adapterView.getItemAtPosition(i) as UpdateMethod

                    //Set update method in preferences.
                    mSettingsManager!!.savePreference(PROPERTY_UPDATE_METHOD_ID, updateMethod.id)
                    mSettingsManager!!.savePreference(PROPERTY_UPDATE_METHOD, updateMethod.englishName)
                    Crashlytics.setUserIdentifier("Device: " + mSettingsManager!!.getPreference(PROPERTY_DEVICE, "<UNKNOWN>") + ", Update Method: " + mSettingsManager!!
                            .getPreference(PROPERTY_UPDATE_METHOD, "<UNKNOWN>"))

                    if (getApplicationData().checkPlayServices(activity!!, false)) {
                        // Subscribe to notifications
                        // Subscribe to notifications for the newly selected device and update method
                        NotificationTopicSubscriber.subscribe(getApplicationData())
                    } else {
                        Toast.makeText(getApplicationData(), getString(R.string.notification_no_notification_support), LENGTH_LONG)
                                .show()
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {

                }
            }

            progressBar!!.visibility = View.GONE
        }
    }
}
