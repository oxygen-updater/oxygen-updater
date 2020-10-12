package com.oxygenupdater.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.format.Formatter
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.work.WorkInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.ActivityLauncher
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.OxygenUpdater.Companion.NO_OXYGEN_OS
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.dialogs.Dialogs
import com.oxygenupdater.dialogs.Dialogs.showDownloadError
import com.oxygenupdater.dialogs.Dialogs.showUpdateAlreadyDownloadedMessage
import com.oxygenupdater.dialogs.ServerMessagesDialog
import com.oxygenupdater.enums.DownloadFailure
import com.oxygenupdater.enums.DownloadStatus
import com.oxygenupdater.enums.DownloadStatus.DOWNLOADING
import com.oxygenupdater.enums.DownloadStatus.DOWNLOAD_COMPLETED
import com.oxygenupdater.enums.DownloadStatus.DOWNLOAD_FAILED
import com.oxygenupdater.enums.DownloadStatus.DOWNLOAD_PAUSED
import com.oxygenupdater.enums.DownloadStatus.DOWNLOAD_QUEUED
import com.oxygenupdater.enums.DownloadStatus.NOT_DOWNLOADING
import com.oxygenupdater.enums.DownloadStatus.VERIFICATION_COMPLETED
import com.oxygenupdater.enums.DownloadStatus.VERIFICATION_FAILED
import com.oxygenupdater.enums.DownloadStatus.VERIFYING
import com.oxygenupdater.extensions.setImageResourceWithTint
import com.oxygenupdater.internal.DeviceInformationData
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.Banner
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.UpdateDataVersionFormatter.canVersionInfoBeFormatted
import com.oxygenupdater.utils.UpdateDataVersionFormatter.getFormattedOxygenOsVersion
import com.oxygenupdater.utils.UpdateDataVersionFormatter.getFormattedVersionNumber
import com.oxygenupdater.utils.UpdateDescriptionParser
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.Utils.SERVER_TIME_ZONE
import com.oxygenupdater.viewmodels.MainViewModel
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_BYTES_DONE
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_ETA
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_FAILURE_TYPE
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_PROGRESS
import com.oxygenupdater.workers.WORK_DATA_DOWNLOAD_TOTAL_BYTES
import kotlinx.android.synthetic.main.fragment_update_information.*
import kotlinx.android.synthetic.main.layout_device_information_software.*
import kotlinx.android.synthetic.main.layout_error.*
import kotlinx.android.synthetic.main.layout_system_is_up_to_date.*
import kotlinx.android.synthetic.main.layout_update_information.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.threeten.bp.LocalDateTime

class UpdateInformationFragment : AbstractFragment(R.layout.fragment_update_information) {

    private var updateData: UpdateData? = null
    private var isLoadedOnce = false

    private val systemVersionProperties by inject<SystemVersionProperties>()
    private val crashlytics by inject<FirebaseCrashlytics>()
    private val mainViewModel by sharedViewModel<MainViewModel>()

