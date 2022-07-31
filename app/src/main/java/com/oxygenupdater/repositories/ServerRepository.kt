package com.oxygenupdater.repositories

import com.android.billingclient.api.Purchase
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.database.LocalAppDb
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.performServerRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class ServerRepository constructor(
    private val serverApi: ServerApi,
    private val systemVersionProperties: SystemVersionProperties,
    localAppDb: LocalAppDb
) {

    private var serverStatus: ServerStatus? = null

    private val newsItemDao by lazy(LazyThreadSafetyMode.NONE) {
        localAppDb.newsItemDao()
    }

    suspend fun fetchFaq() = performServerRequest { serverApi.fetchFaq() }

    suspend fun fetchDevices(
        filter: DeviceRequestFilter
    ) = performServerRequest { serverApi.fetchDevices(filter.filter) }

    suspend fun fetchUpdateData(
        deviceId: Long,
        updateMethodId: Long,
        incrementalSystemVersion: String
    ) = performServerRequest {
        serverApi.fetchUpdateData(
            deviceId,
            updateMethodId,
            incrementalSystemVersion,
            systemVersionProperties.oxygenOSVersion,
            systemVersionProperties.osType,
            systemVersionProperties.fingerprint,
            PrefManager.getBoolean(PrefManager.PROPERTY_IS_EU_BUILD, false),
            BuildConfig.VERSION_NAME
        )
    }.let { updateData: UpdateData? ->
        if (updateData?.information != null
            && updateData.information == OxygenUpdater.UNABLE_TO_FIND_A_MORE_RECENT_BUILD
            && updateData.isUpdateInformationAvailable
            && updateData.systemIsUpToDate
        ) {
            fetchMostRecentUpdateData(deviceId, updateMethodId)
        } else if (!Utils.checkNetworkConnection()) {
            if (PrefManager.checkIfOfflineUpdateDataIsAvailable()) {
                UpdateData(
                    id = PrefManager.getLong(PrefManager.PROPERTY_OFFLINE_ID, -1L),
                    versionNumber = PrefManager.getString(PrefManager.PROPERTY_OFFLINE_UPDATE_NAME, null),
                    description = PrefManager.getString(PrefManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION, null),
                    downloadUrl = PrefManager.getString(PrefManager.PROPERTY_OFFLINE_DOWNLOAD_URL, null),
                    downloadSize = PrefManager.getLong(PrefManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, 0L),
                    filename = PrefManager.getString(PrefManager.PROPERTY_OFFLINE_FILE_NAME, null),
                    updateInformationAvailable = PrefManager.getBoolean(PrefManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, false),
                    systemIsUpToDate = PrefManager.getBoolean(PrefManager.PROPERTY_OFFLINE_IS_UP_TO_DATE, false)
                )
            } else {
                null
            }
        } else {
            updateData
        }
    }

    private suspend fun fetchMostRecentUpdateData(
        deviceId: Long,
        updateMethodId: Long
    ) = performServerRequest {
        serverApi.fetchMostRecentUpdateData(deviceId, updateMethodId)
    }

    suspend fun fetchServerStatus(
        useCache: Boolean = false
    ) = if (useCache && serverStatus != null) {
        serverStatus!!
    } else {
        performServerRequest { serverApi.fetchServerStatus() }.let { status ->
            val automaticInstallationEnabled = false
            val pushNotificationsDelaySeconds = PrefManager.getInt(
                PrefManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS,
                300
            )

            val response = if (status == null && Utils.checkNetworkConnection()) {
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

            PrefManager.putInt(
                PrefManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS,
                response.pushNotificationDelaySeconds
            )

            response.also { serverStatus = it }
        }
    }

    suspend fun fetchServerMessages(
        serverStatus: ServerStatus,
        errorCallback: KotlinCallback<String?>
    ) = performServerRequest {
        serverApi.fetchServerMessages(
            PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L),
            PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        )
    }.let { serverMessages ->
        val status = serverStatus.status!!
        if (status.isNonRecoverableError) {
            when (status) {
                ServerStatus.Status.MAINTENANCE -> withContext(Dispatchers.Main) {
                    errorCallback.invoke(OxygenUpdater.SERVER_MAINTENANCE_ERROR)
                }
                ServerStatus.Status.OUTDATED -> withContext(Dispatchers.Main) {
                    errorCallback.invoke(OxygenUpdater.APP_OUTDATED_ERROR)
                }
                else -> {
                    // no-op
                }
            }
        }

        serverMessages
    }

    suspend fun fetchNews(
        deviceId: Long,
        updateMethodId: Long
    ) = performServerRequest {
        serverApi.fetchNews(deviceId, updateMethodId)
    }.let {
        if (!it.isNullOrEmpty()) {
            newsItemDao.refreshNewsItems(it)
        }

        newsItemDao.getAll()
    }

    suspend fun fetchNewsItem(
        newsItemId: Long
    ) = performServerRequest {
        serverApi.fetchNewsItem(newsItemId)
    }.let {
        if (it != null) {
            newsItemDao.insertOrUpdate(it)
        }

        newsItemDao.getById(newsItemId)
    }

    fun toggleNewsItemReadStatusLocally(
        newsItem: NewsItem,
        newReadStatus: Boolean = !newsItem.read
    ) = newsItemDao.toggleReadStatus(
        newsItem,
        newReadStatus
    )

    suspend fun markNewsItemRead(
        newsItemId: Long
    ) = performServerRequest {
        serverApi.markNewsItemRead(mapOf("news_item_id" to newsItemId))
    }

    suspend fun fetchUpdateMethodsForDevice(
        deviceId: Long,
    ) = performServerRequest {
        serverApi.fetchUpdateMethodsForDevice(deviceId)
    }.let { updateMethods ->
        updateMethods?.map { it.setRecommended(if (it.recommendedForNonRootedDevice) "1" else "0") }
    }

    suspend fun fetchAllMethods() = performServerRequest {
        serverApi.fetchAllUpdateMethods()
    }

    suspend fun fetchInstallGuidePage(
        deviceId: Long,
        updateMethodId: Long,
        pageNumber: Int
    ) = performServerRequest {
        serverApi.fetchInstallGuidePage(deviceId, updateMethodId, pageNumber)
    }

    suspend fun logDownloadError(
        url: String?,
        filename: String?,
        version: String?,
        otaVersion: String?,
        httpCode: Int,
        httpMessage: String?
    ) = performServerRequest {
        serverApi.logDownloadError(
            hashMapOf(
                "url" to url,
                "filename" to filename,
                "version" to version,
                "otaVersion" to otaVersion,
                "httpCode" to httpCode,
                "httpMessage" to httpMessage,
                "appVersion" to BuildConfig.VERSION_NAME,
                "deviceName" to PrefManager.getString(PrefManager.PROPERTY_DEVICE, "<UNKNOWN>"),
                "actualDeviceName" to systemVersionProperties.oxygenDeviceName
            )
        )
    }

    suspend fun verifyPurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType
    ) = performServerRequest {
        serverApi.verifyPurchase(
            hashMapOf(
                "orderId" to purchase.orderId,
                "packageName" to purchase.packageName,
                "productId" to purchase.skus.joinToString(","),
                "purchaseTime" to purchase.purchaseTime,
                "purchaseState" to purchase.purchaseState,
                "developerPayload" to purchase.developerPayload,
                "token" to purchase.purchaseToken,
                "purchaseToken" to purchase.purchaseToken,
                "autoRenewing" to purchase.isAutoRenewing,
                "purchaseType" to purchaseType.name,
                "itemType" to purchaseType.type,
                "signature" to purchase.signature,
                "amount" to amount
            )
        )
    }
}
