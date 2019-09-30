package com.arjanvlek.oxygenupdater.installation.manual

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NUMBER_OF_INSTALL_GUIDE_PAGES
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.installation.InstallActivity
import com.arjanvlek.oxygenupdater.internal.FunctionalAsyncTask
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.Worker
import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD_ID
import java8.util.function.Consumer
import java8.util.function.Function
import java.net.MalformedURLException
import java.net.URL

class InstallGuideFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val installGuideView = inflater.inflate(R.layout.fragment_install_guide, container, false)

        val pageNumber: Int
        val isFirstPage: Boolean

        if (arguments != null) {
            pageNumber = arguments!!.getInt(ARG_PAGE_NUMBER, 1)
            isFirstPage = arguments!!.getBoolean(ARG_IS_FIRST_PAGE, false)
        } else {
            pageNumber = 1
            isFirstPage = false
        }

        val settingsManager = SettingsManager(context)
        val deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L)

        val cache: SparseArray<InstallGuidePage> = if (activity != null && activity is InstallActivity) {
            (activity as InstallActivity).installGuideCache
        } else {
            logWarning(TAG, OxygenUpdaterException("getActivity() returned null or was not an instance of InstallActivity (onCreateView, getInstallGuideCache)"))
            SparseArray()
        }

        if (cache.get(pageNumber) == null) {
            if (activity != null && activity!!.application != null && activity!!.application is ApplicationData) {
                val connector = (activity!!.application as ApplicationData).getServerConnector()
                connector.getInstallGuidePage(deviceId, updateMethodId, pageNumber, Consumer { page ->
                    cache.put(pageNumber, page)
                    displayInstallGuide(installGuideView, page, pageNumber, isFirstPage)
                })
            }

        } else {
            displayInstallGuide(installGuideView, cache.get(pageNumber), pageNumber, isFirstPage)
        }

        return installGuideView
    }

    private fun displayInstallGuide(installGuideView: View, installGuidePage: InstallGuidePage?, pageNumber: Int, isFirstPage: Boolean) {
        if (!isAdded) {
            // Happens when a page is scrolled too far outside the screen (2 or more rows) and content then gets resolved from the server.
            logError(TAG, OxygenUpdaterException("isAdded() returned false (displayInstallGuide)"))
            return
        }

        if (activity == null) {
            // Should not happen, but can occur when the fragment gets content resolved after the user exited the install guide and returned to another activity.
            logError(TAG, OxygenUpdaterException("getActivity() returned null (displayInstallGuide)"))
            return
        }

        // Display a reminder to write everything down on the first page.
        if (isFirstPage) {
            installGuideView.findViewById<View>(R.id.installGuideHeader).visibility = View.VISIBLE
            installGuideView.findViewById<View>(R.id.installGuideTip).visibility = View.VISIBLE
        }

        if (installGuidePage?.deviceId == null || installGuidePage.updateMethodId == null) {
            displayDefaultInstallGuide(installGuideView, pageNumber)
        } else {
            displayCustomInstallGuide(installGuideView, pageNumber, installGuidePage)
        }

        // Hide the loading screen of the install guide page.
        installGuideView.findViewById<View>(R.id.installGuideLoadingScreen).visibility = View.GONE

        val titleTextView = installGuideView.findViewById<TextView>(R.id.installGuideTitle)
        val contentsTextView = installGuideView.findViewById<TextView>(R.id.installGuideText)
        titleTextView.visibility = View.VISIBLE
        contentsTextView.visibility = View.VISIBLE

        // Display the "Close" button on the last page.
        if (pageNumber == NUMBER_OF_INSTALL_GUIDE_PAGES) {
            val closeButton = installGuideView.findViewById<Button>(R.id.installGuideCloseButton)
            closeButton.setOnClickListener { activity!!.finish() }
            closeButton.visibility = View.VISIBLE
        }

    }

    private fun displayDefaultInstallGuide(installGuideView: View, pageNumber: Int) {
        if (activity == null) {
            // Should never happen.
            logError(TAG, OxygenUpdaterException("getActivity() is null (displayDefaultInstallGuide)"))
            return
        }

        val titleTextView = installGuideView.findViewById<TextView>(R.id.installGuideTitle)
        val contentsTextView = installGuideView.findViewById<TextView>(R.id.installGuideText)

        val titleResourceId = resources.getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TITLE, RESOURCE_ID_PACKAGE_STRING, activity!!.packageName)
        val contentsResourceId = resources.getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TEXT, RESOURCE_ID_PACKAGE_STRING, activity!!.packageName)

        titleTextView.text = getString(titleResourceId)
        contentsTextView.text = getString(contentsResourceId)

        loadDefaultImage(installGuideView.findViewById(R.id.installGuideImage), pageNumber)
    }

    private fun displayCustomInstallGuide(installGuideView: View, pageNumber: Int, installGuidePage: InstallGuidePage) {
        val titleTextView = installGuideView.findViewById<TextView>(R.id.installGuideTitle)
        val contentsTextView = installGuideView.findViewById<TextView>(R.id.installGuideText)

        val appLocale = Locale.locale

        if (appLocale == Locale.NL) {
            titleTextView.text = installGuidePage.dutchTitle
            contentsTextView.text = installGuidePage.dutchText
        } else {
            titleTextView.text = installGuidePage.englishTitle
            contentsTextView.text = installGuidePage.englishText
        }

        val imageView = installGuideView.findViewById<ImageView>(R.id.installGuideImage)

        if (installGuidePage.useCustomImage!!) {
            // Fetch the custom image from the server.
            FunctionalAsyncTask<Void, Void, Bitmap>(object : Worker {
                override fun start() { }
            }, Function {
                var image: Bitmap?

                try {
                    val cache: SparseArray<Bitmap> = if (activity != null) {
                        (activity as InstallActivity).installGuideImageCache
                    } else {
                        SparseArray()
                    }

                    // If the cache contains the image, return the image from the cache.
                    if (cache.get(installGuidePage.pageNumber!!) != null) {
                        image = cache.get(installGuidePage.pageNumber!!)
                    } else {
                        // Otherwise, fetch the image from the server.
                        if (isAdded) {
                            image = doGetCustomImage(completeImageUrl(installGuidePage.imageUrl, installGuidePage.fileExtension))
                            cache.put(installGuidePage.pageNumber!!, image)
                        } else {
                            image = null
                        }
                    }
                } catch (e: MalformedURLException) {
                    image = null
                    logError(TAG, NetworkException(String.format("Error loading custom image: Invalid image URL <%s>", installGuidePage.imageUrl)))
                }

                image
            }, Consumer { image ->
                // If there is no image, load a "no entry" sign to show that the image failed to load.
                if (image == null && isAdded) {
                    loadErrorImage(imageView)
                } else {
                    loadCustomImage(imageView, image)
                }
            }).execute()
        } else {
            loadDefaultImage(imageView, pageNumber)
        }
    }

    @Throws(MalformedURLException::class)
    private fun completeImageUrl(imageUrl: String?, fileExtension: String?): URL {
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

        return URL(imageUrl + "_" + imageVariant + "." + fileExtension)
    }

    private fun loadCustomImage(view: ImageView, image: Bitmap?) {
        view.setImageBitmap(image)
        view.visibility = View.VISIBLE
    }

    private fun doGetCustomImage(imageUrl: URL, retryCount: Int = 0): Bitmap? {
        return try {
            val `in` = imageUrl.openStream()
            BitmapFactory.decodeStream(`in`)
        } catch (e: Exception) {
            return if (retryCount < 5) {
                doGetCustomImage(imageUrl, retryCount + 1)
            } else {
                logError(TAG, "Error loading custom install guide image", e)
                null
            }
        }

    }

    private fun loadDefaultImage(view: ImageView, pageNumber: Int) {
        if (activity == null) {
            logError(TAG, OxygenUpdaterException("getActivity() is null (loadDefaultImage)"))
            return
        }

        val imageResourceId = resources.getIdentifier(RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_IMAGE, RESOURCE_ID_PACKAGE_DRAWABLE, activity!!.packageName)
        val image = ResourcesCompat.getDrawable(resources, imageResourceId, null)
        view.setImageDrawable(image)
        view.visibility = View.VISIBLE
    }

    private fun loadErrorImage(view: ImageView) {
        val errorImage = ResourcesCompat.getDrawable(resources, R.drawable.error_image, null)
        view.setImageDrawable(errorImage)
        view.visibility = View.VISIBLE
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
            val args = Bundle()
            args.putInt(ARG_PAGE_NUMBER, pageNumber)
            args.putBoolean(ARG_IS_FIRST_PAGE, isFirstPage)
            fragment.arguments = args
            return fragment
        }
    }
}