    /**
     * Allows an already downloaded update file to be deleted to save storage space.
     */
    private val alreadyDownloadedOnClickListener = View.OnClickListener {
        showUpdateAlreadyDownloadedMessage(updateData, activity) {
            if (updateData != null) {
                val mainActivity = activity as MainActivity? ?: return@showUpdateAlreadyDownloadedMessage

                if (mainActivity.hasDownloadPermissions()) {
                    val deleted = mainViewModel.deleteDownloadedFile(requireContext(), updateData)

                    if (deleted) {
                        mainViewModel.updateDownloadStatus(NOT_DOWNLOADING)
                    }
                } else {
                    mainActivity.requestDownloadPermissions { granted ->
                        if (granted) {
                            val deleted = mainViewModel.deleteDownloadedFile(requireContext(), updateData)

                            if (deleted) {
                                mainViewModel.updateDownloadStatus(NOT_DOWNLOADING)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Download button click listener. Performs these actions when the button is clicked.
     */
    private val downloadNotStartedOnClickListener = View.OnClickListener {
        if (isAdded && updateInformationLayout != null) {
            val mainActivity = activity as MainActivity? ?: return@OnClickListener

            if (mainActivity.hasDownloadPermissions()) {
                mainViewModel.enqueueDownloadWork(mainActivity, updateData!!)

                downloadProgressBar.isIndeterminate = true
                downloadDetailsTextView.setText(R.string.download_pending)

                // Pause is possible on first progress update
                downloadLayout.setOnClickListener { }
            } else {
                mainActivity.requestDownloadPermissions { granted ->
                    if (granted) {
                        mainViewModel.enqueueDownloadWork(mainActivity, updateData!!)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (settingsManager.checkIfSetupScreenHasBeenCompleted()) {
            val analytics by inject<FirebaseAnalytics>()

            swipeRefreshLayout.apply {
                setOnRefreshListener { load() }
                setColorSchemeResources(R.color.colorPrimary)
            }

            analytics.setUserProperty(
                "device_name",
                systemVersionProperties.oxygenDeviceName
            )

            setupServerResponseObservers()
            setupWorkObservers()

            load()
        }
    }

    override fun onResume() = super.onResume().also {
        if (isLoadedOnce) {
            if (mainViewModel.checkDownloadCompletionByFile(updateData)) {
                mainViewModel.updateDownloadStatus(DOWNLOAD_COMPLETED)
            } else {
                mainViewModel.updateDownloadStatus(NOT_DOWNLOADING)
            }
        }
    }

    override fun onDestroy() = super.onDestroy().also {
        mainViewModel.maybePruneWork()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mainViewModel.enqueueDownloadWork(requireActivity(), updateData!!)
            } else if (requestCode == Activity.RESULT_CANCELED) {
                showDownloadError(R.string.download_error_storage)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupServerResponseObservers() {
        mainViewModel.serverMessages.observe(viewLifecycleOwner) {
            displayServerMessageBars(it)
        }

        mainViewModel.serverStatus.observe(viewLifecycleOwner) { serverStatus ->
            // display server status banner if required
            val status = serverStatus.status
            if (status!!.isUserRecoverableError) {
                serverStatusTextView.apply {
                    isVisible = true
                    text = serverStatus.getBannerText(requireContext())
                    setBackgroundColor(serverStatus.getColor(requireContext()))
                    setCompoundDrawablesRelativeWithIntrinsicBounds(serverStatus.getDrawableRes(requireContext()), 0, 0, 0)
                }
            } else {
                serverStatusTextView.isVisible = false
            }

            mainViewModel.fetchServerMessages(serverStatus) { error ->
                if (!isAdded) {
                    return@fetchServerMessages
                }

                when (error) {
                    OxygenUpdater.SERVER_MAINTENANCE_ERROR -> Dialogs.showServerMaintenanceError(activity)
                    OxygenUpdater.APP_OUTDATED_ERROR -> Dialogs.showAppOutdatedError(activity)
                }
            }
        }

        mainViewModel.updateData.observe(viewLifecycleOwner) {
            if (it == null) {
                inflateAndShowErrorState()
            } else {
                hideErrorStateIfInflated()

                this.updateData = it

                // If the activity is started with a download error (when clicked on a "download failed" notification), show it to the user.
                if (!isLoadedOnce && activity?.intent?.getBooleanExtra(KEY_HAS_DOWNLOAD_ERROR, false) == true) {
                    val intent = requireActivity().intent
                    showDownloadError(
                        activity,
                        intent.getBooleanExtra(KEY_DOWNLOAD_ERROR_RESUMABLE, false),
                        intent.getStringExtra(KEY_DOWNLOAD_ERROR_TITLE),
                        intent.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE)
                    ) { isResumable ->
                        if (!isResumable) {
                            // Delete downloaded file, so the user can restart the download
                            mainViewModel.deleteDownloadedFile(requireContext(), updateData)
                        }

                        mainViewModel.enqueueDownloadWork(requireActivity(), updateData!!)
                    }
                }

                displayUpdateInformation(updateData!!)
                isLoadedOnce = true
            }
        }
    }

    private fun setupWorkObservers() {
        mainViewModel.downloadWorkInfo.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                mainViewModel.updateDownloadStatus(it.first())
            }
        }

        mainViewModel.verificationWorkInfo.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                mainViewModel.updateDownloadStatus(it.first(), true)
            }
        }

        mainViewModel.downloadStatus.observe(viewLifecycleOwner) {
            logDebug(TAG, "Download status updated: ${it.first}")

            initDownloadLayout(it)
        }
    }

    /**
     * Fetches all server data. This includes update information, server messages and server status
     * checks
     */
    private fun load() {
        // show the loading shimmer
        shimmerFrameLayout.isVisible = true

        crashlytics.setUserId(
            "Device: "
                    + settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, "<UNKNOWN>")
                    + ", Update Method: "
                    + settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
        )

        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        mainViewModel.fetchServerStatus()

        mainViewModel.fetchUpdateData(
            deviceId,
            updateMethodId,
            systemVersionProperties.oxygenOSOTAVersion
        )
    }

    private fun inflateAndShowErrorState() {
        // Hide the loading shimmer since an error state can only be enabled after a load completes
        shimmerFrameLayout.isVisible = false
        // Hide the refreshing icon if it is present
        swipeRefreshLayout.isRefreshing = false

        // Show error layout
        errorLayoutStub?.inflate()
        errorLayout.isVisible = true
        // Hide "System update available" view
        updateInformationLayout?.visibility = GONE
        // Hide "System is up to date" view
        systemIsUpToDateLayout?.visibility = GONE

        errorTitle.text = getString(R.string.update_information_error_title)
        // Make the links clickable
        errorText.movementMethod = LinkMovementMethod.getInstance()

        errorActionButton.setOnClickListener { load() }
    }

    private fun hideErrorStateIfInflated() {
        // Stub is null only after it has been inflated, and
        // we need to hide the error state only if it has been inflated
        if (errorLayoutStub == null) {
            errorLayout.isVisible = false
            errorActionButton.setOnClickListener { }
        }
    }

    /**
     * Makes [serverBannerTextView] visible.
     *
     * If there are banners that have non-empty text, clicking [serverBannerTextView] will show a [ServerMessagesDialog]
     */
    private fun displayServerMessageBars(banners: List<Banner>) {
        // select only the banners that have a non-empty text (`ServerStatus.kt` has empty text in some cases)
        val bannerList = banners.filter { !it.getBannerText(requireContext()).isNullOrBlank() }

        if (!isAdded || bannerList.isEmpty()) {
            return
        }

        val dialog = ServerMessagesDialog(requireContext(), bannerList)

        serverBannerTextView.apply {
            isVisible = true

            // show dialog
            setOnClickListener { dialog.show() }
        }
    }

    /**
     * Displays the update information from a [UpdateData] with update information.
     *
     * @param updateData Update information to display
     */
    private fun displayUpdateInformation(updateData: UpdateData) {
        if (!isAdded) {
            return
        }

        val online = Utils.checkNetworkConnection()

        // hide the loading shimmer
        shimmerFrameLayout.isVisible = false
        // Hide the refreshing icon if it is present
        swipeRefreshLayout.isRefreshing = false

        if (updateData.id == null
            || updateData.isSystemIsUpToDateCheck(settingsManager)
            || !updateData.isUpdateInformationAvailable
        ) {
            propagateTitleAndSubtitleChanges(
                getString(
                    if (updateData.isUpdateInformationAvailable) {
                        R.string.update_information_system_is_up_to_date
                    } else {
                        R.string.update_information_no_update_data_available
                    }
                )
            )

            displayUpdateInformationWhenUpToDate(updateData, online)
        } else {
            propagateTitleAndSubtitleChanges(
                getString(
                    if (updateData.systemIsUpToDate) {
                        R.string.update_information_header_advanced_mode_hint
                    } else {
                        R.string.notification_version_title
                    }
                )
            )

            displayUpdateInformationWhenNotUpToDate(updateData)
        }

        if (online) {
            // Save update data for offline viewing
            settingsManager.apply {
                savePreference(SettingsManager.PROPERTY_OFFLINE_ID, updateData.id)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME, updateData.versionNumber)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, updateData.downloadSize)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION, updateData.description)
                savePreference(SettingsManager.PROPERTY_OFFLINE_FILE_NAME, updateData.filename)
                savePreference(SettingsManager.PROPERTY_OFFLINE_DOWNLOAD_URL, updateData.downloadUrl)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, updateData.isUpdateInformationAvailable)
                savePreference(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now(SERVER_TIME_ZONE).toString())
                savePreference(SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE, updateData.systemIsUpToDate)
            }
        }
    }

    private fun displayUpdateInformationWhenNotUpToDate(
        updateData: UpdateData
    ) {
        // Show "System update available" view.
        updateInformationLayoutStub?.inflate()
        updateInformationLayout?.visibility = VISIBLE
        // Hide "System is up to date" view
        systemIsUpToDateLayout?.visibility = GONE

        oxygenOsVersionTextView.text = if (updateData.versionNumber != null && updateData.versionNumber != "null") {
            if (canVersionInfoBeFormatted(updateData)) {
                getFormattedVersionNumber(updateData)
            } else {
                updateData.versionNumber
            }
        } else {
            getString(
                R.string.update_information_unknown_update_name,
                settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, getString(R.string.device_information_unknown))
            )
        }

        if (updateData.systemIsUpToDate) {
            val updateMethod = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "'<UNKNOWN>'")

            // Format footer based on system version installed.
            footerTextView.text = getString(R.string.update_information_header_advanced_mode_helper, updateMethod)

            footerTextView.isVisible = true
            footerDivider.isVisible = true
        } else {
            // display badge to indicate a new system update is available
            (activity as MainActivity?)?.updateTabBadge(R.id.page_update)

            footerTextView.isVisible = false
            footerDivider.isVisible = false
        }

        // Display download size.
        downloadSizeTextView.text = Formatter.formatFileSize(
            context,
            updateData.downloadSizeForFormatter
        )

        // Display update description.
        changelogTextView.apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = getUpdateChangelog(updateData.description)
        }

