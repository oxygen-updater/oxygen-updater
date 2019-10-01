package com.arjanvlek.oxygenupdater.updateinformation


import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.RelativeLayout.ABOVE
import android.widget.RelativeLayout.BELOW
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.APP_OUTDATED_ERROR
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NETWORK_CONNECTION_ERROR
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NO_OXYGEN_OS
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.SERVER_MAINTENANCE_ERROR
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.download.DownloadProgressData
import com.arjanvlek.oxygenupdater.download.DownloadReceiver
import com.arjanvlek.oxygenupdater.download.DownloadService
import com.arjanvlek.oxygenupdater.download.DownloadStatus
import com.arjanvlek.oxygenupdater.download.UpdateDownloadListener
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.notifications.Dialogs
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications
import com.arjanvlek.oxygenupdater.settings.SettingsActivity.Companion.SKU_AD_FREE
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_AD_FREE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_DOWNLOAD_URL
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_FILE_NAME
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_IS_UP_TO_DATE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_DESCRIPTION
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_NAME
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_CHECKED_DATE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD_ID
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabHelper
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabResult
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Inventory
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK1
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK2
import com.arjanvlek.oxygenupdater.versionformatter.UpdateDataVersionFormatter
import com.arjanvlek.oxygenupdater.views.AbstractFragment
import com.arjanvlek.oxygenupdater.views.MainActivity
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.AdView
import java8.util.Objects
import java8.util.function.Consumer
import java8.util.stream.StreamSupport.stream
import org.joda.time.LocalDateTime
import java.util.*

class UpdateInformationFragment : AbstractFragment() {
    private var updateInformationRefreshLayout: SwipeRefreshLayout? = null
    private var systemIsUpToDateRefreshLayout: SwipeRefreshLayout? = null
    private var rootView: RelativeLayout? = null
    private var adView: AdView? = null
    private var contextVar: Context? = null
    private var mSettingsManager: SettingsManager? = null
    private var downloadListener: UpdateDownloadListener? = null
    private var updateData: UpdateData? = null
    private var isLoadedOnce: Boolean = false
    private var adsAreSupported = false
    private var serverMessageBars: MutableList<ServerMessageBar> = ArrayList()
    private var downloadReceiver: DownloadReceiver? = null
    /*
      -------------- USER INTERFACE ELEMENT METHODS -------------------
     */

    private val downloadButton: Button
        get() = rootView!!.findViewById<View>(R.id.updateInformationDownloadButton) as Button

    private val downloadCancelButton: ImageButton
        get() = rootView!!.findViewById<View>(R.id.updateInformationDownloadCancelButton) as ImageButton

    private val downloadPauseButton: ImageButton
        get() = rootView!!.findViewById(R.id.updateInformationDownloadPauseButton)


    private val downloadStatusText: TextView
        get() = rootView!!.findViewById<View>(R.id.updateInformationDownloadDetailsView) as TextView


    private val downloadProgressBar: ProgressBar
        get() = rootView!!.findViewById<View>(R.id.updateInformationDownloadProgressBar) as ProgressBar

    private val isDownloadServiceRunning: Boolean
        get() {
            if (activity == null) {
                return false
            }

            val manager = activity!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            for (service in manager.getRunningServices(5)) {
                if (DownloadService::class.java.name == service.service.className) {
                    return true
                }
            }

            return false
        }

