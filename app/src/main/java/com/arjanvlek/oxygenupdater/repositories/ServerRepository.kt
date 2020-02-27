package com.arjanvlek.oxygenupdater.repositories

import android.content.Context
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.apis.ServerApi
import com.arjanvlek.oxygenupdater.database.NewsDatabaseHelper
import com.arjanvlek.oxygenupdater.enums.PurchaseType
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.iab.Purchase
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.utils.Logger
import com.arjanvlek.oxygenupdater.utils.Utils
import com.arjanvlek.oxygenupdater.utils.apiResponse
import com.fasterxml.jackson.core.JsonProcessingException
import org.joda.time.LocalDateTime
import org.json.JSONException

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Suppress("unused")
class ServerRepository constructor(
    private val serverApi: ServerApi,
    private val settingsManager: SettingsManager,
    private val newsDatabaseHelper: NewsDatabaseHelper
) {

    private val devicesCache = HashMap<DeviceRequestFilter, Pair<LocalDateTime, List<Device>>>()

    private var serverStatus: ServerStatus? = null

    suspend fun fetchDevices(
        filter: DeviceRequestFilter,
        alwaysFetch: Boolean = false
    ): List<Device> {
        val cachePreCondition = devicesCache[filter]
            ?.first
            ?.plusMinutes(5)
            ?.isAfter(LocalDateTime.now()) == true

        return if (cachePreCondition && !alwaysFetch) {
            Logger.logVerbose(TAG, "Used in-memory cache to fetch devices")

            devicesCache[filter]!!.second
        } else {
            apiResponse(serverApi.fetchDevices(filter.filter)).also {
                devicesCache[filter] = Pair(LocalDateTime.now(), it.toList())
            }
        }
    }

    suspend fun fetchUpdateData(
        online: Boolean,
        deviceId: Long,
        updateMethodId: Long,
        incrementalSystemVersion: String,
        errorCallback: KotlinCallback<String?>
    ) = apiResponse(
        serverApi.fetchUpdateData(deviceId, updateMethodId, incrementalSystemVersion)
    ).let { updateData: UpdateData? ->
        if (updateData?.information != null
            && updateData.information == OxygenUpdater.UNABLE_TO_FIND_A_MORE_RECENT_BUILD
            && updateData.isUpdateInformationAvailable
            && updateData.systemIsUpToDate
        ) {
            fetchMostRecentUpdateData(deviceId, updateMethodId)
        } else if (!online) {
            if (settingsManager.checkIfOfflineUpdateDataIsAvailable()) {
                UpdateData(
                    id = settingsManager.getPreference<Long?>(SettingsManager.PROPERTY_OFFLINE_ID, null),
                    versionNumber = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME, null),
                    description = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION, null),
                    downloadUrl = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_DOWNLOAD_URL, null),
                    downloadSize = settingsManager.getPreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, 0L),
                    filename = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_FILE_NAME, null),
                    updateInformationAvailable = settingsManager.getPreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, false),
                    systemIsUpToDate = settingsManager.getPreference(SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE, false)
                )
            } else {
                errorCallback.invoke(OxygenUpdater.NETWORK_CONNECTION_ERROR)
                null
            }
        } else {
            updateData
        }
    }

    private suspend fun fetchMostRecentUpdateData(
        deviceId: Long,
        updateMethodId: Long
    ): UpdateData = apiResponse(
        serverApi.fetchMostRecentUpdateData(deviceId, updateMethodId)
    )

    suspend fun fetchServerStatus(online: Boolean) = serverStatus ?: apiResponse(
        serverApi.fetchServerStatus()
    ).let { status: ServerStatus? ->
        val automaticInstallationEnabled = settingsManager.getPreference(SettingsManager.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, false)
        val pushNotificationsDelaySeconds = settingsManager.getPreference(SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, 1800)

        val response = if (status == null && online) {
            ServerStatus(
                ServerStatus.Status.UNREACHABLE,
                BuildConfig.VERSION_NAME,
                automaticInstallationEnabled,
                pushNotificationsDelaySeconds
            )
        } else {
            status ?: ServerStatus(
                ServerStatus.Status.NORMAL,
                BuildConfig.VERSION_NAME,
                automaticInstallationEnabled,
                pushNotificationsDelaySeconds
            )
        }

        settingsManager.savePreference(SettingsManager.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, response.automaticInstallationEnabled)
        settingsManager.savePreference(SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, response.pushNotificationDelaySeconds)

        response.also { serverStatus = it }
    }

    suspend fun fetchServerMessages(
        serverStatus: ServerStatus,
        errorCallback: KotlinCallback<String?>
    ) = apiResponse(
        serverApi.fetchServerMessages(
            settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L),
            settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        )
    ).let { serverMessages ->
        val status = serverStatus.status!!
        if (status.isNonRecoverableError) {
            when (status) {
                ServerStatus.Status.MAINTENANCE -> errorCallback.invoke(OxygenUpdater.SERVER_MAINTENANCE_ERROR)
                ServerStatus.Status.OUTDATED -> errorCallback.invoke(OxygenUpdater.APP_OUTDATED_ERROR)
                else -> {
                    // no-op
                }
            }
        }

        val showServerMessages = settingsManager.getPreference(SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES, true)
        if (showServerMessages) {
            serverMessages
        } else {
            ArrayList()
        }
    }

    suspend fun fetchNews(
        context: Context,
        deviceId: Long,
        updateMethodId: Long
    ) = apiResponse(
        serverApi.fetchNews(deviceId, updateMethodId)
    ).let {
        newsDatabaseHelper.let { databaseHelper ->
            if (!it.isNullOrEmpty() && Utils.checkNetworkConnection(context)) {
                databaseHelper.saveNewsItems(it)
            }

            databaseHelper.allNewsItems.also { databaseHelper.close() }
        }
    }

    suspend fun fetchNewsItem(
        context: Context,
        newsItemId: Long
    ) = apiResponse(
        serverApi.fetchNewsItem(newsItemId)
    ).let { newsItem: NewsItem? ->
        newsDatabaseHelper.let { databaseHelper ->
            if (newsItem != null && Utils.checkNetworkConnection(context)) {
                databaseHelper.saveNewsItem(newsItem)
            }

            databaseHelper.getNewsItem(newsItemId).also { databaseHelper.close() }
        }
    }

    suspend fun markNewsItemRead(
        newsItemId: Long
    ): ServerPostResult = apiResponse(
        serverApi.markNewsItemRead(mapOf("news_item_id" to newsItemId))
    )

    suspend fun fetchUpdateMethodsForDevice(
        deviceId: Long,
        hasRootAccess: Boolean
    ) = apiResponse(
        serverApi.fetchUpdateMethodsForDevice(deviceId)
    ).let { updateMethods ->
        if (hasRootAccess) {
            updateMethods.filter { it.supportsRootedDevice }.map { it.setRecommended(if (it.recommendedForNonRootedDevice) "1" else "0") }
        } else {
            updateMethods.map { it.setRecommended(if (it.recommendedForNonRootedDevice) "1" else "0") }
        }
    }

    suspend fun fetchAllMethods(): List<UpdateMethod> = apiResponse(
        serverApi.fetchAllUpdateMethods()
    )

    suspend fun fetchInstallGuidePage(
        deviceId: Long,
        updateMethodId: Long,
        pageNumber: Int
    ): InstallGuidePage = apiResponse(serverApi.fetchInstallGuidePage(deviceId, updateMethodId, pageNumber))

    suspend fun submitUpdateFile(
        filename: String
    ): ServerPostResult = apiResponse(
        serverApi.submitUpdateFile(mapOf("filename" to filename))
    )

    suspend fun logRootInstall(rootInstall: RootInstall) = try {
        apiResponse(serverApi.logRootInstall(rootInstall))
    } catch (e: JSONException) {
        ServerPostResult(
            false,
            "IN-APP ERROR (ServerConnector): Json parse error on input data $rootInstall"
        )
    } catch (e: JsonProcessingException) {
        ServerPostResult(
            false,
            "IN-APP ERROR (ServerConnector): Json parse error on input data $rootInstall"
        )
    }

    suspend fun verifyPurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType
    ): ServerPostResult = apiResponse(
        serverApi.verifyPurchase(
            mapOf(
                "orderId" to purchase.orderId,
                "packageName" to purchase.packageName,
                "productId" to purchase.sku,
                "purchaseTime" to purchase.purchaseTime,
                "purchaseState" to purchase.purchaseState,
                "developerPayload" to purchase.developerPayload,
                "token" to purchase.token,
                "purchaseToken" to purchase.token,
                "autoRenewing" to purchase.isAutoRenewing,
                "purchaseType" to purchaseType,
                "itemType" to purchase.itemType,
                "signature" to purchase.signature,
                "amount" to amount
            )
        )
    )

    companion object {
        private const val TAG = "ServerRepository"
    }
}
