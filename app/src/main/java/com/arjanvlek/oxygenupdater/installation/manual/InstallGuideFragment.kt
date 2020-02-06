package com.arjanvlek.oxygenupdater.installation.manual

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.installation.InstallActivity
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.models.AppLocale
import com.arjanvlek.oxygenupdater.models.AppLocale.NL
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.fragment_install_guide.*

class InstallGuideFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val installGuideView = inflater.inflate(R.layout.fragment_install_guide, container, false)

        var pageNumber = 1
        var isFirstPage = false

        arguments?.let {
            pageNumber = it.getInt(ARG_PAGE_NUMBER, 1)
            isFirstPage = it.getBoolean(ARG_IS_FIRST_PAGE, false)
        }

        val settingsManager = SettingsManager(context)

        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        val cache = if (activity is InstallActivity) {
            (activity as InstallActivity?)!!.installGuideCache
        } else {
            logWarning(TAG, OxygenUpdaterException("getActivity() returned null or was not an instance of InstallActivity (onCreateView, getInstallGuideCache)"))
            SparseArray()
        }

        if (cache[pageNumber] == null) {
            activity?.application?.let { application ->
                if (application is ApplicationData) {
                    application.serverConnector!!.getInstallGuidePage(deviceId, updateMethodId, pageNumber) { page ->
                        cache.put(pageNumber, page)
                        displayInstallGuide(page, pageNumber, isFirstPage)
                    }
                }
            }
        } else {
            displayInstallGuide(cache[pageNumber], pageNumber, isFirstPage)
        }

        return installGuideView
    }

    private fun displayInstallGuide(installGuidePage: InstallGuidePage?, pageNumber: Int, isFirstPage: Boolean) {
        if (!isAdded) {
            // Happens when a page is scrolled too far outside the screen (2 or more rows) and content then gets resolved from the server.
            logDebug(TAG, "isAdded() returned false (displayInstallGuide)")
            return
        }

        if (activity == null) {
            // Should not happen, but can occur when the fragment gets content resolved after the user exited the install guide and returned to another activity.
            logError(TAG, OxygenUpdaterException("getActivity() returned null (displayInstallGuide)"))
            return
        }

        // Display a reminder to write everything down on the first page.
        if (isFirstPage) {
            installGuideHeader.visibility = VISIBLE
            installGuideTip.visibility = VISIBLE
        }

        if (installGuidePage?.deviceId == null || installGuidePage.updateMethodId == null) {
            displayDefaultInstallGuide(pageNumber)
        } else {
            displayCustomInstallGuide(pageNumber, installGuidePage)
        }

        // Hide the loading screen of the install guide page.
        installGuideLoadingScreen.visibility = GONE
        installGuideTitle.visibility = VISIBLE
        installGuideText.visibility = VISIBLE

        // Display the "Close" button on the last page.
        if (pageNumber == ApplicationData.NUMBER_OF_INSTALL_GUIDE_PAGES) {
            installGuideCloseButton.apply {
                setOnClickListener { activity?.finish() }
                visibility = VISIBLE
            }
        }
    }

    private fun displayDefaultInstallGuide(pageNumber: Int) {
        if (activity == null) {
            // Should never happen.
            logError(TAG, OxygenUpdaterException("getActivity() is null (displayDefaultInstallGuide)"))
            return
        }

        val titleResourceId = resources.getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TITLE, RESOURCE_ID_PACKAGE_STRING, activity!!.packageName)
        val contentsResourceId = resources.getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TEXT, RESOURCE_ID_PACKAGE_STRING, activity!!.packageName)

        installGuideTitle.setText(titleResourceId)
        installGuideText.setText(contentsResourceId)

        loadDefaultImage(installGuideImage, pageNumber)
    }

    private fun displayCustomInstallGuide(pageNumber: Int, installGuidePage: InstallGuidePage) {
        val appLocale = AppLocale.get()

        installGuideTitle.text = if (appLocale == NL) installGuidePage.dutchTitle else installGuidePage.englishTitle
        installGuideText.text = if (appLocale == NL) installGuidePage.dutchText else installGuidePage.englishText

        if (installGuidePage.useCustomImage) {
            // Fetch the custom image from the server.
            Glide.with(this)
                .load(completeImageUrl(installGuidePage.imageUrl, installGuidePage.fileExtension))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        installGuideImage.visibility = VISIBLE
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        installGuideImage.visibility = VISIBLE
                        return false
                    }
                })
                // Load a "no entry" sign to show that the image failed to load.
                .error(R.drawable.error_image)
                .into(installGuideImage)
        } else {
            loadDefaultImage(installGuideImage, pageNumber)
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

    private fun loadDefaultImage(view: ImageView, pageNumber: Int) {
        if (activity == null) {
            logError(TAG, OxygenUpdaterException("getActivity() is null (loadDefaultImage)"))
            return
        }

        val imageResourceId = resources.getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_IMAGE, RESOURCE_ID_PACKAGE_DRAWABLE, activity!!.packageName)
        val image = ResourcesCompat.getDrawable(resources, imageResourceId, null)

        view.apply {
            setImageDrawable(image)
            visibility = VISIBLE
        }
    }

    companion object {
        /**
         * The fragment argument representing the page number for this fragment.
         */
        private const val ARG_PAGE_NUMBER = "page_number"
        private const val ARG_IS_FIRST_PAGE = "is_first_page"

        private const val RESOURCE_ID_PREFIX = "install_guide_page_"
        private const val RESOURCE_ID_TITLE = "_title"
        private const val RESOURCE_ID_TEXT = "_text"
        private const val RESOURCE_ID_IMAGE = "_image"
        private const val RESOURCE_ID_PACKAGE_STRING = "string"
        private const val RESOURCE_ID_PACKAGE_DRAWABLE = "drawable"

        private const val IMAGE_VARIANT_LDPI = "ldpi"
        private const val IMAGE_VARIANT_MDPI = "mdpi"
        private const val IMAGE_VARIANT_TVDPI = "tvdpi"
        private const val IMAGE_VARIANT_HDPI = "hdpi"
        private const val IMAGE_VARIANT_XHDPI = "xhdpi"
        private const val IMAGE_VARIANT_XXHDPI = "xxhdpi"
        private const val IMAGE_VARIANT_XXXHDPI = "xxxhdpi"
        private const val IMAGE_VARIANT_DEFAULT = "default"

        private const val TAG = "InstallGuideFragment"

        /**
         * Returns a new instance of this fragment for the given page number.
         */
        fun newInstance(pageNumber: Int, isFirstPage: Boolean): InstallGuideFragment {
            val fragment = InstallGuideFragment()

            fragment.arguments = bundleOf(
                ARG_PAGE_NUMBER to pageNumber,
                ARG_IS_FIRST_PAGE to isFirstPage
            )

            return fragment
        }
    }
}
