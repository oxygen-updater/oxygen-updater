package com.arjanvlek.oxygenupdater.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.InstallActivity
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.viewmodels.InstallViewModel
import kotlinx.android.synthetic.main.fragment_install_guide.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class InstallGuideFragment : Fragment(R.layout.fragment_install_guide) {

    private var pageNumber = 1
    private var isFirstPage = false

    private val systemVersionProperties by inject<SystemVersionProperties>()
    private val settingsManager by inject<SettingsManager>()
    private val installViewModel by sharedViewModel<InstallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        arguments?.let {
            pageNumber = it.getInt(ARG_PAGE_NUMBER, 1)
            isFirstPage = it.getBoolean(ARG_IS_FIRST_PAGE, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (installViewModel.installGuideCache[pageNumber] == null) {
            installViewModel.fetchInstallGuidePage(
                deviceId,
                updateMethodId,
                pageNumber
            ) {
                // we need to clone the object, otherwise the correct object won't get reflected in
                val page = if (it == null || it.isDefaultPage) {
                    val titleResourceId = resources.getIdentifier(
                        InstallActivity.RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TITLE,
                        RESOURCE_ID_PACKAGE_STRING,
                        requireActivity().packageName
                    )
                    val contentsResourceId = resources.getIdentifier(
                        InstallActivity.RESOURCE_ID_PREFIX + pageNumber + RESOURCE_ID_TEXT,
                        RESOURCE_ID_PACKAGE_STRING,
                        requireActivity().packageName
                    )

                    // This is used to format the `install_guide_page_3_text` string
                    val deviceName = systemVersionProperties.oxygenDeviceName
                        // Ignore variant codes
                        .split("_")[0]
                        // Some old devices have spaces in the name.
                        // Since the filename obviously doesn't have spaces, remove them.
                        .replace(" ", "")

                    val title = getString(titleResourceId)
                    // Format "%1$sOxygen" with the correct device name
                    val text = getString(
                        contentsResourceId,
                        deviceName
                    )

                    it?.cloneWithDefaultTitleAndText(
                        title,
                        text
                    ) ?: InstallGuidePage(
                        id = pageNumber.toLong(),
                        deviceId = null,
                        updateMethodId = null,
                        pageNumber = pageNumber,
                        fileExtension = null,
                        imageUrl = null,
                        englishTitle = title,
                        dutchTitle = title,
                        englishText = text,
                        dutchText = text
                    )
                } else {
                    it.copy()
                }

                installViewModel.installGuideCache.put(pageNumber, page)
                displayInstallGuide(installViewModel.installGuideCache[pageNumber])
            }
        } else {
            displayInstallGuide(installViewModel.installGuideCache[pageNumber])
        }
    }

    private fun displayInstallGuide(installGuidePage: InstallGuidePage) {
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
            installGuideTip.isVisible = true

            installViewModel.markFirstInstallGuidePageLoaded()
        }

        // Hide the loading screen of the install guide page.
        shimmerFrameLayout.isVisible = false
        installGuideText.apply {
            isVisible = true
            text = installGuidePage.text
        }

        // Display the "Close" button on the last page.
        if (pageNumber == InstallActivity.NUMBER_OF_INSTALL_GUIDE_PAGES) {
            installGuideCloseButton.apply {
                setOnClickListener { activity?.onBackPressed() }
                isVisible = true
            }
        }
    }

    companion object {
        /**
         * The fragment argument representing the page number for this fragment.
         */
        private const val ARG_PAGE_NUMBER = "page_number"
        private const val ARG_IS_FIRST_PAGE = "is_first_page"

        private const val RESOURCE_ID_TITLE = "_title"
        private const val RESOURCE_ID_TEXT = "_text"
        private const val RESOURCE_ID_PACKAGE_STRING = "string"

        private const val TAG = "InstallGuideFragment"

        /**
         * Returns a new instance of this fragment for the given page number.
         */
        fun newInstance(pageNumber: Int, isFirstPage: Boolean) = InstallGuideFragment().apply {
            arguments = bundleOf(
                ARG_PAGE_NUMBER to pageNumber,
                ARG_IS_FIRST_PAGE to isFirstPage
            )
        }
    }
}
