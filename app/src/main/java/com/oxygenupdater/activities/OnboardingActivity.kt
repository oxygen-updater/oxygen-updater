package com.oxygenupdater.activities

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ImageView.ScaleType
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.OxygenUpdater.Companion.VERIFY_FILE_PERMISSION
import com.oxygenupdater.R
import com.oxygenupdater.dialogs.ContributorDialogFragment
import com.oxygenupdater.extensions.enableEdgeToEdgeUiSupport
import com.oxygenupdater.extensions.reduceDragSensitivity
import com.oxygenupdater.extensions.setImageResourceWithAnimationAndTint
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.fragments.DeviceChooserOnboardingFragment
import com.oxygenupdater.fragments.SimpleOnboardingFragment
import com.oxygenupdater.fragments.UpdateMethodChooserOnboardingFragment
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.SetupUtils
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.viewmodels.OnboardingViewModel
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_onboarding_complete.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*
import kotlin.math.abs

class OnboardingActivity : BaseActivity(R.layout.activity_onboarding) {

    private lateinit var viewPagerAdapter: OnboardingPagerAdapter

    private val startPage by lazy {
        intent?.getIntExtra(
            MainActivity.INTENT_START_PAGE,
            MainActivity.PAGE_UPDATE
        ) ?: MainActivity.PAGE_UPDATE
    }

    private val contributorDialog by lazy {
        ContributorDialogFragment()
    }

    private val onboardingViewModel by viewModel<OnboardingViewModel>()
    private val systemVersionProperties by inject<SystemVersionProperties>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(
            position: Int
        ) = handlePageChangeCallback(position + 1)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        RequestPermission()
    ) {
        logInfo(TAG, "`$VERIFY_FILE_PERMISSION` granted: $it")

        if (it) {
            // 1st time, will save setting to true.
            ContributorUtils.flushSettings(true)
            SettingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
            startMainActivity(startPage)
            finish()
        } else {
            Toast.makeText(this, R.string.contribute_allow_storage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        enableEdgeToEdgeUiSupport()
        window.navigationBarColor = ContextCompat.getColor(
            this,
            R.color.backgroundVariant
        )

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
                setImageResource(R.drawable.done)
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
                collapsingToolbarImage.scaleType = ScaleType.CENTER_CROP
                collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                    R.drawable.logo_notification,
                    android.R.anim.fade_in,
                    R.color.colorPrimary
                )
            }
            2 -> {
                nextPageButton.isEnabled = viewPagerAdapter.numberOfPages > 2

                val resourceName = systemVersionProperties.oxygenDeviceName.replace(
                    "(?:^OnePlus|^OP|Single\$|NR(?:Spr)?\$|TMO\$|VZW\$|_\\w+\$| )".toRegex(RegexOption.IGNORE_CASE),
                    ""
                ).toLowerCase(Locale.ROOT)

                var imageResId = resources.getIdentifier(
                    "oneplus$resourceName",
                    "drawable",
                    packageName
                )

                // If no image was found default to the latest device image
                if (imageResId == 0) {
                    imageResId = R.drawable.oneplus8t
                }

                collapsingToolbarLayout.title = getString(R.string.onboarding_page_2_title)
                collapsingToolbarImage.scaleType = ScaleType.FIT_CENTER
                collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                    imageResId,
                    android.R.anim.fade_in
                )
            }
            3 -> {
                nextPageButton.isEnabled = viewPagerAdapter.numberOfPages > 3

                collapsingToolbarLayout.title = getString(R.string.onboarding_page_3_title)
                collapsingToolbarImage.scaleType = ScaleType.CENTER_CROP
                collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                    R.drawable.cloud_download,
                    android.R.anim.fade_in,
                    R.color.colorPrimary
                )
            }
            4 -> {
                nextPageButton.isEnabled = true

                collapsingToolbarLayout.title = getString(R.string.onboarding_page_4_title)
                collapsingToolbarImage.scaleType = ScaleType.CENTER_CROP
                collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                    R.drawable.done_all,
                    android.R.anim.fade_in,
                    R.color.colorPrimary
                )
            }
            else -> {
                // no-op
            }
        }
    }

    /**
     * Show the dialog fragment only if it hasn't been added already. This
     * can happen if the user clicks in rapid succession, which can cause
     * the `java.lang.IllegalStateException: Fragment already added` error
     */
    @Suppress("UNUSED_PARAMETER")
    fun onMoreInfoButtonClicked(view: View) {
        if (!contributorDialog.isAdded) {
            contributorDialog.show(
                supportFragmentManager,
                ContributorDialogFragment.TAG
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onStartAppButtonClicked(view: View) {
        if (SettingsManager.checkIfSetupScreenIsFilledIn()) {
            onboardingPage4LogsCheckbox.isChecked.let {
                SettingsManager.savePreference(
                    SettingsManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS,
                    it
                )

                (application as OxygenUpdater?)?.setupCrashReporting(it)
            }

            if (onboardingPage4ContributeCheckbox.isChecked) {
                requestPermissionLauncher.launch(VERIFY_FILE_PERMISSION)
            } else {
                // not signed up, saving this setting will prevent contribute popups which belong to app updates.
                SettingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
                SettingsManager.savePreference(SettingsManager.PROPERTY_CONTRIBUTE, false)
                startMainActivity(startPage)
                finish()
            }
        } else {
            val deviceId = SettingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
            val updateMethodId = SettingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
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
