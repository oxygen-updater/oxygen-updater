package com.oxygenupdater.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.oxygenupdater.activities.InstallActivity
import com.oxygenupdater.databinding.FragmentInstallGuideBinding
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.InstallGuidePage
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.viewmodels.InstallViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class InstallGuideFragment : Fragment() {

    private var pageNumber = 1
    private var isFirstPage = false

    private val installViewModel by sharedViewModel<InstallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        arguments?.let {
            pageNumber = it.getInt(ARG_PAGE_NUMBER, 1)
            isFirstPage = it.getBoolean(ARG_IS_FIRST_PAGE, false)
        }
    }

    /** Only valid between `onCreateView` and `onDestroyView` */
    private var binding: FragmentInstallGuideBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = FragmentInstallGuideBinding.inflate(inflater, container, false).run {
        binding = this
        root
    }

    override fun onDestroyView() = super.onDestroyView().also {
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (installViewModel.installGuideCache[pageNumber] == null) {
            installViewModel.fetchInstallGuidePage(
                deviceId,
                updateMethodId,
                pageNumber
            ) {
                if (!isAdded) {
                    return@fetchInstallGuidePage
                }

                val resources = resources
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
                    val deviceName = SystemVersionProperties.oxygenDeviceName
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
            binding?.installGuideTip?.isVisible = true

            installViewModel.markFirstInstallGuidePageLoaded()
        }

        // Hide the loading screen of the install guide page.
        binding?.shimmerFrameLayout?.isVisible = false
        binding?.installGuideText?.apply {
            isVisible = true
            text = installGuidePage.text
            // Make the links clickable
            movementMethod = LinkMovementMethod.getInstance()
        }

        // Display the "Close" button on the last page.
        if (pageNumber == InstallActivity.NUMBER_OF_INSTALL_GUIDE_PAGES) {
            binding?.installGuideCloseButton?.apply {
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
