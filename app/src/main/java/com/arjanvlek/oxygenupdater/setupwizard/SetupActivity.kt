package com.arjanvlek.oxygenupdater.setupwizard

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.contribution.ContributorUtils
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.SetupUtils
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.MainActivity
import kotlinx.android.synthetic.main.activity_setup.*
import kotlinx.android.synthetic.main.fragment_setup_5.*

class SetupActivity : AppCompatActivity() {
    private lateinit var settingsManager: SettingsManager

    private var step3Fragment: Fragment? = null
    private var step4Fragment: Fragment? = null

    private var permissionCallback: KotlinCallback<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        settingsManager = SettingsManager(applicationContext)

        if (!settingsManager.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)) {
            val applicationData = application as ApplicationData
            applicationData.serverConnector!!.getDevices(DeviceRequestFilter.ALL) {
                val deviceOsSpec = Utils.checkDeviceOsSpec(applicationData.systemVersionProperties!!, it)

                if (!deviceOsSpec.isDeviceOsSpecSupported) {
                    displayUnsupportedDeviceOsSpecMessage(deviceOsSpec)
                }
            }
        }

        // Set up the ViewPager with the sections adapter.
        tutorialActivityPager.apply {
            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            adapter = SectionsPagerAdapter(supportFragmentManager)

            addOnPageChangeListener(object : OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    if (position == 2 && step3Fragment != null) {
                        (step3Fragment as SetupStep3Fragment).fetchDevices()
                    }

                    if (position == 3 && step4Fragment != null) {
                        (step4Fragment as SetupStep4Fragment).fetchUpdateMethods()
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })
        }
    }

    private fun displayUnsupportedDeviceOsSpecMessage(deviceOsSpec: DeviceOsSpec) {
        // Do not show dialog if app was already exited upon receiving of devices from the server.
        if (isFinishing) {
            return
        }

        val resourceId: Int = when (deviceOsSpec) {
            DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS -> R.string.carrier_exclusive_device_warning_message
            DeviceOsSpec.UNSUPPORTED_OXYGEN_OS -> R.string.unsupported_device_warning_message
            DeviceOsSpec.UNSUPPORTED_OS -> R.string.unsupported_os_warning_message
            else -> R.string.unsupported_os_warning_message
        }

        AlertDialog.Builder(this@SetupActivity)
            .setTitle(getString(R.string.unsupported_device_warning_title))
            .setPositiveButton(getString(R.string.download_error_close)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .setMessage(getString(resourceId))
            .show()
    }

    fun newInstance(sectionNumber: Int): Fragment {
        if (sectionNumber == 3) {
            step3Fragment = SetupStep3Fragment()
            return step3Fragment!!
        }

        if (sectionNumber == 4) {
            step4Fragment = SetupStep4Fragment()
            return step4Fragment!!
        }

        SimpleTutorialFragment().apply {
            arguments = bundleOf("section_number" to sectionNumber)
            return this
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun closeInitialTutorial(view: View?) {
        if (settingsManager.checkIfSetupScreenIsFilledIn()) {
            if (introduction_step_5_contribute_checkbox.isChecked) {
                requestContributorStoragePermissions { granted: Boolean ->
                    if (granted) {
                        ContributorUtils(applicationContext).flushSettings(true) // 1st time, will save setting to true.
                        settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
                        NavUtils.navigateUpFromSameTask(this@SetupActivity)
                    } else {
                        Toast.makeText(application, R.string.contribute_allow_storage, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
                settingsManager.savePreference(
                    SettingsManager.PROPERTY_CONTRIBUTE,
                    false
                ) // not signed up, saving this setting will prevent contribute popups which belong to app updates.
                NavUtils.navigateUpFromSameTask(this)
            }
        } else {
            val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
            val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
            logWarning(TAG, SetupUtils.getAsError("Setup wizard", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun onContributeMoreInfoClick(textView: View?) {
        ActivityLauncher(this).apply {
            ContributeNoEnroll()
            dispose()
        }
    }

    private fun requestContributorStoragePermissions(permissionCallback: KotlinCallback<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.permissionCallback = permissionCallback
            requestPermissions(arrayOf(MainActivity.VERIFY_FILE_PERMISSION), MainActivity.PERMISSION_REQUEST_CODE)
        } else {
            permissionCallback.invoke(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && requestCode == MainActivity.PERMISSION_REQUEST_CODE) {
            permissionCallback?.invoke(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    /**
     * Contains the basic / non interactive tutorial fragments.
     */
    class SimpleTutorialFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return when (arguments?.getInt(ARG_SECTION_NUMBER, 0)) {
                1 -> inflater.inflate(R.layout.fragment_setup_1, container, false)
                2 -> inflater.inflate(R.layout.fragment_setup_2, container, false)
                5 -> inflater.inflate(R.layout.fragment_setup_5, container, false)
                else -> null
            }
        }

        companion object {
            /**
             * The fragment argument representing the section number for this fragment.
             */
            private const val ARG_SECTION_NUMBER = "section_number"
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to one of the
     * sections/tabs/pages.
     */
    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager?) : FragmentPagerAdapter(fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a SimpleTutorialFragment (defined as a static inner class below).
            return newInstance(position + 1)
        }

        override fun getCount(): Int {
            // Show 5 total pages.
            return 5
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return null
        }
    }

    companion object {
        private const val TAG = "SetupActivity"
    }
}
