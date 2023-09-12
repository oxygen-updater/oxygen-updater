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
import com.oxygenupdater.ui.onboarding.NOT_SET_L
import com.oxygenupdater.utils.performServerRequest

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class ServerRepository constructor(
    private val serverApi: ServerApi,
    localAppDb: LocalAppDb,
) {

    private val newsItemDao by lazy(LazyThreadSafetyMode.NONE) {
        localAppDb.newsItemDao()
    }

    private val updateDataDao by lazy(LazyThreadSafetyMode.NONE) {
        localAppDb.updateDataDao()
    }

    suspend fun fetchFaq() = performServerRequest { serverApi.fetchFaq() }

    suspend fun fetchInstallGuide() = performServerRequest { serverApi.fetchInstallGuide() }

    suspend fun fetchDevices(filter: DeviceRequestFilter) = performServerRequest {
        serverApi.fetchDevices(filter.value)
    }

    val updateDataFlow
        get() = updateDataDao.getFlow()

    suspend fun fetchUpdateData(deviceId: Long, methodId: Long) = performServerRequest {
        serverApi.fetchUpdateData(
            deviceId,
            methodId,
            SystemVersionProperties.oxygenOSOTAVersion,
            SystemVersionProperties.oxygenOSVersion,
            SystemVersionProperties.osType,
            SystemVersionProperties.fingerprint,
            PrefManager.getBoolean(PrefManager.PROPERTY_IS_EU_BUILD, false),
            BuildConfig.VERSION_NAME
        )
    }.let {
        if (it?.shouldFetchMostRecent == true) performServerRequest {
            serverApi.fetchMostRecentUpdateData(deviceId, methodId)
        } else it
    }.let {
        updateDataDao.refresh(it)
    }

    suspend fun fetchServerStatus() = performServerRequest { serverApi.fetchServerStatus() }.let { status ->
        val automaticInstallationEnabled = false
        val pushNotificationsDelaySeconds = PrefManager.getInt(
            PrefManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS,
            10
        )

        val response = if (status == null && OxygenUpdater.isNetworkAvailable.value) ServerStatus(
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

        response
    }

    suspend fun fetchServerMessages() = performServerRequest {
        serverApi.fetchServerMessages(
            PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, NOT_SET_L),
            PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, NOT_SET_L)
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun fetchNewsFromDb() = newsItemDao.getAll()

    @Suppress("RedundantSuspendModifier")
    suspend fun markAllReadLocally() = newsItemDao.markAllRead()

    suspend fun fetchNews() = performServerRequest {
        serverApi.fetchNews(
            PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, NOT_SET_L),
            PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, NOT_SET_L)
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
        serverApi.markNewsItemRead(ArrayMap<String, Long>(1).apply { put("news_item_id", newsItemId) })
    }

    suspend fun fetchUpdateMethodsForDevice(deviceId: Long) = performServerRequest {
        serverApi.fetchUpdateMethodsForDevice(deviceId)
    }

    suspend fun fetchAllMethods() = performServerRequest {
        serverApi.fetchAllUpdateMethods()
    }

    suspend fun submitOtaDbRows(rows: List<Map<String, Any?>>) = performServerRequest {
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
            ArrayMap<String, Any?>(9).apply {
                put("url", url)
                put("filename", filename)
                put("version", version)
                put("otaVersion", otaVersion)
                put("httpCode", httpCode)
                put("httpMessage", httpMessage)
                put("appVersion", BuildConfig.VERSION_NAME)
                put("deviceName", PrefManager.getString(PrefManager.PROPERTY_DEVICE, "<UNKNOWN>"))
                put("actualDeviceName", SystemVersionProperties.oxygenDeviceName)
            }
        )
    }

    suspend fun verifyPurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType,
    ) = performServerRequest {
        serverApi.verifyPurchase(
            ArrayMap<String, Any?>(13).apply {
                put("orderId", purchase.orderId)
                put("packageName", purchase.packageName)
                put("productId", purchase.products.joinToString(","))
                put("purchaseTime", purchase.purchaseTime)
                put("purchaseState", purchase.purchaseState)
                put("developerPayload", purchase.developerPayload)
                put("token", purchase.purchaseToken)
                put("purchaseToken", purchase.purchaseToken)
                put("autoRenewing", purchase.isAutoRenewing)
                put("purchaseType", purchaseType.name)
                put("itemType", purchaseType.type)
                put("signature", purchase.signature)
                put("amount", amount)
            }
        )
    }
}
