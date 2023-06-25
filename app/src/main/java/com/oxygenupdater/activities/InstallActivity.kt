package com.oxygenupdater.activities

import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.oxygenupdater.R
import com.oxygenupdater.compose.activities.MainActivity
import com.oxygenupdater.databinding.ActivityInstallBinding
import com.oxygenupdater.extensions.setImageResourceWithAnimationAndTint
import com.oxygenupdater.fragments.InstallGuideFragment
import com.oxygenupdater.viewmodels.InstallViewModel
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

    private val installViewModel by viewModel<InstallViewModel>()

    private val appBarOffsetChangeListenerForViewPager = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
        binding.viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            bottomMargin = binding.appBar.totalScrollRange - abs(verticalOffset)
        }
    }

    private val installGuidePageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = handleInstallGuidePageChangeCallback(position)
    }

    private lateinit var binding: ActivityInstallBinding
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) = super.onCreate(savedInstanceState).also {
        binding = ActivityInstallBinding.bind(rootView)
        showDownloadPage = intent == null || intent.getBooleanExtra(INTENT_SHOW_DOWNLOAD_PAGE, true)

        installViewModel.firstInstallGuidePageLoaded.observe(this) {
            if (it) {
                handleInstallGuidePageChangeCallback(0)
            }
        }

        installViewModel.toolbarTitle.observe(this) {
            binding.collapsingToolbarLayout.title = getString(it)
        }

        installViewModel.toolbarSubtitle.observe(this) {
            binding.collapsingToolbarLayout.subtitle = if (it != null) getString(it) else null
        }

        installViewModel.toolbarImage.observe(this) {
            binding.collapsingToolbarImage.setImageResourceWithAnimationAndTint(
                it.first,
                android.R.anim.fade_in,
                if (it.second) R.color.colorPrimary else null
            )
        }

        Toast.makeText(this, getString(R.string.install_guide_no_root), LENGTH_LONG).show()
        setupViewPager()
    }

    private fun setupAppBarForViewPager() = binding.appBar.post {
        // adjust bottom margin on first load
        binding.viewPagerContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            bottomMargin = binding.appBar.totalScrollRange
        }

        // adjust bottom margin on scroll
        binding.appBar.addOnOffsetChangedListener(appBarOffsetChangeListenerForViewPager)
    }.also {
        setNavBarColorToBackgroundVariant()
    }

    private fun setupViewPager() {
        binding.viewPagerContainer.isVisible = true

        binding.viewPager.apply {
            offscreenPageLimit = 4 // Install guide is 5 pages max. So there can be only 4 off-screen
            adapter = InstallGuidePagerAdapter()

            // attach TabLayout to ViewPager2
            TabLayoutMediator(binding.tabLayout, this) { _, _ -> }.attach()

            registerOnPageChangeCallback(installGuidePageChangeCallback)
        }

        setupButtonsForViewPager()
        setupAppBarForViewPager()
    }

    private fun setupButtonsForViewPager() {
        binding.previousPageButton.setOnClickListener {
            binding.viewPager.currentItem--
        }

        binding.nextPageButton.setOnClickListener {
            val lastPageNumber = if (showDownloadPage) {
                NUMBER_OF_INSTALL_GUIDE_PAGES
            } else {
                NUMBER_OF_INSTALL_GUIDE_PAGES - 1
            }

            if (binding.viewPager.currentItem == lastPageNumber - 1) {
                onBackPressed()
            } else {
                binding.viewPager.currentItem++
            }
        }
    }

    private fun handleInstallGuidePageChangeCallback(position: Int) {
        binding.previousPageButton.isEnabled = position != 0

        binding.nextPageButton.apply {
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
            binding.collapsingToolbarLayout.title = title
            binding.collapsingToolbarLayout.subtitle = getString(
                R.string.install_guide_subtitle,
                position + 1,
                if (showDownloadPage) NUMBER_OF_INSTALL_GUIDE_PAGES else NUMBER_OF_INSTALL_GUIDE_PAGES - 1
            )

            if (!isDefaultPage && useCustomImage) {
                // Fetch the custom image from the server.
                binding.collapsingToolbarImage.apply {
                    imageTintList = null
                    load(completeImageUrl(imageUrl, fileExtension)) {
                        error(R.drawable.no_entry)
                    }
                }
            } else {
                installViewModel.updateToolbarImage(
                    if (pageNumber <= 1) R.drawable.download
                    else if (pageNumber == 2) R.drawable.install_guide_app_in_list
                    else if (pageNumber == 3 || pageNumber == 4) R.drawable.install_guide_installing
                    else if (pageNumber >= 5) R.drawable.done_all
                    else R.drawable.logo_notification,
                    pageNumber <= 1 || pageNumber >= 5 // only first/last page
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
        binding.viewPager.unregisterOnPageChangeCallback(installGuidePageChangeCallback)
    }

    companion object {
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

        const val NUMBER_OF_INSTALL_GUIDE_PAGES = 5
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
