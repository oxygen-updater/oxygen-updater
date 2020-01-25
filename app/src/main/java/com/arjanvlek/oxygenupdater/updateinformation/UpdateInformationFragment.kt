package com.arjanvlek.oxygenupdater.updateinformation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.download.DownloadReceiver
import com.arjanvlek.oxygenupdater.download.DownloadService
import com.arjanvlek.oxygenupdater.download.DownloadService.Companion.ACTION_DELETE_DOWNLOADED_UPDATE
import com.arjanvlek.oxygenupdater.download.DownloadService.Companion.ACTION_DOWNLOAD_UPDATE
import com.arjanvlek.oxygenupdater.download.DownloadStatus
import com.arjanvlek.oxygenupdater.download.DownloadStatus.DOWNLOADING
import com.arjanvlek.oxygenupdater.download.DownloadStatus.DOWNLOAD_COMPLETED
import com.arjanvlek.oxygenupdater.download.DownloadStatus.NOT_DOWNLOADING
import com.arjanvlek.oxygenupdater.download.DownloadStatus.VERIFYING
import com.arjanvlek.oxygenupdater.download.UpdateDownloadListener
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.models.Banner
import com.arjanvlek.oxygenupdater.models.DownloadProgressData
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.notifications.Dialogs.showAppOutdatedError
import com.arjanvlek.oxygenupdater.notifications.Dialogs.showDownloadError
import com.arjanvlek.oxygenupdater.notifications.Dialogs.showNoNetworkConnectionError
import com.arjanvlek.oxygenupdater.notifications.Dialogs.showServerMaintenanceError
import com.arjanvlek.oxygenupdater.notifications.Dialogs.showUpdateAlreadyDownloadedMessage
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications.hideDownloadCompleteNotification
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.versionformatter.UpdateDataVersionFormatter
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.MainActivity
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.fragment_update_information.*
import org.joda.time.LocalDateTime
import java.util.*

class UpdateInformationFragment : AbstractFragment() {
    private lateinit var updateInformationRefreshLayout: SwipeRefreshLayout
    private lateinit var systemIsUpToDateRefreshLayout: SwipeRefreshLayout
    private lateinit var rootView: RelativeLayout
    private lateinit var mContext: Context

