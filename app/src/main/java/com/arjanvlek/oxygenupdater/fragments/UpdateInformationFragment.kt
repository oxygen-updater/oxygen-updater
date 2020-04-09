package com.arjanvlek.oxygenupdater.fragments

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
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import androidx.work.WorkInfo
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.dialogs.Dialogs
import com.arjanvlek.oxygenupdater.dialogs.Dialogs.showDownloadError
import com.arjanvlek.oxygenupdater.dialogs.Dialogs.showUpdateAlreadyDownloadedMessage
import com.arjanvlek.oxygenupdater.dialogs.ServerMessagesDialog
import com.arjanvlek.oxygenupdater.dialogs.UpdateChangelogDialog
import com.arjanvlek.oxygenupdater.enums.DownloadFailure
import com.arjanvlek.oxygenupdater.enums.DownloadStatus
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOADING
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_COMPLETED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_FAILED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_PAUSED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_QUEUED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.NOT_DOWNLOADING
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.VERIFICATION_COMPLETED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.VERIFICATION_FAILED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.VERIFYING
import com.arjanvlek.oxygenupdater.extensions.setImageResourceWithTint
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Banner
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.UpdateDataVersionFormatter
import com.arjanvlek.oxygenupdater.utils.UpdateDescriptionParser
import com.arjanvlek.oxygenupdater.utils.Utils
import com.arjanvlek.oxygenupdater.utils.Utils.SERVER_TIME_ZONE
import com.arjanvlek.oxygenupdater.viewmodels.MainViewModel
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_DOWNLOAD_BYTES_DONE
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_DOWNLOAD_ETA
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_DOWNLOAD_FAILURE_TYPE
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_DOWNLOAD_PROGRESS
import com.arjanvlek.oxygenupdater.workers.WORK_DATA_DOWNLOAD_TOTAL_BYTES
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.fragment_update_information.*
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
    private val mainViewModel by sharedViewModel<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (settingsManager.checkIfSetupScreenHasBeenCompleted()) {
            swipeRefreshLayout.apply {
                setOnRefreshListener { load() }
                setColorSchemeResources(R.color.colorPrimary)
            }

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

        Crashlytics.setUserIdentifier(
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
     * @param updateData              Update information to display
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
            displayUpdateInformationWhenUpToDate(updateData, online)
        } else {
            displayUpdateInformationWhenNotUpToDate(updateData)
        }

        if (online) {
            // Save update data for offline viewing
            settingsManager.apply {
                savePreference(SettingsManager.PROPERTY_OFFLINE_ID, updateData.id)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME, updateData.versionNumber)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, updateData.downloadSizeInMegabytes)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION, updateData.description)
                savePreference(SettingsManager.PROPERTY_OFFLINE_FILE_NAME, updateData.filename)
                savePreference(SettingsManager.PROPERTY_OFFLINE_DOWNLOAD_URL, updateData.downloadUrl)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, updateData.isUpdateInformationAvailable)
                savePreference(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now(SERVER_TIME_ZONE).toString())
                savePreference(SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE, updateData.systemIsUpToDate)
            }
        }
    }

    private fun displayUpdateInformationWhenUpToDate(updateData: UpdateData, online: Boolean) {
        // Show "System is up to date" view.
        systemIsUpToDateLayoutStub?.inflate()

        // Set the current OxygenOS version if available.
        systemIsUpToDateVersionTextView.apply {
            val oxygenOSVersion = systemVersionProperties.oxygenOSVersion

            if (oxygenOSVersion != OxygenUpdater.NO_OXYGEN_OS) {
                isVisible = true
                text = getString(R.string.update_information_oxygen_os_version, oxygenOSVersion)
            } else {
                isVisible = false
            }
        }

        val formattedOxygenOsVersion = if (updateData.versionNumber != null && updateData.versionNumber != "null") {
            if (UpdateDataVersionFormatter.canVersionInfoBeFormatted(updateData)) {
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData)
            } else {
                updateData.versionNumber
            }
        } else {
            getString(
                R.string.update_information_unknown_update_name,
                settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE, getString(R.string.device_information_unknown))
            )
        }

        // display a notice in the dialog if the user's currently installed version doesn't match the version this changelog is meant for
        val differentVersionChangelogNoticeText = if (updateData.otaVersionNumber != systemVersionProperties.oxygenOSOTAVersion) {
            getString(
                R.string.update_information_different_version_changelog_notice,
                settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "'<UNKNOWN>'")
            )
        } else {
            null
        }

        val updateChangelogDialog = UpdateChangelogDialog(
            requireContext(),
            formattedOxygenOsVersion,
            getUpdateChangelog(updateData.description),
            differentVersionChangelogNoticeText
        )

        // Set "No Update Information Is Available" button if needed.
        if (!updateData.isUpdateInformationAvailable) {
            systemIsUpToDateStatisticsButton.isVisible = true
            systemIsUpToDateChangelogView.isVisible = false
        } else {
            systemIsUpToDateStatisticsButton.isVisible = false
            systemIsUpToDateChangelogView.isVisible = true

            systemIsUpToDateChangelogView.setOnClickListener { updateChangelogDialog.show() }
        }

        // Save last time checked if online.
        if (online) {
            settingsManager.savePreference(
                SettingsManager.PROPERTY_UPDATE_CHECKED_DATE,
                LocalDateTime.now(SERVER_TIME_ZONE).toString()
            )
        }

        // Show last time checked.
        systemIsUpToDateDateTextView.text = getString(
            R.string.update_information_last_checked_on,
            Utils.formatDateTime(requireContext(), settingsManager.getPreference<String?>(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, null))
        )
    }

    private fun displayUpdateInformationWhenNotUpToDate(updateData: UpdateData) {
        // Show "System update available" view.
        updateInformationLayoutStub?.inflate()

        val formattedOxygenOsVersion = if (updateData.versionNumber != null && updateData.versionNumber != "null") {
            if (UpdateDataVersionFormatter.canVersionInfoBeFormatted(updateData)) {
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData)
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
            (activity as MainActivity?)?.updateTabBadge(1)

            footerTextView.isVisible = false
            footerDivider.isVisible = false
        }

        // Display available update version number.
        oxygenOsVersionTextView.text = formattedOxygenOsVersion

        // Display download size.
        downloadSizeTextView.text = getString(R.string.download_size_megabyte, updateData.downloadSizeInMegabytes)

        // Display update description.
        changelogTextView.apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = getUpdateChangelog(updateData.description)
        }

        // Display update file name.
        fileNameTextView.text = getString(R.string.update_information_file_name, updateData.filename)
        md5TextView.text = getString(R.string.update_information_md5, updateData.mD5Sum)

        mainViewModel.setupWorkRequests(updateData)

        if (mainViewModel.checkDownloadCompletionByFile(updateData)) {
            mainViewModel.updateDownloadStatus(DOWNLOAD_COMPLETED)
        } else {
            mainViewModel.updateDownloadStatus(NOT_DOWNLOADING)
        }
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
                    downloadSizeTextView.text = Formatter.formatFileSize(context, updateData?.downloadSize ?: 0)

                    downloadProgressBar.isVisible = false
                    downloadActionButton.isVisible = false
                    downloadDetailsTextView.isVisible = false

                    downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPrimary)

                    downloadLayout.apply {
                        val shouldEnable = updateData?.downloadUrl?.contains("http") == true
                        isEnabled = shouldEnable
                        isClickable = shouldEnable

                        if (shouldEnable) {
                            setOnClickListener(DownloadNotStartedOnClickListener())
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

                        // Since download has completed successfully, we can start the verification work immediately
                        Toast.makeText(
                            context,
                            getString(R.string.download_verifying_start), LENGTH_LONG
                        ).show()
                        mainViewModel.enqueueVerificationWork()
                    } else {
                        downloadLayout.apply {
                            isEnabled = true
                            isClickable = true
                            setOnClickListener(AlreadyDownloadedOnClickListener())
                        }
                    }
                }
                VERIFICATION_COMPLETED -> {
                    initDownloadActionButton(false)

                    downloadUpdateTextView.setText(R.string.downloaded)

                    downloadProgressBar.isVisible = false
                    downloadDetailsTextView.isVisible = false

                    downloadIcon.setImageResourceWithTint(R.drawable.done_outline, R.color.colorPositive)

                    downloadLayout.apply {
                        isEnabled = true
                        isClickable = true
                        setOnClickListener(AlreadyDownloadedOnClickListener())
                    }

                    // Since file has been verified, we can prune the work and launch [InstallActivity]
                    mainViewModel.maybePruneWork()
                    Toast.makeText(context, getString(R.string.download_complete), LENGTH_LONG).show()
                    ActivityLauncher(requireActivity()).UpdateInstallation(true, updateData)
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

                    // Show error message
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
        val onClickListener: View.OnClickListener

        if (isCancelAction) {
            drawableResId = R.drawable.close
            colorResId = R.color.colorError

            onClickListener = View.OnClickListener {
                mainViewModel.cancelDownloadWork(requireContext(), updateData)

                Handler().postDelayed(
                    { mainViewModel.updateDownloadStatus(NOT_DOWNLOADING) },
                    250
                )
            }
        } else {
            drawableResId = R.drawable.install
            colorResId = R.color.colorPositive

            onClickListener = View.OnClickListener {
                ActivityLauncher(requireActivity()).UpdateInstallation(true, updateData)
            }
        }

        downloadActionButton.apply {
            isVisible = true
            setImageResourceWithTint(drawableResId, colorResId)
            setOnClickListener(onClickListener)
        }
    }

    /**
     * Download button click listener. Performs these actions when the button is clicked.
     */
    private inner class DownloadNotStartedOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            if (isAdded && updateInformationLayout != null) {
                val mainActivity = activity as MainActivity? ?: return

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
    }

    /**
     * Allows an already downloaded update file to be deleted to save storage space.
     */
    private inner class AlreadyDownloadedOnClickListener internal constructor() : View.OnClickListener {
        override fun onClick(view: View) {
            showUpdateAlreadyDownloadedMessage(updateData, this@UpdateInformationFragment.activity) {
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
