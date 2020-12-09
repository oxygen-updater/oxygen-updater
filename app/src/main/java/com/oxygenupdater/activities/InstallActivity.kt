package com.oxygenupdater.activities

import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.oxygenupdater.R
import com.oxygenupdater.extensions.setImageResourceWithAnimationAndTint
import com.oxygenupdater.fragments.InstallGuideFragment
import com.oxygenupdater.fragments.InstallMethodChooserFragment
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.RootAccessChecker
import com.oxygenupdater.viewmodels.InstallViewModel
import kotlinx.android.synthetic.main.activity_install.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class InstallActivity : SupportActionBarActivity(
    R.layout.activity_install,
    MainActivity.PAGE_UPDATE
) {

    private var showDownloadPage = true
    private var updateData: UpdateData? = null

    private val installViewModel by viewModel<InstallViewModel>()

    private val appBarOffsetChangeListenerForMethodChooserFragment = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        fragmentContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            bottomMargin = appBar.totalScrollRange - abs(verticalOffset)
        }
    }

    private val appBarOffsetChangeListenerForViewPager = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            bottomMargin = appBar.totalScrollRange - abs(verticalOffset)
        }
    }

    private val installGuidePageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = handleInstallGuidePageChangeCallback(position)
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        showDownloadPage = intent == null || intent.getBooleanExtra(INTENT_SHOW_DOWNLOAD_PAGE, true)

        if (intent != null) {
            updateData = intent.getParcelableExtra(INTENT_UPDATE_DATA)
        }

        installViewModel.firstInstallGuidePageLoaded.observe(this) {
            if (it) {
                handleInstallGuidePageChangeCallback(0)
            }
        }

        installViewModel.toolbarTitle.observe(this) {
            collapsingToolbarLayout.title = getString(it)
        }

        installViewModel.toolbarSubtitle.observe(this) {
            collapsingToolbarLayout.subtitle = if (it != null) getString(it) else null
        }

        installViewModel.toolbarImage.observe(this) {
            collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                it.first,
                android.R.anim.fade_in,
                if (it.second) R.color.colorPrimary else null
            )
        }

        initialize()
    }

    private fun initialize() {
        RootAccessChecker.checkRootAccess { isRooted ->
            if (isFinishing) {
                return@checkRootAccess
            }

            rootStatusCheckLayout.isVisible = false

            if (isRooted) {
                installViewModel.fetchServerStatus().observe(this) { serverStatus ->
                    if (serverStatus.automaticInstallationEnabled) {
                        openMethodSelectionPage()
                    } else {
                        Toast.makeText(this, getString(R.string.install_guide_automatic_install_disabled), LENGTH_LONG).show()
                        openInstallGuide()
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.install_guide_no_root), LENGTH_LONG).show()
                openInstallGuide()
            }
        }
    }

    private fun openMethodSelectionPage() = supportFragmentManager.commit {
        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        replace(
            R.id.fragmentContainer,
            InstallMethodChooserFragment().apply {
                arguments = bundleOf(INTENT_UPDATE_DATA to updateData)
            },
            INSTALL_METHOD_CHOOSER_FRAGMENT_TAG
        )
        addToBackStack(INSTALL_METHOD_CHOOSER_FRAGMENT_TAG)
    }

    fun setupAppBarForMethodChooserFragment() = appBar.post {
        // adjust bottom margin on first load
        fragmentContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = appBar.totalScrollRange }

        // adjust bottom margin on scroll
        appBar.addOnOffsetChangedListener(appBarOffsetChangeListenerForMethodChooserFragment)
    }.also {
        setNavBarColorToBackground()
    }

    private fun setupAppBarForViewPager() = appBar.post {
        // adjust bottom margin on first load
        viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = appBar.totalScrollRange }

        // adjust bottom margin on scroll
        appBar.addOnOffsetChangedListener(appBarOffsetChangeListenerForViewPager)
    }.also {
        setNavBarColorToBackgroundVariant()
    }

    fun resetAppBarForMethodChooserFragment() = appBar.post {
        // reset bottom margin on first load
        fragmentContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = 0 }

        // remove listener
        appBar.removeOnOffsetChangedListener(appBarOffsetChangeListenerForMethodChooserFragment)
    }

    fun resetAppBarForViewPager() = appBar.post {
        // reset bottom margin on first load
        viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = 0 }

        // remove listener
        appBar.removeOnOffsetChangedListener(appBarOffsetChangeListenerForViewPager)
    }

    fun openInstallGuide() = setupViewPager()

    private fun hideViewPager() {
        fragmentContainer.isVisible = true
        viewPagerContainer.isVisible = false
        resetAppBarForViewPager()
    }

    private fun setupViewPager() {
        fragmentContainer.isVisible = false
        viewPagerContainer.isVisible = true

        viewPager.apply {
            offscreenPageLimit = 4 // Install guide is 5 pages max. So there can be only 4 off-screen
            adapter = InstallGuidePagerAdapter()

            // attach TabLayout to ViewPager2
            TabLayoutMediator(tabLayout, this) { _, _ -> }.attach()

            registerOnPageChangeCallback(installGuidePageChangeCallback)
        }

        setupButtonsForViewPager()
        resetAppBarForMethodChooserFragment()
        setupAppBarForViewPager()
    }

    private fun setupButtonsForViewPager() {
        previousPageButton.setOnClickListener {
            viewPager.currentItem--
        }

        nextPageButton.setOnClickListener {
            val lastPageNumber = if (showDownloadPage) {
                NUMBER_OF_INSTALL_GUIDE_PAGES
            } else {
                NUMBER_OF_INSTALL_GUIDE_PAGES - 1
            }

            if (viewPager.currentItem == lastPageNumber - 1) {
                onBackPressed()
            } else {
                viewPager.currentItem++
            }
        }
    }

    private fun handleInstallGuidePageChangeCallback(position: Int) {
        previousPageButton.isEnabled = position != 0

        nextPageButton.apply {
            val lastPageNumber = if (showDownloadPage) {
                NUMBER_OF_INSTALL_GUIDE_PAGES
            } else {
                NUMBER_OF_INSTALL_GUIDE_PAGES - 1
            }

            rotation = if (position == lastPageNumber - 1) {
                setImageResource(R.drawable.done)
                0f
            } else {
                setImageResource(R.drawable.expand)
                90f
            }
        }

        val pageNumber = position + if (showDownloadPage) 1 else 2

        installViewModel.installGuideCache[pageNumber]?.run {
            collapsingToolbarLayout.title = title
            collapsingToolbarLayout.subtitle = getString(
                R.string.install_guide_subtitle,
                position + 1,
                if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1
            )

            if (!isDefaultPage && useCustomImage) {
                // Fetch the custom image from the server.
                Glide.with(this@InstallActivity)
                    .load(completeImageUrl(imageUrl, fileExtension))
                    // Load a "no entry" sign to show that the image failed to load.
                    .error(R.drawable.no_entry)
                    .into(collapsingToolbarImage)

                collapsingToolbarImage.apply {
                    imageTintList = null
                }
            } else {
                installViewModel.updateToolbarImage(
                    resources.getIdentifier(
                        RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_IMAGE,
                        RESOURCE_ID_PACKAGE_DRAWABLE,
                        packageName
                    )
                )
            }
        }
    }

    private fun completeImageUrl(imageUrl: String?, fileExtension: String?): String {
        val imageVariant: String = when (resources.displayMetrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> IMAGE_VARIANT_LDPI
            DisplayMetrics.DENSITY_MEDIUM -> IMAGE_VARIANT_MDPI
            DisplayMetrics.DENSITY_TV -> IMAGE_VARIANT_TVDPI
            DisplayMetrics.DENSITY_HIGH -> IMAGE_VARIANT_HDPI
            DisplayMetrics.DENSITY_280, DisplayMetrics.DENSITY_XHIGH -> IMAGE_VARIANT_XHDPI
            DisplayMetrics.DENSITY_360, DisplayMetrics.DENSITY_400, DisplayMetrics.DENSITY_420, DisplayMetrics.DENSITY_XXHIGH -> IMAGE_VARIANT_XXHDPI
            DisplayMetrics.DENSITY_560, DisplayMetrics.DENSITY_XXXHIGH -> IMAGE_VARIANT_XXXHDPI
            else -> IMAGE_VARIANT_DEFAULT
        }

        return "${imageUrl}_$imageVariant.$fileExtension"
    }

    override fun onDestroy() = super.onDestroy().also {
        viewPager?.unregisterOnPageChangeCallback(installGuidePageChangeCallback)
    }

    /**
     * Handles the following cases:
     * * Once the installation is being started, there is no way out => do nothing
     * * If [InstallGuideFragment] is being displayed, and was opened from [InstallMethodChooserFragment], switch back to [fragmentContainer]
     * * If [InstallGuideFragment] was opened directly after checking for root (meaning if the device isn't rooted), delegate to `super`
     * * If [InstallMethodChooserFragment] is the only fragment being displayed (meaning [FragmentManager.getBackStackEntryCount] returns `1`, delegate to `super`
     * * Otherwise just call `super`, which respects [FragmentManager]'s back stack
     */
    override fun onBackPressed() = when {
        // Once the installation is being started, there is no way out.
        !installViewModel.canGoBack -> Toast.makeText(this, R.string.install_going_back_not_possible, LENGTH_LONG).show()
        // if InstallGuide is being displayed, and was opened from `InstallMethodChooserFragment`, switch back to fragmentContainer
        // if it was opened directly after checking for root (meaning if the device isn't rooted), delegate to `super`
        viewPagerContainer.isVisible -> {
            if (supportFragmentManager.backStackEntryCount == 0) {
                super.onBackPressed()
            } else {
                hideViewPager()

                // update toolbar state to reflect values set in `InstallMethodChooserFragment`
                installViewModel.updateToolbarTitle(R.string.install_method_chooser_title)
                installViewModel.updateToolbarSubtitle(R.string.install_method_chooser_subtitle)
                installViewModel.updateToolbarImage(R.drawable.list_select, true)

                setupAppBarForMethodChooserFragment()

                viewPager.unregisterOnPageChangeCallback(installGuidePageChangeCallback)
            }
        }
        supportFragmentManager.backStackEntryCount == 1 -> super.onBackPressed()
        else -> super.onBackPressed()
    }

    companion object {
        private const val RESOURCE_ID_IMAGE = "_image"
        private const val RESOURCE_ID_PACKAGE_DRAWABLE = "drawable"

        private const val IMAGE_VARIANT_LDPI = "ldpi"
        private const val IMAGE_VARIANT_MDPI = "mdpi"
        private const val IMAGE_VARIANT_TVDPI = "tvdpi"
        private const val IMAGE_VARIANT_HDPI = "hdpi"
        private const val IMAGE_VARIANT_XHDPI = "xhdpi"
        private const val IMAGE_VARIANT_XXHDPI = "xxhdpi"
        private const val IMAGE_VARIANT_XXXHDPI = "xxxhdpi"
        private const val IMAGE_VARIANT_DEFAULT = "default"

        const val RESOURCE_ID_PREFIX = "install_guide_page_"

        const val INTENT_SHOW_DOWNLOAD_PAGE = "show_download_page"
        const val INTENT_UPDATE_DATA = "update_data"

        const val NUMBER_OF_INSTALL_GUIDE_PAGES = 5

        const val INSTALL_METHOD_CHOOSER_FRAGMENT_TAG = "InstallMethodChooser"
        const val AUTOMATIC_INSTALL_FRAGMENT_TAG = "AutomaticInstall"
    }

    /**
     * A [FragmentStateAdapter] that returns a fragment corresponding to one of the sections/tabs/pages
     */
    private inner class InstallGuidePagerAdapter : FragmentStateAdapter(this) {

        /**
         * This is called to instantiate the fragment for the given page.
         * Returns an instance of [InstallGuideFragment]
         */
        override fun createFragment(position: Int) = InstallGuideFragment.newInstance(
            (position + if (showDownloadPage) 1 else 2),
            position == 0
        )

        /**
         * Show the predefined amount of total pages
         */
        override fun getItemCount() = if (showDownloadPage) {
            NUMBER_OF_INSTALL_GUIDE_PAGES
        } else {
            NUMBER_OF_INSTALL_GUIDE_PAGES - 1
        }
    }
}