    private var downloadListener: UpdateDownloadListener? = null
    private var updateData: UpdateData? = null
    private var isLoadedOnce = false
    private var serverMessageBars = ArrayList<ServerMessageBar>()
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
        rootView = inflater.inflate(R.layout.fragment_update_information, container, false) as RelativeLayout
        return rootView
    }

    override fun onStart() {
        super.onStart()

        if (isAdded && settingsManager!!.checkIfSetupScreenHasBeenCompleted()) {
            updateInformationRefreshLayout = rootView.findViewById(R.id.updateInformationRefreshLayout)
            systemIsUpToDateRefreshLayout = rootView.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout)

            updateInformationRefreshLayout.setOnRefreshListener { load() }
            updateInformationRefreshLayout.setColorSchemeResources(R.color.colorPrimary)

            systemIsUpToDateRefreshLayout.setOnRefreshListener { load() }
            systemIsUpToDateRefreshLayout.setColorSchemeResources(R.color.colorPrimary)

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
        Crashlytics.setUserIdentifier(
            "Device: "
                    + settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE, "<UNKNOWN>")
                    + ", Update Method: "
                    + settingsManager!!.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
        )

        val instance: AbstractFragment = this
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
                    instance,
                    updateData,
                    intent.getBooleanExtra(KEY_DOWNLOAD_ERROR_RESUMABLE, false),
                    intent.getStringExtra(KEY_DOWNLOAD_ERROR_TITLE),
                    intent.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE)
                )
            }

            displayUpdateInformation(updateData, online, false)
            isLoadedOnce = true
        }) { error ->
            if (error == ApplicationData.NETWORK_CONNECTION_ERROR) {
                showNoNetworkConnectionError(instance)
            }
        }

        serverConnector.getInAppMessages(online, { displayServerMessageBars(it) }) { error ->
            when (error) {
                ApplicationData.SERVER_MAINTENANCE_ERROR -> showServerMaintenanceError(instance)
                ApplicationData.APP_OUTDATED_ERROR -> showAppOutdatedError(instance, activity)
            }
        }
    }

    /*
      -------------- METHODS FOR DISPLAYING DATA ON THE FRAGMENT -------------------
     */
    private fun addServerMessageBar(view: ServerMessageBar) {
        // Add the message to the update information screen.
        // Set the layout params based on the view count.
        // First view should go below the app update message bar (if visible)
        // Consecutive views should go below their parent / previous view.
        val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val numberOfBars = serverMessageBars.size

        // position each bar below the previous one
        if (serverMessageBars.isNotEmpty()) {
            params.addRule(RelativeLayout.BELOW, serverMessageBars[serverMessageBars.size - 1].id)
        }

        view.id = numberOfBars * 20000 + 1
        rootView.addView(view, params)
        serverMessageBars.add(view)
    }

    private fun deleteAllServerMessageBars() {
        serverMessageBars.forEach {
            rootView.removeView(it)
        }

        serverMessageBars = ArrayList()
    }

    private fun displayServerMessageBars(banners: List<Banner>) {
        if (!isAdded) {
            return
        }

        deleteAllServerMessageBars()

        val createdServerMessageBars = ArrayList<ServerMessageBar>()

        banners.forEach {
            val bar = ServerMessageBar(activity)
            val backgroundBar = bar.backgroundBar
            val textView = bar.textView

            backgroundBar.setBackgroundColor(it.getColor(mContext))
            textView.text = it.getBannerText(mContext)

            if (it.getBannerText(mContext) is Spanned) {
                textView.movementMethod = LinkMovementMethod.getInstance()
            }

            addServerMessageBar(bar)
            createdServerMessageBars.add(bar)
        }

        // Position the app UI  to be below the last added server message bar
        if (createdServerMessageBars.isNotEmpty()) {
            val lastServerMessageView: View = createdServerMessageBars[createdServerMessageBars.size - 1]
            val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            params.addRule(RelativeLayout.BELOW, lastServerMessageView.id)

            systemIsUpToDateRefreshLayout.layoutParams = params
            updateInformationRefreshLayout.layoutParams = params
        }

        serverMessageBars = createdServerMessageBars
    }

    /**
     * Displays the update information from a [UpdateData] with update information.
     *
     * @param updateData              Update information to display
     * @param online                  Whether or not the device has an active network connection
     * @param displayInfoWhenUpToDate Flag set to show update information anyway, even if the system is up to date.
     */
    private fun displayUpdateInformation(updateData: UpdateData?, online: Boolean, displayInfoWhenUpToDate: Boolean) {
        // Abort if no update data is found or if the fragment is not attached to its activity to prevent crashes.
        if (!isAdded || updateData == null) {
            return
        }

        // Hide the loading screen
        updateInformationLoadingScreen.visibility = GONE

        if (updateData.id == null) {
            displayUpdateInformationWhenUpToDate(updateData, online)
        }

        if (updateData.isSystemIsUpToDateCheck(settingsManager) && !displayInfoWhenUpToDate || !updateData.isUpdateInformationAvailable()) {
            displayUpdateInformationWhenUpToDate(updateData, online)
        } else {
            displayUpdateInformationWhenNotUpToDate(updateData, displayInfoWhenUpToDate)
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
                savePreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, updateData.isUpdateInformationAvailable())
                savePreference(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString())
                savePreference(SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE, updateData.systemIsUpToDate)
            }
        }

        // Hide the refreshing icon if it is present.
        hideRefreshIcons()
    }

    private fun displayUpdateInformationWhenUpToDate(updateData: UpdateData, online: Boolean) {
        if (activity?.application == null) {
            return
        }

        // Show "System is up to date" view.
        updateInformationRefreshLayout.visibility = GONE
        updateInformationSystemIsUpToDateRefreshLayout.visibility = VISIBLE

        // Set the current Oxygen OS version if available.
        updateInformationSystemIsUpToDateVersionTextView.apply {
            val oxygenOSVersion = (activity?.application as ApplicationData).systemVersionProperties?.oxygenOSVersion

            if (oxygenOSVersion != ApplicationData.NO_OXYGEN_OS) {
                visibility = VISIBLE
                text = String.format(getString(R.string.update_information_oxygen_os_version), oxygenOSVersion)
            } else {
                visibility = GONE
            }
        }

        // Set "No Update Information Is Available" button if needed.
        updateInformationSystemIsUpToDateStatisticsButton.apply {
            if (!updateData.isUpdateInformationAvailable()) {
                text = getString(R.string.update_information_no_update_data_available)
                isClickable = false
            } else {
                text = getString(R.string.update_information_view_update_information)
                isClickable = true
                setOnClickListener { displayUpdateInformation(updateData, online, true) }
            }
        }

        // Save last time checked if online.
        if (online) {
            settingsManager!!.savePreference(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString())
        }

        // Show last time checked.
        updateInformationSystemIsUpToDateDateTextView.text = String.format(
            getString(R.string.update_information_last_checked_on),
            Utils.formatDateTime(mContext, settingsManager!!.getPreference<String?>(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE, null))
        )
    }

    private fun displayUpdateInformationWhenNotUpToDate(updateData: UpdateData, displayInfoWhenUpToDate: Boolean) {
        showDownloadLink(updateData)
        // Show "System update available" view.
        updateInformationRefreshLayout.visibility = VISIBLE
        updateInformationSystemIsUpToDateRefreshLayout.visibility = GONE

        // Display available update version number.
        updateInformationBuildNumberView.text = if (updateData.versionNumber != null && updateData.versionNumber != "null") {
            if (UpdateDataVersionFormatter.canVersionInfoBeFormatted(updateData)) {
                UpdateDataVersionFormatter.getFormattedVersionNumber(updateData)
            } else {
                updateData.versionNumber
            }
        } else {
            String.format(
                getString(R.string.update_information_unknown_update_name),
                settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE, mContext.getString(R.string.device_information_unknown))
            )
        }

        // Display download size.
        val downloadSizeView = rootView.findViewById<TextView>(R.id.updateInformationDownloadSizeView)
        downloadSizeView.text = String.format(getString(R.string.download_size_megabyte), updateData.downloadSizeInMegabytes)

        // Display update description.
        updateDescriptionView.apply {
            val description = updateData.description

            movementMethod = LinkMovementMethod.getInstance()
            text = if (!description.isNullOrBlank() && description != "null") {
                UpdateDescriptionParser.parse(context, description)
            } else {
                getString(R.string.update_information_description_not_available)
            }
        }

        // Display update file name.
        val fileNameView = updateFileNameView
        fileNameView.text = String.format(getString(R.string.update_information_file_name), updateData.filename)

        // Format top title based on system version installed.
        val headerLabel = rootView.findViewById<TextView>(R.id.headerLabel)
        val updateInstallationGuideButton = rootView.findViewById<Button>(R.id.updateInstallationInstructionsButton)
        val downloadSizeTable = rootView.findViewById<View>(R.id.buttonTable)
        val downloadSizeImage = rootView.findViewById<View>(R.id.downloadSizeImage)

        if (displayInfoWhenUpToDate) {
            headerLabel.text = getString(R.string.update_information_installed_update)

            updateInformationDownloadButton.visibility = GONE
            updateInstallationGuideButton.visibility = GONE
            fileNameView.visibility = GONE
            downloadSizeTable.visibility = GONE
            downloadSizeImage.visibility = GONE
            downloadSizeView.visibility = GONE
        } else {
            headerLabel.text = if (updateData.systemIsUpToDate) {
                getString(R.string.update_information_installed_update)
            } else {
                getString(R.string.update_information_latest_available_update)
            }

            updateInformationDownloadButton.visibility = VISIBLE
            fileNameView.visibility = VISIBLE
            downloadSizeTable.visibility = VISIBLE
            downloadSizeImage.visibility = VISIBLE
            downloadSizeView.visibility = VISIBLE
        }
    }

    private fun showDownloadProgressBar() {
        downloadProgressTable?.visibility = VISIBLE
    }

    private fun showDownloadLink(updateData: UpdateData?) {
        if (updateData != null) {
            updateDownloadLinkView.apply {
                visibility = VISIBLE
                movementMethod = LinkMovementMethod.getInstance()
                text = SpannableString(
                    String.format(getString(R.string.update_information_download_link), updateData.downloadUrl)
                ).apply {
                    setSpan(URLSpan(updateData.downloadUrl), indexOf("\n") + 1, length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun hideDownloadProgressBar() {
        downloadProgressTable.visibility = GONE
    }

    private fun hideRefreshIcons() {
        if (updateInformationRefreshLayout.isRefreshing) {
            updateInformationRefreshLayout.isRefreshing = false
        }

        if (systemIsUpToDateRefreshLayout.isRefreshing) {
            systemIsUpToDateRefreshLayout.isRefreshing = false
        }
    }
    /*
      -------------- UPDATE DOWNLOAD METHODS -------------------
     */
    /**
     * Creates an [UpdateDownloadListener]
     */
    private fun buildDownloadListener(updateData: UpdateData?): UpdateDownloadListener {
        return object : UpdateDownloadListener {
            override fun onInitialStatusUpdate() {
                if (isAdded) {
                    updateInformationDownloadCancelButton.setOnClickListener {
                        DownloadService.performOperation(activity, DownloadService.ACTION_CANCEL_DOWNLOAD, updateData)

                        initUpdateDownloadButton(updateData, NOT_DOWNLOADING)
                        initInstallButton(updateData, NOT_DOWNLOADING)
                    }

                    updateInformationDownloadPauseButton.setOnClickListener {
                        updateInformationDownloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.play, null))

                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)

                        initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                    }
                }
            }

            override fun onDownloadStarted() {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DOWNLOADING)
                    initInstallButton(updateData, DOWNLOADING)
                    showDownloadProgressBar()

                    updateInformationDownloadPauseButton.setOnClickListener {
                        updateInformationDownloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.pause, null))

                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)

                        initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)

                        // Prevents sending duplicate Intents, will be automatically overridden in onDownloadPaused().
                        updateInformationDownloadPauseButton.setOnClickListener { }
                    }
                }
            }

            override fun onDownloadProgressUpdate(downloadProgressData: DownloadProgressData) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DOWNLOADING)
                    initInstallButton(updateData, DOWNLOADING)
                    showDownloadProgressBar()

                    updateInformationDownloadProgressBar.isIndeterminate = false
                    updateInformationDownloadProgressBar.progress = downloadProgressData.progress

                    if (downloadProgressData.isWaitingForConnection) {
                        updateInformationDownloadPauseButton.visibility = GONE
                        updateInformationDownloadDetailsView.text = getString(
                            R.string.download_waiting_for_network, downloadProgressData
                                .progress
                        )
                        return
                    }

                    updateInformationDownloadPauseButton.visibility = VISIBLE
                    updateInformationDownloadPauseButton.setOnClickListener {
                        updateInformationDownloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.pause, null))

                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)

                        initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)

                        // Prevents sending duplicate Intents, will be automatically overridden in onDownloadPaused().
                        updateInformationDownloadPauseButton.setOnClickListener { }
                    }

                    if (downloadProgressData.timeRemaining == null) {
                        updateInformationDownloadDetailsView.text = getString(
                            R.string.download_progress_text_unknown_time_remaining, downloadProgressData
                                .progress
                        )
                    } else {
                        updateInformationDownloadDetailsView.text = downloadProgressData.timeRemaining.toString(applicationData)
                    }
                }
            }

            override fun onDownloadPaused(queued: Boolean, downloadProgressData: DownloadProgressData) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                    initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                    showDownloadProgressBar()

                    if (downloadProgressData.isWaitingForConnection) {
                        onDownloadProgressUpdate(downloadProgressData)
                        return
                    }

                    if (!queued) {
                        updateInformationDownloadProgressBar.progress = downloadProgressData.progress
                        updateInformationDownloadDetailsView.text = getString(
                            R.string.download_progress_text_paused, downloadProgressData
                                .progress
                        )

                        updateInformationDownloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.play, null))

                        updateInformationDownloadPauseButton.setOnClickListener {
                            updateInformationDownloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.pause, null))

                            DownloadService.performOperation(activity, DownloadService.ACTION_RESUME_DOWNLOAD, updateData)

                            initUpdateDownloadButton(updateData, DOWNLOADING)
                            initInstallButton(updateData, DOWNLOADING)

                            // No resuming twice allowed, will be updated in onDownloadProgressUpdate()
                            updateInformationDownloadPauseButton.setOnClickListener { }
                        }
                    } else {
                        updateInformationDownloadDetailsView.text = getString(R.string.download_pending)
                    }
                }
            }

            override fun onDownloadComplete() {
                if (isAdded) {
                    Toast.makeText(applicationData, getString(R.string.download_verifying_start), LENGTH_LONG).show()
                }
            }

            override fun onDownloadCancelled() {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, NOT_DOWNLOADING)
                    initInstallButton(updateData, NOT_DOWNLOADING)
                    hideDownloadProgressBar()
                }
            }

            override fun onDownloadError(isInternalError: Boolean, isStorageSpaceError: Boolean, isServerError: Boolean) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, NOT_DOWNLOADING)
                    initInstallButton(updateData, NOT_DOWNLOADING)
                    hideDownloadProgressBar()

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
                if (isAdded) {
                    initUpdateDownloadButton(updateData, VERIFYING)
                    initInstallButton(updateData, VERIFYING)
                    showDownloadProgressBar()

                    updateInformationDownloadProgressBar.isIndeterminate = true
                    updateInformationDownloadDetailsView.text = getString(R.string.download_progress_text_verifying)
                }
            }

            override fun onVerifyError() {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, NOT_DOWNLOADING)
                    initInstallButton(updateData, NOT_DOWNLOADING)
                    hideDownloadProgressBar()
                    showDownloadError(updateData, R.string.download_error_corrupt)
                }
            }

            override fun onVerifyComplete(launchInstallation: Boolean) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DOWNLOAD_COMPLETED)
                    initInstallButton(updateData, DOWNLOAD_COMPLETED)
                    hideDownloadProgressBar()

                    if (launchInstallation) {
                        Toast.makeText(applicationData, getString(R.string.download_complete), LENGTH_LONG).show()

                        ActivityLauncher(activity!!).UpdateInstallation(true, updateData)
                    }
                }
            }
        }
    }

    private fun showDownloadError(updateData: UpdateData?, @StringRes message: Int) {
        showDownloadError(this, updateData, false, R.string.download_error, message)
    }

    private fun initUpdateDownloadButton(updateData: UpdateData?, downloadStatus: DownloadStatus) {
        updateInformationDownloadButton.let {
            when (downloadStatus) {
                NOT_DOWNLOADING -> {
                    it.text = getString(R.string.download)

                    if (Utils.checkNetworkConnection(applicationData) && updateData?.downloadUrl?.contains("http") == true) {
                        it.isEnabled = true
                        it.isClickable = true
                        it.setOnClickListener(DownloadButtonOnClickListener(updateData))
                        it.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                    } else {
                        it.isEnabled = false
                        it.isClickable = false
                        it.setTextColor(ContextCompat.getColor(mContext, R.color.dark_grey))
                    }

                    if (updateData == null) {
                        load()
                    }
                }
                DownloadStatus.DOWNLOAD_QUEUED, DOWNLOADING -> {
                    it.text = getString(R.string.downloading)
                    it.isEnabled = true
                    it.isClickable = false
                    it.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                }
                DownloadStatus.DOWNLOAD_PAUSED -> {
                    it.text = getString(R.string.paused)
                    it.isEnabled = true
                    it.isClickable = false
                    it.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                }
                DOWNLOAD_COMPLETED -> {
                    it.text = getString(R.string.downloaded)
                    it.isEnabled = true
                    it.isClickable = true
                    it.setOnClickListener(AlreadyDownloadedOnClickListener(this, updateData))
                    it.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                }
                VERIFYING -> {
                    it.text = getString(R.string.download_verifying)
                    it.isEnabled = true
                    it.isClickable = false
                    it.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary))
                }
                DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION -> {

                }
            }
        }
    }

    private fun initInstallButton(updateData: UpdateData?, downloadStatus: DownloadStatus) {
        updateInstallationInstructionsButton.apply {
            if (downloadStatus !== DOWNLOAD_COMPLETED) {
                visibility = GONE
            } else {
                if (activity == null) {
                    return
                }

                visibility = VISIBLE
                setOnClickListener {
                    (activity as MainActivity).activityLauncher.UpdateInstallation(true, updateData)

                    hideDownloadCompleteNotification(activity)
                }
            }
        }
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
    private inner class DownloadButtonOnClickListener internal constructor(private val updateData: UpdateData) : View.OnClickListener {
        override fun onClick(v: View) {
            if (isAdded) {
                val mainActivity = activity as MainActivity? ?: return

                if (mainActivity.hasDownloadPermissions()) {
                    DownloadService.performOperation(activity, ACTION_DOWNLOAD_UPDATE, updateData)

                    initUpdateDownloadButton(updateData, DOWNLOADING)
                    showDownloadProgressBar()

                    updateInformationDownloadProgressBar.isIndeterminate = true
                    updateInformationDownloadDetailsView.text = getString(R.string.download_pending)

                    // Pause is possible on first progress update
                    updateInformationDownloadPauseButton.setOnClickListener { }
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
    private inner class AlreadyDownloadedOnClickListener internal constructor(private val targetFragment: Fragment, private val updateData: UpdateData?) : View.OnClickListener {
        override fun onClick(v: View) {
            showUpdateAlreadyDownloadedMessage(updateData, targetFragment) {
                if (updateData != null) {
                    DownloadService.performOperation(activity, ACTION_DELETE_DOWNLOADED_UPDATE, updateData)

                    initUpdateDownloadButton(updateData, NOT_DOWNLOADING)
                    initInstallButton(updateData, NOT_DOWNLOADING)
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
