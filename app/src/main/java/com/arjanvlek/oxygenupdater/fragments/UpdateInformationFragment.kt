package com.arjanvlek.oxygenupdater.fragments

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.MainActivity
import com.arjanvlek.oxygenupdater.dialogs.Dialogs
import com.arjanvlek.oxygenupdater.dialogs.Dialogs.showDownloadError
import com.arjanvlek.oxygenupdater.dialogs.Dialogs.showNoNetworkConnectionError
import com.arjanvlek.oxygenupdater.dialogs.Dialogs.showUpdateAlreadyDownloadedMessage
import com.arjanvlek.oxygenupdater.dialogs.ServerMessagesDialog
import com.arjanvlek.oxygenupdater.dialogs.UpdateChangelogDialog
import com.arjanvlek.oxygenupdater.enums.DownloadStatus
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOADING
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_COMPLETED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_PAUSED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.DOWNLOAD_QUEUED
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.NOT_DOWNLOADING
import com.arjanvlek.oxygenupdater.enums.DownloadStatus.VERIFYING
import com.arjanvlek.oxygenupdater.extensions.setImageResourceWithTint
import com.arjanvlek.oxygenupdater.internal.UpdateDownloadListener
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Banner
import com.arjanvlek.oxygenupdater.models.DownloadProgressData
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.receivers.DownloadReceiver
import com.arjanvlek.oxygenupdater.services.DownloadService
import com.arjanvlek.oxygenupdater.services.DownloadService.Companion.ACTION_DELETE_DOWNLOADED_UPDATE
import com.arjanvlek.oxygenupdater.services.DownloadService.Companion.ACTION_DOWNLOAD_UPDATE
import com.arjanvlek.oxygenupdater.utils.UpdateDataVersionFormatter
import com.arjanvlek.oxygenupdater.utils.UpdateDescriptionParser
import com.arjanvlek.oxygenupdater.utils.Utils
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.fragment_update_information.*
import kotlinx.android.synthetic.main.layout_system_is_up_to_date.*
import kotlinx.android.synthetic.main.layout_update_information.*
import org.joda.time.LocalDateTime

class UpdateInformationFragment : AbstractFragment() {

    private lateinit var rootView: SwipeRefreshLayout
    private lateinit var mContext: Context

    private var downloadListener: UpdateDownloadListener? = null
    private var updateData: UpdateData? = null
    private var isLoadedOnce = false
    private var downloadReceiver: DownloadReceiver? = null

