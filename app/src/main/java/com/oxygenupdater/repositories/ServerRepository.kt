package com.oxygenupdater.repositories

import android.content.SharedPreferences
import androidx.collection.ArrayMap
import com.android.billingclient.api.Purchase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.dao.ArticleDao
import com.oxygenupdater.dao.UpdateDataDao
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyNotificationDelayInSeconds
import com.oxygenupdater.internal.settings.KeyUpdateMethodId
import com.oxygenupdater.models.Article
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.logError
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.utils.logVerbose
import com.oxygenupdater.utils.logWarning
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val serverApi: ServerApi,
    private val updateDataDao: UpdateDataDao,
    private val articleDao: ArticleDao,
    private val crashlytics: FirebaseCrashlytics,
) {

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
            SystemVersionProperties.otaVersion,
            SystemVersionProperties.osVersion,
            SystemVersionProperties.osType,
            SystemVersionProperties.fingerprint,
            SystemVersionProperties.isEuBuild,
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
        val pushNotificationsDelaySeconds = sharedPreferences[KeyNotificationDelayInSeconds, 10]

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

        sharedPreferences[KeyNotificationDelayInSeconds] = response.pushNotificationDelaySeconds

        response
    }

    suspend fun fetchServerMessages() = performServerRequest {
        serverApi.fetchServerMessages(
            sharedPreferences[KeyDeviceId, NotSetL],
            sharedPreferences[KeyUpdateMethodId, NotSetL]
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun fetchNewsFromDb() = articleDao.getAll()

    @Suppress("RedundantSuspendModifier")
    suspend fun markAllReadLocally() = articleDao.markAllRead()

    suspend fun fetchNews() = performServerRequest {
        serverApi.fetchNews(
            sharedPreferences[KeyDeviceId, NotSetL],
            sharedPreferences[KeyUpdateMethodId, NotSetL],
        )
    }.let {
        if (!it.isNullOrEmpty()) articleDao.refreshArticles(it)

        // Note: we're returning a local copy so that read statuses are respected
        fetchNewsFromDb()
    }

    suspend fun fetchArticle(id: Long) = performServerRequest {
        serverApi.fetchArticle(id)
    }.let {
        if (it != null) articleDao.insertOrUpdate(it)

        articleDao.getById(id)
    }

    suspend fun toggleArticleReadLocally(
        article: Article,
        read: Boolean = !article.readState,
    ) = articleDao.toggleRead(article, read)

    suspend fun markArticleRead(id: Long) = performServerRequest {
        serverApi.markArticleRead(ArrayMap<String, Long>(1).apply { put("news_item_id", id) })
    }?.also {
        if (!it.success) crashlytics.logWarning(
            TAG,
            "Failed to mark article as read on the server: ${it.errorMessage}"
        )
    }

    suspend fun fetchUpdateMethodsForDevice(deviceId: Long) = performServerRequest {
        serverApi.fetchUpdateMethodsForDevice(deviceId)
    }

    suspend fun osInfoHeartbeat(fromIntentAction: String) = performServerRequest {
        serverApi.osInfoHeartbeat(
            ArrayMap<String, Any>(7).apply {
                put("fromAction", fromIntentAction)
                put("otaVersion", SystemVersionProperties.otaVersion)
                put("osVersion", SystemVersionProperties.osVersion)
                put("osType", SystemVersionProperties.osType)
                put("fingerprint", SystemVersionProperties.fingerprint)
                put("isEuBuild", SystemVersionProperties.isEuBuild)
                put("appVersion", BuildConfig.VERSION_NAME)
            }
        )
    }

    suspend fun submitOtaDbRows(rows: List<Map<String, Any?>>) = performServerRequest {
        serverApi.submitOtaDbRows(
            ArrayMap<String, Any>(7).apply {
                put("rows", rows)
                put("fingerprint", SystemVersionProperties.fingerprint)
                put("currentOtaVersion", SystemVersionProperties.otaVersion)
                put("isEuBuild", SystemVersionProperties.isEuBuild)
                put("appVersion", BuildConfig.VERSION_NAME)
                put("deviceName", sharedPreferences[KeyDevice, "<UNKNOWN>"])
                put("actualDeviceName", SystemVersionProperties.deviceProductName)
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
                put("deviceName", sharedPreferences[KeyDevice, "<UNKNOWN>"])
                put("actualDeviceName", SystemVersionProperties.deviceProductName)
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

    @Suppress("RedundantSuspendModifier")
    private suspend inline fun <R> performServerRequest(block: () -> Response<R>) = try {
        val response = block()
        if (response.isSuccessful) response.body().apply {
            logVerbose(TAG, "Response: $this")
        } else null
    } catch (e: Exception) {
        // Don't log cancellations to crashlytics, we don't care
        if (e is CancellationException) logInfo(TAG, e.message ?: "CancellationException")
        else crashlytics.logError(TAG, "Error performing request", e)
        null
    }

    companion object {
        private const val TAG = "ServerRepository"
    }
}
