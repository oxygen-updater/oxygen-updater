package com.arjanvlek.oxygenupdater.setupwizard

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.contribution.ContributorUtils
import com.arjanvlek.oxygenupdater.internal.SetupUtils
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_CONTRIBUTE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD_ID
import com.arjanvlek.oxygenupdater.views.MainActivity.Companion.PERMISSION_REQUEST_CODE
import com.arjanvlek.oxygenupdater.views.MainActivity.Companion.VERIFY_FILE_PERMISSION


class SetupActivity : AppCompatActivity() {
    private var step3Fragment: Fragment? = null
    private var step4Fragment: Fragment? = null
    private var settingsManager: SettingsManager? = null
    private var permissionCallback: Consumer<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }

        settingsManager = SettingsManager(applicationContext)

        if (!settingsManager!!.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)) {
            val applicationData = application as ApplicationData
            applicationData.getServerConnector().getDevices(java8.util.function.Consumer { result ->
                if (!Utils.isSupportedDevice(applicationData.mSystemVersionProperties!!, result)) {
                    displayUnsupportedDeviceMessage()
                }
            })
        }

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        val mViewPager = findViewById<ViewPager>(R.id.tutorialActivityPager)
        mViewPager.adapter = mSectionsPagerAdapter
        mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                if (position == 2) {
                    if (step3Fragment != null) {
                        val setupStep3Fragment = step3Fragment as SetupStep3Fragment?
                        setupStep3Fragment!!.fetchDevices()
                    }
                }
                if (position == 3) {
                    if (step4Fragment != null) {
                        val setupStep4Fragment = step4Fragment as SetupStep4Fragment?
                        setupStep4Fragment!!.fetchUpdateMethods()
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

    }

    fun displayUnsupportedDeviceMessage() {
        // Do not show dialog if app was already exited upon receiving of devices from the server.
        if (isFinishing) {
            return
        }

        val builder = AlertDialog.Builder(this@SetupActivity)
        builder.setTitle(getString(R.string.unsupported_device_warning_title))
        builder.setMessage(getString(R.string.unsupported_device_warning_message))

        builder.setPositiveButton(getString(R.string.download_error_close)) { dialog, which ->
            dialog
                    .dismiss()
        }
        builder.show()
    }

    fun newInstance(sectionNumber: Int): Fragment {
        if (sectionNumber == 3) {
            return SetupStep3Fragment()
        }
        if (sectionNumber == 4) {
            return SetupStep4Fragment()
        }
        val args = Bundle()
        args.putInt("section_number", sectionNumber)
        val simpleTutorialFragment = SimpleTutorialFragment()
        simpleTutorialFragment.arguments = args
        return simpleTutorialFragment
    }

    @Suppress("UNUSED_PARAMETER")
    fun closeInitialTutorial(view: View) {
        if (settingsManager!!.checkIfSetupScreenIsFilledIn()) {
            val contributorCheckbox = findViewById<CheckBox>(R.id.introduction_step_5_contribute_checkbox)

            if (contributorCheckbox.isChecked) {
                requestContributorStoragePermissions(Consumer { granted ->
                    if (granted!!) {
                        val contributorUtils = ContributorUtils(applicationContext)
                        contributorUtils.flushSettings(true) // 1st time, will save setting to true.
                        settingsManager!!.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
                        NavUtils.navigateUpFromSameTask(this@SetupActivity)
                    } else {
                        Toast.makeText(application, R.string.contribute_allow_storage, LENGTH_LONG).show()
                    }
                })
            } else {
                settingsManager!!.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
                settingsManager!!.savePreference(PROPERTY_CONTRIBUTE, false) // not signed up, saving this setting will prevent contribute popups which belong to app updates.
                NavUtils.navigateUpFromSameTask(this)
            }
        } else {
            val deviceId = settingsManager!!.getPreference(PROPERTY_DEVICE_ID, -1L)
            val updateMethodId = settingsManager!!.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L)
            logWarning(TAG, SetupUtils.getAsError("Setup wizard", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), LENGTH_LONG).show()
        }
    }

    fun onContributeMoreInfoClick(textView: View) {
        val launcher = ActivityLauncher(this)
        launcher.Contribute_noenroll()
        launcher.dispose()
    }

    private fun requestContributorStoragePermissions(permissionCallback: Consumer<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.permissionCallback = permissionCallback
            requestPermissions(arrayOf(VERIFY_FILE_PERMISSION), PERMISSION_REQUEST_CODE)
        } else {
            permissionCallback.accept(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (permissionCallback != null && grantResults.isNotEmpty()) {
                permissionCallback!!.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    /**
     * Contains the basic / non interactive tutorial fragments.
     */
    class SimpleTutorialFragment : Fragment() {


        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val args = arguments
            return when (args!!.getInt(ARG_SECTION_NUMBER, 0)) {
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
    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager) : FragmentPagerAdapter(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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