    /*
      -------------- ANDROID ACTIVITY LIFECYCLE METHODS -------------------
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contextVar = getApplicationData()
        mSettingsManager = SettingsManager(contextVar)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        rootView = inflater.inflate(R.layout.fragment_update_information, container, false) as RelativeLayout
        adView = rootView!!.findViewById(R.id.updateInformationAdView)
        return rootView
    }

    override fun onStart() {
        super.onStart()
        if (isAdded && mSettingsManager!!.checkIfSetupScreenHasBeenCompleted()) {
            updateInformationRefreshLayout = rootView!!.findViewById(R.id.updateInformationRefreshLayout)
            systemIsUpToDateRefreshLayout = rootView!!.findViewById(R.id.updateInformationSystemIsUpToDateRefreshLayout)

            updateInformationRefreshLayout!!.setOnRefreshListener { load(adsAreSupported) }
            updateInformationRefreshLayout!!.setColorSchemeResources(R.color.colorPrimary)

            systemIsUpToDateRefreshLayout!!.setOnRefreshListener { load(adsAreSupported) }
            systemIsUpToDateRefreshLayout!!.setColorSchemeResources(R.color.colorPrimary)

            checkAdSupportStatus(Consumer { adsAreSupported ->
                this.adsAreSupported = adsAreSupported!!

                load(adsAreSupported)

                if (adsAreSupported) {
                    showAds()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (isLoadedOnce) {
            registerDownloadReceiver(downloadListener)
            // If service reports being inactive, check if download is finished or paused to update state.
            // If download is running then it auto-updates the UI using the downloadListener.
            if (!DownloadService.isRunning && updateData != null) {
                DownloadService.performOperation(activity, DownloadService.ACTION_GET_INITIAL_STATUS, updateData!!)
            }
        }
        if (adView != null) {
            adView!!.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (downloadReceiver != null) {
            unregisterDownloadReceiver()
        }
        if (adView != null) {
            adView!!.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (downloadReceiver != null) {
            unregisterDownloadReceiver()
        }
        if (activity != null && isDownloadServiceRunning) {
            activity!!.stopService(Intent(getContext(), DownloadService::class.java))
        }
        if (adView != null) {
            adView!!.destroy()
        }
    }

    /*
      -------------- INITIALIZATION / DATA FETCHING METHODS -------------------
     */

    private fun checkAdSupportStatus(callback: Consumer<Boolean>) {
        if (activity == null) {
            callback.accept(false)
            return
        }

        val helper = IabHelper(activity!!, PK1.A + "/" + PK2.B)

        helper.startSetup(object : IabHelper.OnIabSetupFinishedListener {
            override fun onIabSetupFinished(result: IabResult) {
                if (!result.isSuccess) {
                    // Failed to setup IAB, so we might be offline or the device does not support IAB. Return the last stored value of the ad-free status.
                    callback.accept(!mSettingsManager!!.getPreference(PROPERTY_AD_FREE, false))
                    return
                }

                try {
                    helper.queryInventoryAsync(true, listOf(SKU_AD_FREE), null, object : IabHelper.QueryInventoryFinishedListener {
                        override fun onQueryInventoryFinished(result: IabResult, inv: Inventory?) {
                            if (!result.isSuccess) {
                                // Failed to check inventory, so we might be offline. Return the last stored value of the ad-free status.
                                callback.accept(!mSettingsManager!!.getPreference(PROPERTY_AD_FREE, false))
                                return
                            }

                            if (result.isSuccess) {
                                if (inv!!.hasPurchase(SKU_AD_FREE)) {
                                    // User has bought the upgrade. Save this to the app's settings and return that ads may not be shown.
                                    mSettingsManager!!.savePreference(PROPERTY_AD_FREE, true)
                                    callback.accept(false)
                                } else {
                                    // User has not bought the item and we're online, so ads are definitely supported
                                    callback.accept(true)
                                }
                            }
                        }

                    })
                } catch (e: IabHelper.IabAsyncInProgressException) {
                    // A check is already in progress, so wait 3 secs and try to check again.
                    Handler().postDelayed({ checkAdSupportStatus(callback) }, 3000)
                }
            }

        })
    }