        // Display update file name.
        fileNameTextView.text = getString(R.string.update_information_file_name, updateData.filename)
        md5TextView.text = getString(R.string.update_information_md5, updateData.mD5Sum)

        mainViewModel.setupDownloadWorkRequest(updateData)

        if (mainViewModel.checkDownloadCompletionByFile(updateData)) {
            mainViewModel.updateDownloadStatus(DOWNLOAD_COMPLETED)
        } else {
            mainViewModel.updateDownloadStatus(NOT_DOWNLOADING)
        }
    }

    private fun displayUpdateInformationWhenUpToDate(
        updateData: UpdateData,
        online: Boolean
    ) {
        // Show "System is up to date" view.
        systemIsUpToDateLayoutStub?.inflate()
        systemIsUpToDateLayout?.visibility = VISIBLE
        // Hide "System update available" view
        updateInformationLayout?.visibility = GONE

        // https://stackoverflow.com/a/60542345
        systemIsUpToDateLayoutChild?.layoutTransition?.setAnimateParentHierarchy(false)

        val isDifferentVersion = updateData.otaVersionNumber != systemVersionProperties.oxygenOSOTAVersion

        advancedModeTipTextView.run {
            isVisible = isDifferentVersion
            advancedModeTipDivider.isVisible = isDifferentVersion

            text = getString(
                R.string.update_information_banner_advanced_mode_tip,
                settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "'<UNKNOWN>'")
            )
        }

        // Save last time checked if online.
        if (online) {
            settingsManager.savePreference(
                SettingsManager.PROPERTY_UPDATE_CHECKED_DATE,
                LocalDateTime.now(SERVER_TIME_ZONE).toString()
            )
        }

        // Show last time checked.
        updateLastCheckedField.text = getString(
            R.string.update_information_last_checked_on,
            Utils.formatDateTime(requireContext(), settingsManager.getPreference<String?>(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, null))
        )

        displaySoftwareInfo()
        setupChangelogViews(isDifferentVersion)
    }

    private fun displaySoftwareInfo() {
        if (!isAdded) {
            logDebug(TAG, "Fragment not added. Can not display software information!")
            return
        }

        softwareHeader.isVisible = false

        // Android version
        osVersionField.text = DeviceInformationData.osVersion

        // OxygenOS version (if available)
        oxygenOsVersionField.run {
            val oxygenOSVersion = getFormattedOxygenOsVersion(systemVersionProperties.oxygenOSVersion)

            if (oxygenOSVersion != NO_OXYGEN_OS) {
                text = oxygenOSVersion
            } else {
                oxygenOsVersionLabel.isVisible = false
                isVisible = false
            }
        }

        // OxygenOS OTA version (if available)
        otaVersionField.run {
            val oxygenOSOTAVersion = systemVersionProperties.oxygenOSOTAVersion

            if (oxygenOSOTAVersion != NO_OXYGEN_OS) {
                text = oxygenOSOTAVersion
            } else {
                otaVersionLabel.isVisible = false
                isVisible = false
            }
        }

        // Incremental OS version
        incrementalOsVersionField.text = DeviceInformationData.incrementalOsVersion

        // Security Patch Date (if available)
        securityPatchField.run {
            val securityPatchDate = systemVersionProperties.securityPatchDate

            if (securityPatchDate != NO_OXYGEN_OS) {
                text = securityPatchDate
            } else {
                securityPatchLabel.isVisible = false
                isVisible = false
            }
        }
    }

    private fun setupChangelogViews(isDifferentVersion: Boolean) {
        changelogField.text = getUpdateChangelog(updateData?.description)
        differentVersionChangelogNotice.text = getString(
            R.string.update_information_different_version_changelog_notice,
            settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "'<UNKNOWN>'")
        )

        changelogLabel.run {
            // Set text to "No update information is available" if needed
            text = getString(
                if (updateData?.isUpdateInformationAvailable == false) {
                    R.string.update_information_no_update_data_available
                } else {
                    R.string.update_information_view_update_information
                }
            )

            setOnClickListener {
                val visible = !changelogField.isVisible

                // Show the changelog
                changelogField.isVisible = visible
                // Display a notice above the changelog if the user's currently installed
                // version doesn't match the version this changelog is meant for
                differentVersionChangelogNotice.isVisible = visible && isDifferentVersion

                // Toggle expand/collapse icons
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    if (visible) {
                        R.drawable.expand
                    } else {
                        R.drawable.collapse
                    }, 0, 0, 0
                )
            }
        }
    }

    private fun propagateTitleAndSubtitleChanges(subtitle: CharSequence?) {
        mainViewModel.saveSubtitleForPage(
            R.id.page_update,
            subtitle
        )

        // Since this is the first/default page, the page change callback isn't fired
        // So we need to manually update toolbar text
        (activity as MainActivity?)?.updateToolbarForPage(R.id.page_update)
    }

    private fun getUpdateChangelog(description: String?) = if (!description.isNullOrBlank() && description != "null") {
        UpdateDescriptionParser.parse(context, description).trim()
    } else {
        getString(R.string.update_information_description_not_available)
    }

    private fun showDownloadLink() {
        if (updateData != null) {
            downloadLinkTextView.apply {
                isVisible = true
                movementMethod = LinkMovementMethod.getInstance()
                text = SpannableString(
                    getString(R.string.update_information_download_link, updateData!!.downloadUrl)
                ).apply {
                    setSpan(URLSpan(updateData!!.downloadUrl), indexOf("\n") + 1, length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun showDownloadErrorForUnsuccessfulResponse() = showDownloadError(
        requireActivity(),
        false,
        getString(R.string.download_error),
        HtmlCompat.fromHtml(
            getString(R.string.download_error_unsuccessful_response),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    )

    private fun showDownloadError(
        @StringRes message: Int,
        isResumable: Boolean = false,
        callback: KotlinCallback<Boolean>? = null
    ) = showDownloadError(
        requireActivity(),
        isResumable,
        R.string.download_error,
        message,
        callback
    )

    private fun initDownloadLayout(pair: Pair<DownloadStatus, WorkInfo?>) {
        // If the stub is null, that means it's been inflated, which means views used in this function aren't null
        if (updateInformationLayoutStub == null) {
            val workInfo = pair.second

            when (pair.first) {
                NOT_DOWNLOADING -> {
                    initDownloadActionButton(true)

                    downloadUpdateTextView.setText(R.string.download)
                    downloadSizeTextView.text = Formatter.formatFileSize(
                        context,
                        updateData?.downloadSizeForFormatter ?: 0
                    )

                    downloadProgressBar.isVisible = false
                    downloadActionButton.isVisible = false
                    downloadDetailsTextView.isVisible = false

                    downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPrimary)

                    downloadLayout.apply {
                        val shouldEnable = updateData?.downloadUrl?.contains("http") == true
                        isEnabled = shouldEnable
                        isClickable = shouldEnable

                        if (shouldEnable) {
                            setOnClickListener(downloadNotStartedOnClickListener)
                        }
                    }

                    if (updateData == null) {
                        load()
                    }
                }
                DOWNLOAD_QUEUED,
                DOWNLOADING -> {
                    initDownloadActionButton(true)

                    downloadUpdateTextView.setText(R.string.downloading)

                    val workProgress = workInfo?.progress
                    val bytesDone = workProgress?.getLong(WORK_DATA_DOWNLOAD_BYTES_DONE, -1L) ?: -1L
                    val totalBytes = workProgress?.getLong(WORK_DATA_DOWNLOAD_TOTAL_BYTES, -1L) ?: -1L
                    val currentProgress = workProgress?.getInt(WORK_DATA_DOWNLOAD_PROGRESS, 0) ?: 0
                    val downloadEta = workProgress?.getString(WORK_DATA_DOWNLOAD_ETA)

                    downloadDetailsTextView.apply {
                        isVisible = true
                        if (downloadEta != null) {
                            text = downloadEta
                        }
                    }

                    downloadProgressBar.apply {
                        isVisible = true

                        if (bytesDone != -1L && totalBytes != -1L) {
                            val previousProgress = progress

                            // only update view in increments of progress to avoid multiple unnecessary view updates or logs
                            if (currentProgress != previousProgress) {
                                isIndeterminate = false
                                progress = currentProgress

                                val bytesDoneStr = Formatter.formatFileSize(context, bytesDone)
                                val totalBytesStr = Formatter.formatFileSize(context, totalBytes)

                                @SuppressLint("SetTextI18n")
                                downloadSizeTextView.text = "$bytesDoneStr / $totalBytesStr ($currentProgress%)"
                            }
                        } else {
                            isIndeterminate = true
                        }
                    }

                    downloadIcon.apply {
                        if (drawable !is AnimationDrawable || !(drawable as AnimationDrawable).isRunning) {
                            setImageResourceWithTint(android.R.drawable.stat_sys_download, R.color.colorPositive)
                            (drawable as AnimationDrawable).start()
                        }
                    }

                    downloadLayout.apply {
                        isEnabled = true
                        isClickable = false
                        setOnClickListener {
                            downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPositive)

                            mainViewModel.pauseDownloadWork()

                            // Prevents sending duplicate Intents
                            setOnClickListener { }
                        }
                    }
                }
                DOWNLOAD_PAUSED -> {
                    initDownloadActionButton(true)

                    downloadUpdateTextView.setText(R.string.paused)
                    downloadDetailsTextView.setText(R.string.download_progress_text_paused)

                    // Hide progress bar if it's in an indeterminate state
                    downloadProgressBar.isVisible = !downloadProgressBar.isIndeterminate
                    downloadDetailsTextView.isVisible = true

                    downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPositive)

                    downloadLayout.apply {
                        isEnabled = true
                        isClickable = false
                        setOnClickListener {
                            downloadIcon.setImageResourceWithTint(android.R.drawable.stat_sys_download, R.color.colorPositive)
                            (downloadIcon.drawable as AnimationDrawable).start()

                            mainViewModel.enqueueDownloadWork(requireActivity(), updateData!!)

                            // No resuming twice allowed
                            setOnClickListener { }
                        }
                    }
                }
                DOWNLOAD_COMPLETED -> {
                    initDownloadActionButton(false)

                    downloadUpdateTextView.setText(R.string.downloaded)
                    downloadSizeTextView.text = Formatter.formatFileSize(
                        context,
                        updateData?.downloadSizeForFormatter ?: 0
                    )

                    downloadProgressBar.isVisible = false
                    downloadDetailsTextView.isVisible = false

                    downloadIcon.setImageResourceWithTint(R.drawable.done_outline, R.color.colorPositive)

                    // `workInfo` is null only while delivering the initial status update
                    // This check prevents starting verification work as soon as the app starts
                    if (workInfo != null) {
                        downloadLayout.apply {
                            isEnabled = false
                            isClickable = false
                            setOnClickListener { }
                        }
                    } else {
                        downloadLayout.apply {
                            isEnabled = true
                            isClickable = true
                            setOnClickListener(alreadyDownloadedOnClickListener)
                        }
                    }
                }
                DOWNLOAD_FAILED -> {
                    val outputData = workInfo?.outputData

                    val failureType = outputData?.getString(WORK_DATA_DOWNLOAD_FAILURE_TYPE)

                    if (failureType != null) {
                        mainViewModel.updateDownloadStatus(NOT_DOWNLOADING)

                        when (DownloadFailure[failureType]) {
                            DownloadFailure.SERVER_ERROR -> {
                                showDownloadLink()
                                showDownloadError(
                                    R.string.download_error_server,
                                    true
                                ) {
                                    mainViewModel.enqueueDownloadWork(requireActivity(), updateData!!)
                                }
                            }
                            DownloadFailure.CONNECTION_ERROR -> {
                                showDownloadLink()
                                showDownloadError(
                                    R.string.download_error_internal,
                                    true
                                ) {
                                    mainViewModel.enqueueDownloadWork(requireActivity(), updateData!!)
                                }
                            }
                            DownloadFailure.UNSUCCESSFUL_RESPONSE -> {
                                showDownloadLink()
                                showDownloadErrorForUnsuccessfulResponse()
                                mainViewModel.logDownloadError(outputData)
                            }
                            DownloadFailure.NULL_UPDATE_DATA_OR_DOWNLOAD_URL,
                            DownloadFailure.DOWNLOAD_URL_INVALID_SCHEME,
                            DownloadFailure.UNKNOWN -> {
                                showDownloadLink()
                                showDownloadError(R.string.download_error_internal)
                            }
                            DownloadFailure.COULD_NOT_MOVE_TEMP_FILE -> showDownloadError(
                                requireActivity(),
                                false,
                                getString(R.string.download_error),
                                getString(
                                    R.string.download_error_could_not_move_temp_file,
                                    "Android/data/${requireContext().packageName}/files"
                                )
                            )
                        }
                    }
                }
                VERIFYING -> {
                    downloadUpdateTextView.setText(R.string.download_verifying)
                    downloadDetailsTextView.setText(R.string.download_progress_text_verifying)

                    downloadProgressBar.isVisible = true
                    downloadActionButton.isVisible = false
                    downloadDetailsTextView.isVisible = true

                    downloadProgressBar.isIndeterminate = true
                    downloadLayout.apply {
                        isEnabled = true
                        isClickable = false
                    }
                }
                VERIFICATION_COMPLETED -> {
                    initDownloadActionButton(false)

                    downloadUpdateTextView.setText(R.string.downloaded)
                    downloadSizeTextView.text = Formatter.formatFileSize(
                        context,
                        updateData?.downloadSizeForFormatter ?: 0
                    )

                    downloadProgressBar.isVisible = false
                    downloadDetailsTextView.isVisible = false

                    downloadIcon.setImageResourceWithTint(R.drawable.done_outline, R.color.colorPositive)

                    downloadLayout.apply {
                        isEnabled = true
                        isClickable = true
                        setOnClickListener(alreadyDownloadedOnClickListener)
                    }

                    // Since file has been verified, we can prune the work and launch [InstallActivity]
                    mainViewModel.maybePruneWork()
                    Toast.makeText(context, getString(R.string.download_complete), LENGTH_LONG).show()
                    ActivityLauncher(requireActivity()).UpdateInstallation(true, updateData)
                }
                VERIFICATION_FAILED -> {
                    initDownloadActionButton(false)

                    downloadUpdateTextView.setText(R.string.download_verifying_error)

                    downloadProgressBar.isVisible = false
                    downloadDetailsTextView.isVisible = true

                    downloadDetailsTextView.setText(R.string.download_notification_error_corrupt)

                    downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPositive)

                    downloadLayout.apply {
                        isEnabled = true
                        isClickable = true

                        setOnClickListener {
                            downloadIcon.setImageResourceWithTint(android.R.drawable.stat_sys_download, R.color.colorPositive)
                            (downloadIcon.drawable as AnimationDrawable).start()

                            mainViewModel.enqueueDownloadWork(requireActivity(), updateData!!)

                            // No resuming twice allowed
                            setOnClickListener { }
                        }
                    }

                    showDownloadLink()
                    showDownloadError(
                        R.string.download_error_corrupt,
                        false
                    ) {
                        mainViewModel.enqueueDownloadWork(requireActivity(), updateData!!)
                    }
                    // Delete downloaded file, so the user can restart the download
                    mainViewModel.deleteDownloadedFile(requireContext(), updateData)
                }
            }
        }
    }

    private fun initDownloadActionButton(isCancelAction: Boolean) {
        val drawableResId: Int
        val colorResId: Int

        val onClickListener = if (isCancelAction) {
            drawableResId = R.drawable.close
            colorResId = R.color.colorError

            View.OnClickListener {
                mainViewModel.cancelDownloadWork(requireContext(), updateData)

                Handler().postDelayed(
                    { mainViewModel.updateDownloadStatus(NOT_DOWNLOADING) },
                    250
                )
            }
        } else {
            drawableResId = R.drawable.install
            colorResId = R.color.colorPositive

            View.OnClickListener {
                ActivityLauncher(requireActivity()).UpdateInstallation(true, updateData)
            }
        }

        downloadActionButton.apply {
            isVisible = true
            setImageResourceWithTint(drawableResId, colorResId)
            setOnClickListener(onClickListener)
        }
    }

    companion object {
        const val TAG = "UpdateInformationFragment"

        const val MANAGE_STORAGE_REQUEST_CODE = 300

        /**
         * Amount of free storage space to reserve when downloading an update.
         * Currently: `25 MB`
         */
        const val SAFE_MARGIN = 1048576 * 25L

        // In app message bar collections and identifiers.
        const val KEY_HAS_DOWNLOAD_ERROR = "has_download_error"
        const val KEY_DOWNLOAD_ERROR_TITLE = "download_error_title"
        const val KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message"
        const val KEY_DOWNLOAD_ERROR_RESUMABLE = "download_error_resumable"
    }
}