    /*
      -------------- ANDROID ACTIVITY LIFECYCLE METHODS -------------------
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = applicationData as Context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        rootView = inflater.inflate(R.layout.fragment_update_information, container, false) as SwipeRefreshLayout
        return rootView
    }

    override fun onStart() {
        super.onStart()

        if (isAdded && settingsManager!!.checkIfSetupScreenHasBeenCompleted()) {
            rootView.setOnRefreshListener { load() }
            rootView.setColorSchemeResources(R.color.colorPrimary)

            load()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isLoadedOnce) {
            registerDownloadReceiver(downloadListener!!)

            // If service reports being inactive, check if download is finished or paused to update state.
            // If download is running then it auto-updates the UI using the downloadListener.
            if (!DownloadService.isRunning() && updateData != null) {
                DownloadService.performOperation(activity, DownloadService.ACTION_GET_INITIAL_STATUS, updateData)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (downloadReceiver != null) {
            unregisterDownloadReceiver()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (downloadReceiver != null) {
            unregisterDownloadReceiver()
        }

        if (activity != null && isDownloadServiceRunning) {
            activity!!.stopService(Intent(mContext, DownloadService::class.java))
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
                    + settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE, "<UNKNOWN>")
                    + ", Update Method: "
                    + settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
        )

        val deviceId = settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        val online = Utils.checkNetworkConnection(applicationData)
        val serverConnector = applicationData?.serverConnector!!
        val systemVersionProperties = applicationData?.systemVersionProperties!!

        serverConnector.getUpdateData(online, deviceId, updateMethodId, systemVersionProperties.oxygenOSOTAVersion, { updateData ->
            this.updateData = updateData

            if (!isLoadedOnce) {
                downloadListener = buildDownloadListener(updateData)
                registerDownloadReceiver(downloadListener!!)

                DownloadService.performOperation(activity, DownloadService.ACTION_GET_INITIAL_STATUS, updateData)
            }

            // If the activity is started with a download error (when clicked on a "download failed" notification), show it to the user.
            if (!isLoadedOnce && activity?.intent?.getBooleanExtra(KEY_HAS_DOWNLOAD_ERROR, false) == true) {
                val intent = activity!!.intent
                showDownloadError(
                    activity,
                    updateData,
                    intent.getBooleanExtra(KEY_DOWNLOAD_ERROR_RESUMABLE, false),
                    intent.getStringExtra(KEY_DOWNLOAD_ERROR_TITLE),
                    intent.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE)
                )
            }

            displayUpdateInformation(updateData, online)
            isLoadedOnce = true
        }) { error ->
            if (error == ApplicationData.NETWORK_CONNECTION_ERROR) {
                showNoNetworkConnectionError(activity)
            }
        }

        // display the "No connection" banner if required
        ApplicationData.isNetworkAvailable.observe(viewLifecycleOwner) {
            noConnectionTextView.isVisible = !it
        }

        serverConnector.getServerStatus(online) { serverStatus ->
            // display server status banner if required
            val status = serverStatus.status
            if (status!!.isUserRecoverableError) {
                serverStatusTextView.apply {
                    isVisible = true
                    text = serverStatus.getBannerText(context!!)
                    setBackgroundColor(serverStatus.getColor(context!!))
                    setCompoundDrawablesRelativeWithIntrinsicBounds(serverStatus.getDrawableRes(context!!), 0, 0, 0)
                }
            }

            // banner is displayed if app version is outdated
            if (settingsManager!!.getPreference(SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !serverStatus.checkIfAppIsUpToDate()) {
                appUpdateBannerLayout.isVisible = true
                appUpdateBannerLayout.setOnClickListener { ActivityLauncher(activity!!).openPlayStorePage(context!!) }
                appUpdateBannerTextView.text = getString(R.string.new_app_version, serverStatus.latestAppVersion)
            }

            serverConnector.getInAppMessages(serverStatus, { displayServerMessageBars(it) }) { error ->
                when (error) {
                    ApplicationData.SERVER_MAINTENANCE_ERROR -> Dialogs.showServerMaintenanceError(activity)
                    ApplicationData.APP_OUTDATED_ERROR -> Dialogs.showAppOutdatedError(activity)
                }
            }
        }
    }

    /**
     * Makes [serverBannerTextView] visible.
     *
     * If there are banners that have non-empty text, clicking [serverBannerTextView] will show a [ServerMessagesDialog]
     */
    private fun displayServerMessageBars(banners: List<Banner>) {
        // select only the banners that have a non-empty text (`ServerStatus.kt` has empty text in some cases)
        val bannerList = banners.filter { !it.getBannerText(context!!).isNullOrBlank() }

        if (!isAdded || bannerList.isEmpty()) {
            return
        }

        val dialog = ServerMessagesDialog(context!!, bannerList)

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
     * @param online                  Whether or not the device has an active network connection
     */
    private fun displayUpdateInformation(updateData: UpdateData?, online: Boolean) {
        // Abort if no update data is found or if the fragment is not attached to its activity to prevent crashes.
        if (!isAdded || updateData == null) {
            return
        }

        // Hide the loading screen
        updateInformationProgressBar.isVisible = false

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
            settingsManager?.apply {
                savePreference(SettingsManager.PROPERTY_OFFLINE_ID, updateData.id)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME, updateData.versionNumber)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, updateData.downloadSizeInMegabytes)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION, updateData.description)
                savePreference(SettingsManager.PROPERTY_OFFLINE_FILE_NAME, updateData.filename)
                savePreference(SettingsManager.PROPERTY_OFFLINE_DOWNLOAD_URL, updateData.downloadUrl)
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, updateData.isUpdateInformationAvailable)
                savePreference(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString())
                savePreference(SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE, updateData.systemIsUpToDate)
            }
        }

        // Hide the refreshing icon if it is present.
        rootView.isRefreshing = false
    }

    private fun updateBannerText(string: String) {
        bannerLayout.isVisible = true
        bannerTextView.text = string
    }

    private fun displayUpdateInformationWhenUpToDate(updateData: UpdateData, online: Boolean) {
        if (activity?.application == null) {
            return
        }

        updateBannerText(getString(R.string.update_information_banner_congrats_latest))

        // Show "System is up to date" view.
        systemIsUpToDateLayoutStub?.inflate()
        // hide the loading shimmer
        shimmerFrameLayout.isVisible = false

        // Set the current OxygenOS version if available.
        systemIsUpToDateVersionTextView.apply {
            val oxygenOSVersion = (activity?.application as ApplicationData).systemVersionProperties?.oxygenOSVersion

            if (oxygenOSVersion != ApplicationData.NO_OXYGEN_OS) {
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
                settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE, getString(R.string.device_information_unknown))
            )
        }

        // display a notice in the dialog if the user's currently installed version doesn't match the version this changelog is meant for
        val differentVersionChangelogNoticeText = if (updateData.otaVersionNumber != (activity?.application as ApplicationData).systemVersionProperties?.oxygenOSOTAVersion) {
            getString(
                R.string.update_information_different_version_changelog_notice,
                settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "'<UNKNOWN>'")
            )
        } else {
            null
        }

        val updateChangelogDialog = UpdateChangelogDialog(
            context!!,
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
            settingsManager!!.savePreference(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString())
        }

        // Show last time checked.
        systemIsUpToDateDateTextView.text = getString(
            R.string.update_information_last_checked_on,
            Utils.formatDateTime(mContext, settingsManager!!.getPreference<String?>(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, null))
        )
    }

    private fun displayUpdateInformationWhenNotUpToDate(updateData: UpdateData) {
        // Show "System update available" view.
        updateInformationLayoutStub?.inflate()
        // hide the loading shimmer
        shimmerFrameLayout.isVisible = false

        val formattedOxygenOsVersion = if (updateData.versionNumber != null && updateData.versionNumber != "null") {
            if (UpdateDataVersionFormatter.canVersionInfoBeFormatted(updateData)) {
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData)
            } else {
                updateData.versionNumber
            }
        } else {
            getString(
                R.string.update_information_unknown_update_name,
                settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE, getString(R.string.device_information_unknown))
            )
        }

        if (updateData.systemIsUpToDate) {
            updateBannerText(getString(R.string.update_information_banner_already_latest))

            val updateMethod = settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "'<UNKNOWN>'")

            // Format footer based on system version installed.
            footerTextView.text = getString(R.string.update_information_header_advanced_mode_helper, updateMethod)

            footerTextView.isVisible = true
            footerDivider.isVisible = true
        } else {
            updateBannerText(getString(R.string.update_information_banner_update_available, formattedOxygenOsVersion))
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
    }

    private fun getUpdateChangelog(description: String?): CharSequence {
        return if (!description.isNullOrBlank() && description != "null") {
            UpdateDescriptionParser.parse(context, description).trim()
        } else {
            getString(R.string.update_information_description_not_available)
        }
    }

    private fun showDownloadLink(updateData: UpdateData?) {
        if (updateData != null) {
            downloadLinkTextView.apply {
                isVisible = true
                movementMethod = LinkMovementMethod.getInstance()
                text = SpannableString(
                    getString(R.string.update_information_download_link, updateData.downloadUrl)
                ).apply {
                    setSpan(URLSpan(updateData.downloadUrl), indexOf("\n") + 1, length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    /**
     * Creates an [UpdateDownloadListener]
     */
    private fun buildDownloadListener(updateData: UpdateData?): UpdateDownloadListener {
        return object : UpdateDownloadListener {
            override fun onInitialStatusUpdate() {
                if (isAdded && updateInformationLayout != null) {
                    downloadLayout.setOnClickListener {
                        downloadIcon.setImageResourceWithTint(android.R.drawable.stat_sys_download, R.color.colorPositive)
                        (downloadIcon.drawable as AnimationDrawable).start()

                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)

                        initDownloadLayout(updateData, DOWNLOAD_PAUSED)
                    }
                }
            }

            override fun onDownloadStarted() {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, DOWNLOADING)

                    downloadLayout.setOnClickListener {
                        downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPositive)

                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)

                        initDownloadLayout(updateData, DOWNLOAD_PAUSED)

                        // Prevents sending duplicate Intents, will be automatically overridden in onDownloadPaused().
                        downloadLayout.setOnClickListener { }
                    }
                }
            }

            override fun onDownloadProgressUpdate(downloadProgressData: DownloadProgressData) {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, DOWNLOADING)

                    val progress = downloadProgressData.progress

                    downloadProgressBar.isIndeterminate = false
                    downloadProgressBar.progress = progress

                    val downloadSizeInMegabytes = updateData!!.downloadSizeInMegabytes
                    val completedMegabytes = (progress / 100f * downloadSizeInMegabytes).toInt()

                    downloadSizeTextView.text = getString(
                        R.string.download_progress,
                        completedMegabytes, updateData.downloadSizeInMegabytes, progress
                    )

                    if (downloadProgressData.isWaitingForConnection) {
                        downloadDetailsTextView.setText(R.string.download_waiting_for_network)
                        return
                    }

                    if (downloadProgressData.timeRemaining == null) {
                        downloadDetailsTextView.setText(R.string.download_progress_text_unknown_time_remaining)
                    } else {
                        downloadDetailsTextView.text = downloadProgressData.timeRemaining.toString(applicationData)
                    }
                }
            }

            override fun onDownloadPaused(queued: Boolean, downloadProgressData: DownloadProgressData) {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, DOWNLOAD_PAUSED)

                    if (downloadProgressData.isWaitingForConnection) {
                        onDownloadProgressUpdate(downloadProgressData)
                        return
                    }

                    if (!queued) {
                        downloadProgressBar.progress = downloadProgressData.progress
                    } else {
                        downloadDetailsTextView.setText(R.string.download_pending)
                    }
                }
            }

            override fun onDownloadComplete() {
                if (isAdded && updateInformationLayout != null) {
                    Toast.makeText(applicationData, getString(R.string.download_verifying_start), LENGTH_LONG).show()
                }
            }

            override fun onDownloadCancelled() {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, NOT_DOWNLOADING)
                }
            }

            override fun onDownloadError(isInternalError: Boolean, isStorageSpaceError: Boolean, isServerError: Boolean) {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, NOT_DOWNLOADING)

                    when {
                        isStorageSpaceError -> showDownloadError(updateData, R.string.download_error_storage)
                        isServerError -> {
                            showDownloadLink(updateData)
                            showDownloadError(updateData, R.string.download_error_server)
                        }
                        else -> {
                            showDownloadLink(updateData)
                            showDownloadError(updateData, R.string.download_error_internal)
                        }
                    }
                }
            }

            override fun onVerifyStarted() {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, VERIFYING)
                }
            }

            override fun onVerifyError() {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, NOT_DOWNLOADING)

                    showDownloadError(updateData, R.string.download_error_corrupt)
                }
            }

            override fun onVerifyComplete(launchInstallation: Boolean) {
                if (isAdded && updateInformationLayout != null) {
                    initDownloadLayout(updateData, DOWNLOAD_COMPLETED)

                    if (launchInstallation) {
                        Toast.makeText(applicationData, getString(R.string.download_complete), LENGTH_LONG).show()

                        ActivityLauncher(activity!!).UpdateInstallation(true, updateData)
                    }
                }
            }
        }
    }

    private fun showDownloadError(updateData: UpdateData?, @StringRes message: Int) {
        showDownloadError(activity!!, updateData, false, R.string.download_error, message)
    }

    private fun initDownloadLayout(updateData: UpdateData?, downloadStatus: DownloadStatus) {
        when (downloadStatus) {
            NOT_DOWNLOADING -> {
                initDownloadActionButton(true)

                downloadUpdateTextView.setText(R.string.download)
                downloadSizeTextView.text = getString(R.string.download_size_megabyte, updateData?.downloadSizeInMegabytes)

                downloadProgressBar.isVisible = false
                downloadActionButton.isVisible = false
                downloadDetailsTextView.isVisible = false

                downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPrimary)

                if (Utils.checkNetworkConnection(applicationData) && updateData?.downloadUrl?.contains("http") == true) {
                    downloadLayout.isEnabled = true
                    downloadLayout.isClickable = true
                    downloadLayout.setOnClickListener(DownloadNotStartedOnClickListener(updateData))
                } else {
                    downloadLayout.isEnabled = false
                    downloadLayout.isClickable = false
                }

                if (updateData == null) {
                    load()
                }
            }
            DOWNLOAD_QUEUED, DOWNLOADING -> {
                initDownloadActionButton(true)

                downloadUpdateTextView.setText(R.string.downloading)

                downloadProgressBar.isVisible = true
                downloadDetailsTextView.isVisible = true

                downloadLayout.isEnabled = true
                downloadLayout.isClickable = false

                downloadIcon.setImageResourceWithTint(android.R.drawable.stat_sys_download, R.color.colorPositive)
                (downloadIcon.drawable as AnimationDrawable).start()

                downloadLayout.setOnClickListener {
                    downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPositive)

                    DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)

                    initDownloadLayout(updateData, DOWNLOAD_PAUSED)

                    // Prevents sending duplicate Intents, will be automatically overridden in onDownloadPaused().
                    downloadLayout.setOnClickListener { }
                }
            }
            DOWNLOAD_PAUSED -> {
                initDownloadActionButton(true)

                downloadUpdateTextView.setText(R.string.paused)
                downloadDetailsTextView.setText(R.string.download_progress_text_paused)

                downloadProgressBar.isVisible = true
                downloadDetailsTextView.isVisible = true

                downloadLayout.isEnabled = true
                downloadLayout.isClickable = false

                downloadIcon.setImageResourceWithTint(R.drawable.download, R.color.colorPositive)

                downloadLayout.setOnClickListener {
                    downloadIcon.setImageResourceWithTint(android.R.drawable.stat_sys_download, R.color.colorPositive)
                    (downloadIcon.drawable as AnimationDrawable).start()

                    DownloadService.performOperation(activity, DownloadService.ACTION_RESUME_DOWNLOAD, updateData)

                    initDownloadLayout(updateData, DOWNLOADING)

                    // No resuming twice allowed, will be updated in onDownloadProgressUpdate()
                    downloadLayout.setOnClickListener { }
                }
            }
            DOWNLOAD_COMPLETED -> {
                initDownloadActionButton(false)

                downloadUpdateTextView.setText(R.string.downloaded)

                downloadProgressBar.isVisible = false
                downloadDetailsTextView.isVisible = false

                initDownloadActionButton(false)

                downloadLayout.isEnabled = true
                downloadLayout.isClickable = true

                downloadIcon.setImageResourceWithTint(R.drawable.done_outline, R.color.colorPositive)

                downloadLayout.setOnClickListener(AlreadyDownloadedOnClickListener(updateData))
            }
            VERIFYING -> {
                downloadUpdateTextView.setText(R.string.download_verifying)
                downloadDetailsTextView.setText(R.string.download_progress_text_verifying)

                downloadProgressBar.isVisible = true
                downloadActionButton.isVisible = false
                downloadDetailsTextView.isVisible = true

                downloadLayout.isEnabled = true
                downloadLayout.isClickable = false
                downloadProgressBar.isIndeterminate = true
            }
            DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION -> {
                initDownloadActionButton(true)

                downloadDetailsTextView.setText(R.string.download_waiting_for_network)

                downloadProgressBar.isVisible = true
                downloadDetailsTextView.isVisible = true

                downloadProgressBar.isIndeterminate = true
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
                DownloadService.performOperation(activity, DownloadService.ACTION_CANCEL_DOWNLOAD, updateData)
                initDownloadLayout(updateData, NOT_DOWNLOADING)
            }
        } else {
            drawableResId = R.drawable.install
            colorResId = R.color.colorPositive

            onClickListener = View.OnClickListener {
                ActivityLauncher(activity!!).UpdateInstallation(true, updateData)
            }
        }

        downloadActionButton.isVisible = true
        downloadActionButton.setImageResourceWithTint(drawableResId, colorResId)
        downloadActionButton.setOnClickListener(onClickListener)
    }

    private fun registerDownloadReceiver(downloadListener: UpdateDownloadListener) {
        val filter = IntentFilter(DownloadReceiver.ACTION_DOWNLOAD_EVENT)
        filter.addCategory(Intent.CATEGORY_DEFAULT)

        downloadReceiver = DownloadReceiver(downloadListener)

        activity?.registerReceiver(downloadReceiver, filter)
    }

    private fun unregisterDownloadReceiver() {
        if (activity != null) {
            activity?.unregisterReceiver(downloadReceiver)
            downloadReceiver = null
        }
    }

    private val isDownloadServiceRunning: Boolean
        get() {
            val manager = (activity?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?) ?: return false

            @Suppress("DEPRECATION")
            manager.getRunningServices(5).forEach {
                if (DownloadService::class.java.name == it.service.className) {
                    return true
                }
            }

            return false
        }

    /**
     * Download button click listener. Performs these actions when the button is clicked.
     */
    private inner class DownloadNotStartedOnClickListener internal constructor(private val updateData: UpdateData) : View.OnClickListener {
        override fun onClick(view: View) {
            if (isAdded && updateInformationLayout != null) {
                val mainActivity = activity as MainActivity? ?: return

                if (mainActivity.hasDownloadPermissions()) {
                    DownloadService.performOperation(activity, ACTION_DOWNLOAD_UPDATE, updateData)

                    initDownloadLayout(updateData, DOWNLOADING)

                    downloadProgressBar.isIndeterminate = true
                    downloadDetailsTextView.setText(R.string.download_pending)

                    // Pause is possible on first progress update
                    downloadLayout.setOnClickListener { }
                } else {
                    mainActivity.requestDownloadPermissions { granted ->
                        if (granted) {
                            DownloadService.performOperation(activity, ACTION_DOWNLOAD_UPDATE, updateData)
                        }
                    }
                }
            }
        }
    }

    /**
     * Allows an already downloaded update file to be deleted to save storage space.
     */
    private inner class AlreadyDownloadedOnClickListener internal constructor(private val updateData: UpdateData?) : View.OnClickListener {
        override fun onClick(view: View) {
            showUpdateAlreadyDownloadedMessage(updateData, this@UpdateInformationFragment.activity) {
                if (updateData != null) {
                    val mainActivity = activity as MainActivity? ?: return@showUpdateAlreadyDownloadedMessage

                    if (mainActivity.hasDownloadPermissions()) {
                        DownloadService.performOperation(activity, ACTION_DELETE_DOWNLOADED_UPDATE, updateData)

                        initDownloadLayout(updateData, NOT_DOWNLOADING)
                    } else {
                        mainActivity.requestDownloadPermissions { granted ->
                            if (granted) {
                                DownloadService.performOperation(activity, ACTION_DELETE_DOWNLOADED_UPDATE, updateData)

                                initDownloadLayout(updateData, NOT_DOWNLOADING)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        // In app message bar collections and identifiers.
        const val KEY_HAS_DOWNLOAD_ERROR = "has_download_error"
        const val KEY_DOWNLOAD_ERROR_TITLE = "download_error_title"
        const val KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message"
        const val KEY_DOWNLOAD_ERROR_RESUMABLE = "download_error_resumable"
    }
}
