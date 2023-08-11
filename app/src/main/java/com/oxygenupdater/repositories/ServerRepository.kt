package com.oxygenupdater.repositories

import androidx.collection.ArrayMap
import com.android.billingclient.api.Purchase
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.database.LocalAppDb
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.performServerRequest

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class ServerRepository constructor(
    private val serverApi: ServerApi,
    localAppDb: LocalAppDb,
) {

    private var serverStatus: ServerStatus? = null

    private val newsItemDao by lazy(LazyThreadSafetyMode.NONE) {
        localAppDb.newsItemDao()
    }

    suspend fun fetchFaq() = performServerRequest { serverApi.fetchFaq() }

    suspend fun fetchDevices(filter: DeviceRequestFilter) = performServerRequest {
        serverApi.fetchDevices(filter.value)
    }

    fun fetchUpdateDataFromPrefs() = if (PrefManager.checkIfOfflineUpdateDataIsAvailable()) {
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
    } else null

    suspend fun fetchUpdateData(): UpdateData? {
        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        return performServerRequest {
            serverApi.fetchUpdateData(
                deviceId,
                updateMethodId,
                SystemVersionProperties.oxygenOSOTAVersion,
                SystemVersionProperties.oxygenOSVersion,
                SystemVersionProperties.osType,
                SystemVersionProperties.fingerprint,
                PrefManager.getBoolean(PrefManager.PROPERTY_IS_EU_BUILD, false),
                BuildConfig.VERSION_NAME
            )
        }.let {
            if (it?.information != null
                && it.information == OxygenUpdater.UNABLE_TO_FIND_A_MORE_RECENT_BUILD
                && it.isUpdateInformationAvailable
                && it.systemIsUpToDate
            ) performServerRequest {
                serverApi.fetchMostRecentUpdateData(deviceId, updateMethodId)
            } else if (!Utils.checkNetworkConnection()) fetchUpdateDataFromPrefs() else it
        }
    }

    suspend fun fetchServerStatus(useCache: Boolean = false) = if (useCache && serverStatus != null) {
        serverStatus!!
    } else performServerRequest { serverApi.fetchServerStatus() }.let { status ->
        val automaticInstallationEnabled = false
        val pushNotificationsDelaySeconds = PrefManager.getInt(
            PrefManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS,
            300
        )

        val response = if (status == null && Utils.checkNetworkConnection()) ServerStatus(
            ServerStatus.Status.UNREACHABLE,
            BuildConfig.VERSION_NAME,
            automaticInstallationEnabled,
            pushNotificationsDelaySeconds
        ) else status ?: ServerStatus(
            ServerStatus.Status.NORMAL,
            BuildConfig.VERSION_NAME,
            automaticInstallationEnabled,
            pushNotificationsDelaySeconds
        )

        PrefManager.putInt(
            PrefManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS,
            response.pushNotificationDelaySeconds
        )

        response.also { serverStatus = it }
    }

    suspend fun fetchServerMessages() = performServerRequest {
        serverApi.fetchServerMessages(
            PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L),
            PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun fetchNewsFromDb() = newsItemDao.getAll()

    @Suppress("RedundantSuspendModifier")
    suspend fun markAllReadLocally() = newsItemDao.markAllRead()

    suspend fun fetchNews() = performServerRequest {
        serverApi.fetchNews(
            PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L),
            PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)
        )
    }.let {
        if (!it.isNullOrEmpty()) newsItemDao.refreshNewsItems(it)

        // Note: we're returning a local copy so that read statuses are respected
        newsItemDao.getAll()
    }

    suspend fun fetchNewsItem(newsItemId: Long) = performServerRequest {
        serverApi.fetchNewsItem(newsItemId)
    }.let {
        if (it != null) newsItemDao.insertOrUpdate(it)

        newsItemDao.getById(newsItemId)
    }

    fun toggleNewsItemReadLocally(
        newsItem: NewsItem,
        read: Boolean = !newsItem.readState.value,
    ) = newsItemDao.toggleRead(newsItem, read)

    suspend fun markNewsItemRead(newsItemId: Long) = performServerRequest {
        serverApi.markNewsItemRead(mapOf("news_item_id" to newsItemId))
    }

    suspend fun fetchUpdateMethodsForDevice(deviceId: Long) = performServerRequest {
        serverApi.fetchUpdateMethodsForDevice(deviceId)
    }

    suspend fun fetchAllMethods() = performServerRequest {
        serverApi.fetchAllUpdateMethods()
    }

    suspend fun fetchInstallGuidePage(
        deviceId: Long,
        updateMethodId: Long,
        pageNumber: Int,
    ) = performServerRequest {
        serverApi.fetchInstallGuidePage(deviceId, updateMethodId, pageNumber)
    }

    suspend fun submitOtaDbRows(rows: List<ArrayMap<String, Any?>>) = performServerRequest {
        serverApi.submitOtaDbRows(
            ArrayMap<String, Any>(7).apply {
                put("rows", rows)
                put("fingerprint", SystemVersionProperties.fingerprint)
                put("currentOtaVersion", SystemVersionProperties.oxygenOSOTAVersion)
                put("isEuBuild", PrefManager.getBoolean(PrefManager.PROPERTY_IS_EU_BUILD, false))
                put("appVersion", BuildConfig.VERSION_NAME)
                put("deviceName", PrefManager.getString(PrefManager.PROPERTY_DEVICE, "<UNKNOWN>") ?: "<UNKNOWN>")
                put("actualDeviceName", SystemVersionProperties.oxygenDeviceName)
            }
        )
    }

    suspend fun logDownloadError(
        url: String?,
        filename: String?,
        version: String?,
        otaVersion: String?,
        httpCode: Int,
        httpMessage: String?,
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
                "actualDeviceName" to SystemVersionProperties.oxygenDeviceName
            )
        )
    }

    suspend fun verifyPurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType,
    ) = performServerRequest {
        serverApi.verifyPurchase(
            hashMapOf(
                "orderId" to purchase.orderId,
                "packageName" to purchase.packageName,
                "productId" to purchase.products.joinToString(","),
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