    /**
     * Fetches all server data. This includes update information, server messages and server status
     * checks
     */
    private fun load(adsAreSupported: Boolean) {
        Crashlytics.setUserIdentifier("Device: "
                + mSettingsManager!!.getPreference(PROPERTY_DEVICE, "<UNKNOWN>")
                + ", Update Method: "
                + mSettingsManager!!.getPreference(PROPERTY_UPDATE_METHOD, "<UNKNOWN>")
        )

        val instance = this

        val deviceId = mSettingsManager!!.getPreference(PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = mSettingsManager!!.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L)

        val online = Utils.checkNetworkConnection(appData)

        val serverConnector = appData?.getServerConnector()
        val systemVersionProperties = appData?.mSystemVersionProperties

        serverConnector?.getUpdateData(online, deviceId, updateMethodId,
                systemVersionProperties!!.oxygenOSOTAVersion, Consumer { updateData ->
            this.updateData = updateData

            if (!isLoadedOnce) {
                downloadListener = buildDownloadListener(updateData)
                registerDownloadReceiver(downloadListener)
                DownloadService.performOperation(activity, DownloadService.ACTION_GET_INITIAL_STATUS, updateData)
            }

            // If the activity is started with a download error (when clicked on a "download failed" notification), show it to the user.
            if (!isLoadedOnce && activity != null
                    && activity!!.intent != null
                    && activity!!.intent.getBooleanExtra(KEY_HAS_DOWNLOAD_ERROR, false)) {
                val i = activity!!.intent
                Dialogs.showDownloadError(instance, updateData, i.getBooleanExtra(KEY_DOWNLOAD_ERROR_RESUMABLE, false), i
                        .getStringExtra(KEY_DOWNLOAD_ERROR_TITLE), i.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE))
            }

            displayUpdateInformation(updateData, online, false)

            isLoadedOnce = true

        }, Consumer { error ->
            if (error == NETWORK_CONNECTION_ERROR) {
                Dialogs.showNoNetworkConnectionError(instance)
            }
        })

