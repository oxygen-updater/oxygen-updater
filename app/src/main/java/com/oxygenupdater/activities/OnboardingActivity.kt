package com.oxygenupdater.activities

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.databinding.ActivityOnboardingBinding
import com.oxygenupdater.extensions.enableEdgeToEdgeUiSupport
import com.oxygenupdater.extensions.reduceDragSensitivity
import com.oxygenupdater.extensions.setImageResourceWithAnimationAndTint
import com.oxygenupdater.extensions.startMainActivity
import com.oxygenupdater.fragments.DeviceChooserOnboardingFragment
import com.oxygenupdater.fragments.SimpleOnboardingFragment
import com.oxygenupdater.fragments.UpdateMethodChooserOnboardingFragment
import com.oxygenupdater.internal.DeviceInformationData
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.SetupUtils
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.viewmodels.OnboardingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class OnboardingActivity : BaseActivity(R.layout.activity_onboarding) {

    private lateinit var viewPagerAdapter: OnboardingPagerAdapter
    private var deviceImageUrl: String? = null

    private val startPage by lazy(LazyThreadSafetyMode.NONE) {
        intent?.getIntExtra(
            MainActivity.INTENT_START_PAGE,
            MainActivity.PAGE_UPDATE
        ) ?: MainActivity.PAGE_UPDATE
    }

    private val onboardingViewModel by viewModel<OnboardingViewModel>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(
            position: Int
        ) = handlePageChangeCallback(position + 1)
    }

    private lateinit var binding: ActivityOnboardingBinding
    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        binding = ActivityOnboardingBinding.bind(rootView)
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

            val deviceName = it?.find { device ->
                device.productNames.contains(SystemVersionProperties.oxygenDeviceName)
            }?.name ?: getString(
                R.string.device_information_device_name,
                DeviceInformationData.deviceManufacturer,
                DeviceInformationData.deviceName
            )

            deviceImageUrl = Device.constructImageUrl(deviceName)
        }

        setupViewPager()
    }

    override fun onDestroy() = super.onDestroy().also {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = if (binding.viewPager.currentItem == 0) {
        // If the user is currently looking at the first step, allow the system to handle the
        // Back button. This calls finish() on this activity and pops the back stack.
        super.onBackPressed()
    } else {
        // Otherwise, select the previous step.
        binding.viewPager.currentItem -= 1
    }

    private fun setupViewPager() {
        binding.viewPager.apply {
            // create the adapter that will return a fragment for each of the pages of the activity.
            adapter = OnboardingPagerAdapter().also { viewPagerAdapter = it }
            reduceDragSensitivity()

            // attach TabLayout to ViewPager2
            TabLayoutMediator(binding.tabLayout, this) { _, _ -> }.attach()

            registerOnPageChangeCallback(pageChangeCallback)
        }

        setupButtonsForViewPager()
        setupAppBarForViewPager()
        setupObserversForViewPagerAdapter()
    }

    private fun setupButtonsForViewPager() {
        binding.previousPageButton.setOnClickListener {
            binding.viewPager.currentItem = if (binding.viewPager.currentItem == 0) {
                0
            } else {
                binding.viewPager.currentItem - 1
            }
        }

        binding.nextPageButton.setOnClickListener {
            if (binding.viewPager.currentItem == 3) {
                onStartAppButtonClicked(it)
            } else {
                binding.viewPager.currentItem++
            }
        }
    }

    private fun setupAppBarForViewPager() {
        binding.appBar.post {
            val totalScrollRange = binding.appBar.totalScrollRange

            // adjust bottom margin on first load
            binding.viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = totalScrollRange }

            // adjust bottom margin on scroll
            binding.appBar.addOnOffsetChangedListener { _, verticalOffset ->
                binding.viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    bottomMargin = totalScrollRange - abs(verticalOffset)
                }
            }
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

        onboardingViewModel.fragmentCreated.observe(this) {
            if (it == 4) {
                findViewById<Button>(R.id.onboardingPage4StartAppButton)?.setOnClickListener(this::onStartAppButtonClicked)
            }
        }
    }

    private fun handlePageChangeCallback(pageNumber: Int) {
        binding.previousPageButton.isEnabled = pageNumber != 1

        binding.nextPageButton.apply {
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
                binding.nextPageButton.isEnabled = true

                binding.collapsingToolbarLayout.title = getString(R.string.onboarding_page_1_title)
                binding.collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                    R.drawable.logo_notification,
                    android.R.anim.fade_in,
                    R.color.colorPrimary
                )
            }
            2 -> {
                binding.nextPageButton.isEnabled = viewPagerAdapter.numberOfPages > 2

                binding.collapsingToolbarLayout.title = getString(R.string.onboarding_page_2_title)
                binding.collapsingToolbarImage.apply {
                    imageTintList = null
                    load(deviceImageUrl) {
                        placeholder(R.drawable.oneplus7pro)
                        error(R.drawable.oneplus7pro)
                    }
                }
            }
            3 -> {
                binding.nextPageButton.isEnabled = viewPagerAdapter.numberOfPages > 3

                binding.collapsingToolbarLayout.title = getString(R.string.onboarding_page_3_title)
                binding.collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                    R.drawable.cloud_download,
                    android.R.anim.fade_in,
                    R.color.colorPrimary
                )
            }
            4 -> {
                binding.nextPageButton.isEnabled = true

                binding.collapsingToolbarLayout.title = getString(R.string.onboarding_page_4_title)
                binding.collapsingToolbarImage.setImageResourceWithAnimationAndTint(
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

    @Suppress("UNUSED_PARAMETER")
    fun onStartAppButtonClicked(view: View) {
        if (PrefManager.checkIfSetupScreenIsFilledIn()) {
            findViewById<CheckBox>(R.id.onboardingPage4LogsCheckbox).isChecked.let {
                PrefManager.putBoolean(
                    PrefManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS,
                    it
                )

                (application as OxygenUpdater?)?.setupCrashReporting(it)
            }

            PrefManager.putBoolean(PrefManager.PROPERTY_SETUP_DONE, true)
            startMainActivity(startPage)
            finish()
        } else {
            val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L)
            val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)
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

                when (binding.viewPager.currentItem) {
                    0 -> {
                        binding.previousPageButton.isEnabled = false
                        binding.nextPageButton.isEnabled = true
                    }
                    field - 1 -> {
                        binding.previousPageButton.isEnabled = true
                        binding.nextPageButton.isEnabled = false
                    }
                    else -> {
                        binding.previousPageButton.isEnabled = true
                        binding.nextPageButton.isEnabled = true
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
