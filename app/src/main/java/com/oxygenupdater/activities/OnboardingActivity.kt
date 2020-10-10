package com.oxygenupdater.activities

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.oxygenupdater.ActivityLauncher
import com.oxygenupdater.R
import com.oxygenupdater.dialogs.Dialogs
import com.oxygenupdater.extensions.enableEdgeToEdgeUiSupport
import com.oxygenupdater.extensions.reduceDragSensitivity
import com.oxygenupdater.extensions.setImageResourceWithAnimation
import com.oxygenupdater.fragments.DeviceChooserOnboardingFragment
import com.oxygenupdater.fragments.SimpleOnboardingFragment
import com.oxygenupdater.fragments.UpdateMethodChooserOnboardingFragment
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.SetupUtils
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.viewmodels.OnboardingViewModel
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_onboarding_complete.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class OnboardingActivity : AppCompatActivity(R.layout.activity_onboarding) {

    private lateinit var viewPagerAdapter: OnboardingPagerAdapter

    private var permissionCallback: KotlinCallback<Boolean>? = null

    private val settingsManager by inject<SettingsManager>()
    private val onboardingViewModel by viewModel<OnboardingViewModel>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(
            position: Int
        ) = handlePageChangeCallback(position + 1)
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        enableEdgeToEdgeUiSupport()

        onboardingViewModel.fetchAllDevices().observe(this) {
            val deviceOsSpec = Utils.checkDeviceOsSpec(it)

            if (!deviceOsSpec.isDeviceOsSpecSupported) {
                displayUnsupportedDeviceOsSpecMessage(deviceOsSpec)
            }
        }

        setupViewPager()
    }

    override fun onDestroy() = super.onDestroy().also {
        viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
    }

    override fun onBackPressed() = if (viewPager.currentItem == 0) {
        // If the user is currently looking at the first step, allow the system to handle the
        // Back button. This calls finish() on this activity and pops the back stack.
        super.onBackPressed()
    } else {
        // Otherwise, select the previous step.
        viewPager.currentItem = viewPager.currentItem - 1
    }

    private fun setupViewPager() {
        viewPager.apply {
            // create the adapter that will return a fragment for each of the pages of the activity.
            adapter = OnboardingPagerAdapter().also { viewPagerAdapter = it }
            reduceDragSensitivity()

            // attach TabLayout to ViewPager2
            TabLayoutMediator(tabLayout, this) { _, _ -> }.attach()

            registerOnPageChangeCallback(pageChangeCallback)
        }

        setupButtonsForViewPager()
        setupAppBarForViewPager()
        setupObserversForViewPagerAdapter()
    }

    private fun setupButtonsForViewPager() {
        previousPageButton.setOnClickListener {
            viewPager.currentItem = if (viewPager.currentItem == 0) {
                0
            } else {
                viewPager.currentItem - 1
            }
        }

        nextPageButton.setOnClickListener {
            if (viewPager.currentItem == 3) {
                onStartAppButtonClicked(it)
            } else {
                viewPager.currentItem++
            }
        }
    }

    private fun setupAppBarForViewPager() {
        appBar.post {
            val totalScrollRange = appBar.totalScrollRange

            // adjust bottom margin on first load
            viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = totalScrollRange }

            // adjust bottom margin on scroll
            appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    bottomMargin = totalScrollRange - abs(verticalOffset)
                }
            })
        }
    }

    private fun setupObserversForViewPagerAdapter() {
        onboardingViewModel.enabledDevices.observe(this) {
            if (it == null) {
                viewPagerAdapter.numberOfPages = 2
                logDebug(TAG, "Couldn't retrieve supported devices. Hiding last 2 fragments.")
            } else {
                if (viewPagerAdapter.numberOfPages <= 2) {
                    viewPagerAdapter.numberOfPages = 3
                    logDebug(TAG, "Retrieved ${it.size} supported devices. Showing the next fragment.")
                } else {
                    logDebug(TAG, "Retrieved ${it.size} supported devices.")
                }
            }
        }

        onboardingViewModel.selectedDevice.observe(this) {
            if (viewPagerAdapter.numberOfPages <= 2) {
                viewPagerAdapter.numberOfPages = 3
                logDebug(TAG, "Selected device: $it. Showing the next fragment.")
            } else {
                logDebug(TAG, "Selected device: $it")
            }
        }

        onboardingViewModel.selectedUpdateMethod.observe(this) {
            if (viewPagerAdapter.numberOfPages <= 3) {
                viewPagerAdapter.numberOfPages = 4
                logDebug(TAG, "Selected update method: $it. Showing the next fragment.")
            } else {
                logDebug(TAG, "Selected update method: $it")
            }
        }
    }

    private fun handlePageChangeCallback(pageNumber: Int) {
        previousPageButton.isEnabled = pageNumber != 1

        nextPageButton.apply {
            rotation = if (pageNumber == 4) {
                setImageResource(R.drawable.checkmark)
                0f
            } else {
                setImageResource(R.drawable.expand)
                90f
            }
        }

        when (pageNumber) {
            1 -> {
                nextPageButton.isEnabled = true

                collapsingToolbarLayout.title = getString(R.string.onboarding_page_1_title)
                collapsingToolbarImage.setImageResourceWithAnimation(R.drawable.logo_outline, android.R.anim.fade_in)
            }
            2 -> {
                nextPageButton.isEnabled = viewPagerAdapter.numberOfPages > 2

                collapsingToolbarLayout.title = getString(R.string.onboarding_page_2_title)
                collapsingToolbarImage.setImageResourceWithAnimation(R.drawable.smartphone, android.R.anim.fade_in)
            }
            3 -> {
                nextPageButton.isEnabled = viewPagerAdapter.numberOfPages > 3

                collapsingToolbarLayout.title = getString(R.string.onboarding_page_3_title)
                collapsingToolbarImage.setImageResourceWithAnimation(R.drawable.cloud_download, android.R.anim.fade_in)
            }
            4 -> {
                nextPageButton.isEnabled = true

                collapsingToolbarLayout.title = getString(R.string.onboarding_page_4_title)
                collapsingToolbarImage.setImageResourceWithAnimation(R.drawable.done_all, android.R.anim.fade_in)
            }
            else -> {
                // no-op
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onMoreInfoButtonClicked(view: View) = Dialogs.showContributorExplanation(this)

    @Suppress("UNUSED_PARAMETER")
    fun onStartAppButtonClicked(view: View) {
        if (settingsManager.checkIfSetupScreenIsFilledIn()) {
            if (onboardingPage4ContributeCheckbox.isChecked) {
                requestContributorStoragePermissions { granted: Boolean ->
                    if (granted) {
                        // 1st time, will save setting to true.
                        ContributorUtils.flushSettings(true)
                        settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
                        ActivityLauncher(this).Main()
                        finish()
                    } else {
                        Toast.makeText(this, R.string.contribute_allow_storage, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // not signed up, saving this setting will prevent contribute popups which belong to app updates.
                settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
                settingsManager.savePreference(SettingsManager.PROPERTY_CONTRIBUTE, false)
                ActivityLauncher(this).Main()
                finish()
            }
        } else {
            val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
            val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
            logWarning(TAG, SetupUtils.getAsError("Setup wizard", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
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

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.unsupported_device_warning_title))
            .setMessage(getString(resourceId))
            .setPositiveButton(getString(R.string.download_error_close)) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun requestContributorStoragePermissions(permissionCallback: KotlinCallback<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.permissionCallback = permissionCallback
            requestPermissions(arrayOf(MainActivity.VERIFY_FILE_PERMISSION), MainActivity.PERMISSION_REQUEST_CODE)
        } else {
            permissionCallback.invoke(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) = super.onRequestPermissionsResult(requestCode, permissions, grantResults).also {
        if (grantResults.isNotEmpty() && requestCode == MainActivity.PERMISSION_REQUEST_CODE) {
            permissionCallback?.invoke(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    /**
     * A [FragmentStateAdapter] that returns a fragment corresponding to one of the sections/tabs/pages.
     */
    inner class OnboardingPagerAdapter : FragmentStateAdapter(this) {

        /**
         * Used to hide fragments if required. Done in the following cases:
         * * Devices couldn't be retrieved from the server, or if the user hasn't selected a device yet
         *   Set to 2, to hide the last 2 fragments
         * * Update methods couldn't be retrieved from the server, or if the user hasn't selected an update method yet
         *   Set to 3, to hide the last fragment
         *
         * This implicitly calls [notifyDataSetChanged] if the value changes.
         */
        var numberOfPages = 2
            set(value) {
                field = value
                notifyDataSetChanged()

                when (viewPager.currentItem) {
                    0 -> {
                        previousPageButton.isEnabled = false
                        nextPageButton.isEnabled = true
                    }
                    field - 1 -> {
                        previousPageButton.isEnabled = true
                        nextPageButton.isEnabled = false
                    }
                    else -> {
                        previousPageButton.isEnabled = true
                        nextPageButton.isEnabled = true
                    }
                }
            }

        /**
         * This is called to instantiate the fragment for the given page.
         * Return one of:
         * * [DeviceChooserOnboardingFragment]
         * * [UpdateMethodChooserOnboardingFragment]
         * * [SimpleOnboardingFragment]
         */
        override fun createFragment(position: Int) = (position + 1).let { pageNumber ->
            when (pageNumber) {
                2 -> DeviceChooserOnboardingFragment()
                3 -> UpdateMethodChooserOnboardingFragment()
                else -> SimpleOnboardingFragment.newInstance(pageNumber)
            }
        }

        /**
         * Show 4 total pages: Welcome screen, device selection, update method selection, and completion screen
         */
        override fun getItemCount() = numberOfPages
    }

    companion object {
        private const val TAG = "OnboardingActivity"
    }
}