        serverConnector?.getInAppMessages(online, Consumer { banners ->
            displayServerMessageBars(banners, adsAreSupported)
        }, Consumer { error ->
            when (error) {
                SERVER_MAINTENANCE_ERROR -> Dialogs.showServerMaintenanceError(instance)
                APP_OUTDATED_ERROR -> activity?.let { Dialogs.showAppOutdatedError(instance, it) }
            }
        })
    }


    /*
      -------------- METHODS FOR DISPLAYING DATA ON THE FRAGMENT -------------------
     */


    private fun addServerMessageBar(view: ServerMessageBar) {
        // Add the message to the update information screen.
        // Set the layout params based on the view count.
        // First view should go below the app update message bar (if visible)
        // Consecutive views should go below their parent / previous view.
        val params = RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        val numberOfBars = serverMessageBars.size

        // position each bar below the previous one
        if (!serverMessageBars.isEmpty()) {
            params.addRule(BELOW, serverMessageBars[serverMessageBars.size - 1].id)
        }

        view.id = numberOfBars * 20000 + 1
        rootView!!.addView(view, params)
        serverMessageBars.add(view)
    }

    private fun deleteAllServerMessageBars() {
        stream(serverMessageBars).filter { Objects.nonNull(it) }.forEach { v -> rootView!!.removeView(v) }

        serverMessageBars = ArrayList()
    }

    private fun displayServerMessageBars(banners: List<Banner>, adsAreSupported: Boolean) {

        if (!isAdded) {
            return
        }
        deleteAllServerMessageBars()

        val createdServerMessageBars = ArrayList<ServerMessageBar>()

        for (banner in banners) {
            val bar = ServerMessageBar(activity!!)
            val backgroundBar = bar.backgroundBar
            val textView = bar.textView

            backgroundBar.setBackgroundColor(banner.getColor(contextVar!!))
            textView.text = banner.getBannerText(contextVar!!)

            if (banner.getBannerText(contextVar!!) is Spanned) {
                textView.movementMethod = LinkMovementMethod.getInstance()
            }

            addServerMessageBar(bar)
            createdServerMessageBars.add(bar)
        }

        // Position the app UI  to be below the last added server message bar
        if (createdServerMessageBars.isNotEmpty()) {
            val lastServerMessageView = createdServerMessageBars[createdServerMessageBars.size - 1]
            val params = RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            params.addRule(BELOW, lastServerMessageView.id)
            if (adsAreSupported) {
                params.addRule(ABOVE, adView!!.id)
            }

            systemIsUpToDateRefreshLayout!!.layoutParams = params
            updateInformationRefreshLayout!!.layoutParams = params

        }
        serverMessageBars = createdServerMessageBars
    }

    /**
     * Displays the update information from a [UpdateData] with update information.
     *
     * @param updateData              Update information to display
     * @param online                  Whether or not the device has an active network connection
     * @param displayInfoWhenUpToDate Flag set to show update information anyway, even if the system
     * is up to date.
     */
    private fun displayUpdateInformation(updateData: UpdateData?, online: Boolean, displayInfoWhenUpToDate: Boolean) {
        // Abort if no update data is found or if the fragment is not attached to its activity to prevent crashes.
        if (!isAdded || updateData == null) {
            return
        }

        // Hide the loading screen
        rootView!!.findViewById<View>(R.id.updateInformationLoadingScreen).visibility = GONE

        if (updateData.id == null) {
            displayUpdateInformationWhenUpToDate(updateData, online)
        }

        if (updateData.isSystemIsUpToDateCheck(mSettingsManager) && !displayInfoWhenUpToDate || !updateData
                        .isUpdateInformationAvailable) {
            displayUpdateInformationWhenUpToDate(updateData, online)
        } else {
            displayUpdateInformationWhenNotUpToDate(updateData, displayInfoWhenUpToDate)
        }

        if (online) {
            // Save update data for offline viewing
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_ID, updateData.id)
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_UPDATE_NAME, updateData.versionNumber)
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, updateData.downloadSizeInMegabytes)
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION, updateData.description)
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_FILE_NAME, updateData.filename)
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_DOWNLOAD_URL, updateData.downloadUrl)
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, updateData
                    .isUpdateInformationAvailable)
            mSettingsManager!!.savePreference(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now()
                    .toString())
            mSettingsManager!!.savePreference(PROPERTY_OFFLINE_IS_UP_TO_DATE, updateData.isSystemIsUpToDate)
        }

        // Hide the refreshing icon if it is present.
        hideRefreshIcons()
    }

    private fun displayUpdateInformationWhenUpToDate(updateData: UpdateData, online: Boolean) {
        if (activity == null || activity!!.application == null) {
            return
        }

        // Show "System is up to date" view.
        rootView!!.findViewById<View>(R.id.updateInformationRefreshLayout).visibility = GONE
        rootView!!.findViewById<View>(R.id.updateInformationSystemIsUpToDateRefreshLayout).visibility = VISIBLE

        // Set the current Oxygen OS version if available.
        val oxygenOSVersion = (activity!!.application as ApplicationData)
                .mSystemVersionProperties?.oxygenOSVersion
        val versionNumberView = rootView!!.findViewById<TextView>(R.id.updateInformationSystemIsUpToDateVersionTextView)
        if (oxygenOSVersion != NO_OXYGEN_OS) {
            versionNumberView.visibility = VISIBLE
            versionNumberView.text = String.format(getString(R.string.update_information_oxygen_os_version), oxygenOSVersion)
        } else {
            versionNumberView.visibility = GONE
        }

        // Set "No Update Information Is Available" button if needed.
        val updateInformationButton = rootView!!.findViewById<Button>(R.id.updateInformationSystemIsUpToDateStatisticsButton)
        if (!updateData.isUpdateInformationAvailable) {
            updateInformationButton.text = getString(R.string.update_information_no_update_data_available)
            updateInformationButton.isClickable = false
        } else {
            updateInformationButton.text = getString(R.string.update_information_view_update_information)
            updateInformationButton.isClickable = true
            updateInformationButton.setOnClickListener { v -> displayUpdateInformation(updateData, online, true) }
        }

        // Save last time checked if online.
        if (online) {
            mSettingsManager!!.savePreference(PROPERTY_UPDATE_CHECKED_DATE, LocalDateTime.now().toString())
        }

        // Show last time checked.
        val dateCheckedView = rootView!!.findViewById<TextView>(R.id.updateInformationSystemIsUpToDateDateTextView)
        dateCheckedView.text = String.format(getString(R.string.update_information_last_checked_on), Utils
                .formatDateTime(contextVar!!, mSettingsManager!!.getPreference(PROPERTY_UPDATE_CHECKED_DATE, "")))
    }

    private fun displayUpdateInformationWhenNotUpToDate(updateData: UpdateData, displayInfoWhenUpToDate: Boolean) {
        // Show "System update available" view.
        rootView!!.findViewById<View>(R.id.updateInformationRefreshLayout).visibility = VISIBLE
        rootView!!.findViewById<View>(R.id.updateInformationSystemIsUpToDateRefreshLayout).visibility = GONE

        // Display available update version number.
        val buildNumberView = rootView!!.findViewById<TextView>(R.id.updateInformationBuildNumberView)
        if (updateData.versionNumber != null && updateData.versionNumber != "null") {
            if (UpdateDataVersionFormatter.canVersionInfoBeFormatted(updateData)) {
                buildNumberView.text = UpdateDataVersionFormatter.getFormattedVersionNumber(updateData)
            } else {
                buildNumberView.text = updateData.versionNumber
            }
        } else {
            buildNumberView.text = String.format(getString(R.string.update_information_unknown_update_name), mSettingsManager!!.getPreference(PROPERTY_DEVICE, contextVar!!.getString(R.string.device_information_unknown)))
        }

        // Display download size.
        val downloadSizeView = rootView!!.findViewById<TextView>(R.id.updateInformationDownloadSizeView)
        downloadSizeView.text = String.format(getString(R.string.download_size_megabyte), updateData.downloadSizeInMegabytes)

        // Display update description.
        val description = updateData.description
        val descriptionView = rootView!!.findViewById<TextView>(R.id.updateDescriptionView)
        descriptionView.movementMethod = LinkMovementMethod.getInstance()
        descriptionView.text = if (description != null && !description.isEmpty() && description != "null")
            UpdateDescriptionParser
                    .parse(description)
        else
            getString(R.string.update_information_description_not_available)

        // Display update file name.
        val fileNameView = rootView!!.findViewById<TextView>(R.id.updateFileNameView)
        fileNameView.text = String.format(getString(R.string.update_information_file_name), updateData.filename)

        // Format top title based on system version installed.
        val headerLabel = rootView!!.findViewById<TextView>(R.id.headerLabel)
        val updateInstallationGuideButton = rootView!!.findViewById<Button>(R.id.updateInstallationInstructionsButton)
        val downloadSizeTable = rootView!!.findViewById<View>(R.id.buttonTable)
        val downloadSizeImage = rootView!!.findViewById<View>(R.id.downloadSizeImage)

        val downloadButton = downloadButton

        if (displayInfoWhenUpToDate) {
            headerLabel.text = getString(R.string.update_information_installed_update)
            downloadButton.visibility = GONE
            updateInstallationGuideButton.visibility = GONE
            fileNameView.visibility = GONE
            downloadSizeTable.visibility = GONE
            downloadSizeImage.visibility = GONE
            downloadSizeView.visibility = GONE
        } else {
            if (updateData.isSystemIsUpToDate) {
                headerLabel.text = getString(R.string.update_information_installed_update)
            } else {
                headerLabel.text = getString(R.string.update_information_latest_available_update)
            }
            downloadButton.visibility = VISIBLE
            fileNameView.visibility = VISIBLE
            downloadSizeTable.visibility = VISIBLE
            downloadSizeImage.visibility = VISIBLE
            downloadSizeView.visibility = VISIBLE
        }
    }

    private fun showDownloadProgressBar() {
        val downloadProgressBar = rootView!!.findViewById<View>(R.id.downloadProgressTable)
        if (downloadProgressBar != null) {
            downloadProgressBar.visibility = VISIBLE
        }
    }

    private fun hideDownloadProgressBar() {
        rootView!!.findViewById<View>(R.id.downloadProgressTable).visibility = GONE

    }

    private fun hideRefreshIcons() {
        if (updateInformationRefreshLayout != null) {
            if (updateInformationRefreshLayout!!.isRefreshing) {
                updateInformationRefreshLayout!!.isRefreshing = false
            }
        }
        if (systemIsUpToDateRefreshLayout != null) {
            if (systemIsUpToDateRefreshLayout!!.isRefreshing) {
                systemIsUpToDateRefreshLayout!!.isRefreshing = false
            }
        }
    }


    /*
      -------------- GOOGLE ADS METHODS -------------------
     */


    private fun showAds() {
        adView!!.loadAd(ApplicationData.buildAdRequest())
    }

    /*
      -------------- UPDATE DOWNLOAD METHODS -------------------
     */

    /**
     * Creates an [UpdateDownloadListener]
     */
    private fun buildDownloadListener(updateData: UpdateData): UpdateDownloadListener {
        return object : UpdateDownloadListener {
            override fun onInitialStatusUpdate() {
                if (isAdded) {
                    downloadCancelButton.setOnClickListener { v ->
                        DownloadService.performOperation(activity, DownloadService.ACTION_CANCEL_DOWNLOAD, updateData)
                        initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING)
                        initInstallButton(updateData, DownloadStatus.NOT_DOWNLOADING)
                    }

                    downloadPauseButton.setOnClickListener { v ->
                        downloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.play, null))
                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)
                        initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                    }
                }
            }

            override fun onDownloadStarted() {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING)
                    initInstallButton(updateData, DownloadStatus.DOWNLOADING)

                    showDownloadProgressBar()

                    downloadPauseButton.setOnClickListener { v ->
                        downloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.pause, null))
                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)
                        initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        downloadPauseButton.setOnClickListener { vw -> } // Prevents sending duplicate Intents, will be automatically overridden in onDownloadPaused().
                    }
                }
            }

            override fun onDownloadProgressUpdate(downloadProgressData: DownloadProgressData) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING)
                    initInstallButton(updateData, DownloadStatus.DOWNLOADING)

                    showDownloadProgressBar()
                    downloadProgressBar.isIndeterminate = false
                    downloadProgressBar.progress = downloadProgressData.progress

                    if (downloadProgressData.isWaitingForConnection) {
                        downloadPauseButton.visibility = GONE
                        downloadStatusText.text = getString(R.string.download_waiting_for_network, downloadProgressData
                                .progress)
                        return
                    }

                    downloadPauseButton.visibility = VISIBLE
                    downloadPauseButton.setOnClickListener { v ->
                        downloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.pause, null))
                        DownloadService.performOperation(activity, DownloadService.ACTION_PAUSE_DOWNLOAD, updateData)
                        initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                        downloadPauseButton.setOnClickListener { vw -> } // Prevents sending duplicate Intents, will be automatically overridden in onDownloadPaused().
                    }

                    if (downloadProgressData.timeRemaining == null) {
                        downloadStatusText.text = getString(R.string.download_progress_text_unknown_time_remaining, downloadProgressData
                                .progress)
                    } else {
                        downloadStatusText.text = downloadProgressData.timeRemaining!!
                                .toString(appData)
                    }
                }
            }

            override fun onDownloadPaused(queued: Boolean, progressData: DownloadProgressData) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)
                    initInstallButton(updateData, DownloadStatus.DOWNLOAD_PAUSED)

                    showDownloadProgressBar()

                    if (progressData.isWaitingForConnection) {
                        onDownloadProgressUpdate(progressData)
                        return
                    }

                    if (!queued) {
                        downloadProgressBar.progress = progressData.progress
                        downloadStatusText.text = getString(R.string.download_progress_text_paused, progressData
                                .progress)
                        downloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.play, null))
                        downloadPauseButton.setOnClickListener { v ->
                            downloadPauseButton.setImageDrawable(resources.getDrawable(R.drawable.pause, null))
                            DownloadService.performOperation(activity, DownloadService.ACTION_RESUME_DOWNLOAD, updateData)
                            initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING)
                            initInstallButton(updateData, DownloadStatus.DOWNLOADING)
                            downloadPauseButton.setOnClickListener { vw -> } // No resuming twice allowed, will be updated in onDownloadProgressUpdate()
                        }
                    } else {
                        downloadStatusText.text = getString(R.string.download_pending)
                    }
                }
            }

            override fun onDownloadComplete() {
                if (isAdded) {
                    Toast.makeText(appData, getString(R.string.download_verifying_start), LENGTH_LONG)
                            .show()
                }
            }

            override fun onDownloadCancelled() {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING)
                    initInstallButton(updateData, DownloadStatus.NOT_DOWNLOADING)

                    hideDownloadProgressBar()
                }
            }

            override fun onDownloadError(isInternalError: Boolean, isStorageSpaceError: Boolean, isServerError: Boolean) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING)
                    initInstallButton(updateData, DownloadStatus.NOT_DOWNLOADING)

                    hideDownloadProgressBar()

                    if (isServerError) {
                        showDownloadError(updateData, R.string.download_error_server)
                    } else if (isStorageSpaceError) {
                        showDownloadError(updateData, R.string.download_error_storage)
                    } else {
                        showDownloadError(updateData, R.string.download_error_internal)
                    }
                }
            }

            override fun onVerifyStarted() {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.VERIFYING)
                    initInstallButton(updateData, DownloadStatus.VERIFYING)

                    showDownloadProgressBar()
                    downloadProgressBar.isIndeterminate = true
                    downloadStatusText.text = getString(R.string.download_progress_text_verifying)
                }
            }

            override fun onVerifyError() {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING)
                    initInstallButton(updateData, DownloadStatus.NOT_DOWNLOADING)

                    hideDownloadProgressBar()

                    showDownloadError(updateData, R.string.download_error_corrupt)
                }
            }

            override fun onVerifyComplete(launchInstallation: Boolean) {
                if (isAdded) {
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOAD_COMPLETED)
                    initInstallButton(updateData, DownloadStatus.DOWNLOAD_COMPLETED)

                    hideDownloadProgressBar()

                    if (launchInstallation) {
                        Toast.makeText(appData, getString(R.string.download_complete), LENGTH_LONG)
                                .show()
                        val launcher = ActivityLauncher(activity!!)
                        launcher.UpdateInstallation(true, updateData)
                    }
                }
            }
        }
    }

    private fun showDownloadError(updateData: UpdateData, @StringRes message: Int) {
        Dialogs.showDownloadError(this, updateData, false, R.string.download_error, message)
    }

    private fun initUpdateDownloadButton(updateData: UpdateData?, downloadStatus: DownloadStatus) {
        val downloadButton = downloadButton

        when (downloadStatus) {
            DownloadStatus.NOT_DOWNLOADING -> {
                downloadButton.text = getString(R.string.download)

                if (Utils.checkNetworkConnection(appData) && updateData != null && updateData.downloadUrl != null
                        && updateData.downloadUrl!!.contains("http")) {
                    downloadButton.isEnabled = true
                    downloadButton.isClickable = true
                    downloadButton.setOnClickListener(DownloadButtonOnClickListener(updateData))
                    downloadButton.setTextColor(ContextCompat.getColor(contextVar!!, R.color.colorPrimary))
                } else {
                    downloadButton.isEnabled = false
                    downloadButton.isClickable = false
                    downloadButton.setTextColor(ContextCompat.getColor(contextVar!!, R.color.dark_grey))
                }

                if (updateData == null) {
                    load(adsAreSupported)
                }
            }
            DownloadStatus.DOWNLOAD_QUEUED, DownloadStatus.DOWNLOADING -> {
                downloadButton.text = getString(R.string.downloading)
                downloadButton.isEnabled = true
                downloadButton.isClickable = false
                downloadButton.setTextColor(ContextCompat.getColor(contextVar!!, R.color.colorPrimary))
            }
            DownloadStatus.DOWNLOAD_PAUSED -> {
                downloadButton.text = getString(R.string.paused)
                downloadButton.isEnabled = true
                downloadButton.isClickable = false
                downloadButton.setTextColor(ContextCompat.getColor(contextVar!!, R.color.colorPrimary))
            }
            DownloadStatus.DOWNLOAD_COMPLETED -> {
                downloadButton.text = getString(R.string.downloaded)
                downloadButton.isEnabled = true
                downloadButton.isClickable = true
                downloadButton.setOnClickListener(AlreadyDownloadedOnClickListener(this, updateData))
                downloadButton.setTextColor(ContextCompat.getColor(contextVar!!, R.color.colorPrimary))
            }
            DownloadStatus.VERIFYING -> {
                downloadButton.text = getString(R.string.download_verifying)
                downloadButton.isEnabled = true
                downloadButton.isClickable = false
                downloadButton.setTextColor(ContextCompat.getColor(contextVar!!, R.color.colorPrimary))
            }
        }
    }

    private fun initInstallButton(updateData: UpdateData, downloadStatus: DownloadStatus) {
        val installButton = rootView!!.findViewById<Button>(R.id.updateInstallationInstructionsButton)
        if (downloadStatus != DownloadStatus.DOWNLOAD_COMPLETED) {
            installButton.visibility = GONE
        } else {
            if (activity == null) {
                return
            }

            installButton.visibility = VISIBLE
            installButton.setOnClickListener { v ->
                (activity as MainActivity).activityLauncher!!
                        .UpdateInstallation(true, updateData)
                LocalNotifications.hideDownloadCompleteNotification(activity!!)
            }
        }
    }

    private fun registerDownloadReceiver(downloadListener: UpdateDownloadListener?) {
        val filter = IntentFilter(DownloadReceiver.ACTION_DOWNLOAD_EVENT)
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        downloadReceiver = DownloadReceiver(downloadListener)

        if (activity != null) {
            activity!!.registerReceiver(downloadReceiver, filter)
        }
    }

    private fun unregisterDownloadReceiver() {
        if (activity != null) {
            activity!!.unregisterReceiver(downloadReceiver)
            downloadReceiver = null
        }
    }

    /**
     * Download button click listener. Performs these actions when the button is clicked.
     */
    private inner class DownloadButtonOnClickListener internal constructor(private val updateData: UpdateData) : View.OnClickListener {

        override fun onClick(v: View) {
            if (isAdded) {
                val mainActivity = activity as MainActivity? ?: return

                if (mainActivity.hasDownloadPermissions()) {
                    DownloadService.performOperation(activity, DownloadService.ACTION_DOWNLOAD_UPDATE, updateData)
                    initUpdateDownloadButton(updateData, DownloadStatus.DOWNLOADING)
                    showDownloadProgressBar()
                    downloadProgressBar.isIndeterminate = true
                    downloadStatusText.text = getString(R.string.download_pending)
                    downloadPauseButton.setOnClickListener { } // Pause is possible on first progress update
                } else {
                    mainActivity.requestDownloadPermissions(Consumer { granted ->
                        if (granted!!) {
                            DownloadService.performOperation(activity, DownloadService.ACTION_DOWNLOAD_UPDATE, updateData)
                        }
                    })
                }
            }
        }
    }

    /**
     * Allows an already downloaded update file to be deleted to save storage space.
     */
    private inner class AlreadyDownloadedOnClickListener internal constructor(private val targetFragment: Fragment, private val updateData: UpdateData?) : View.OnClickListener {

        override fun onClick(v: View) {
            Dialogs.showUpdateAlreadyDownloadedMessage(updateData!!, targetFragment, Consumer {
                if (updateData != null) {
                    DownloadService.performOperation(activity, DownloadService.ACTION_DELETE_DOWNLOADED_UPDATE, updateData)
                    initUpdateDownloadButton(updateData, DownloadStatus.NOT_DOWNLOADING)
                    initInstallButton(updateData, DownloadStatus.NOT_DOWNLOADING)
                }
            })
        }
    }

    companion object {
        // In app message bar collections and identifiers.
        val KEY_HAS_DOWNLOAD_ERROR = "has_download_error"
        val KEY_DOWNLOAD_ERROR_TITLE = "download_error_title"
        val KEY_DOWNLOAD_ERROR_MESSAGE = "download_error_message"
        val KEY_DOWNLOAD_ERROR_RESUMABLE = "download_error_resumable"
    }
}
