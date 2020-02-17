package com.arjanvlek.oxygenupdater.internal.server

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.text.Html
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils.isNetworkError
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.Utils.checkNetworkConnection
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.internal.objectMapper
import com.arjanvlek.oxygenupdater.internal.root.RootAccessChecker
import com.arjanvlek.oxygenupdater.models.Banner
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerMessage
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.news.NewsDatabaseHelper
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseType
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Purchase
import com.fasterxml.jackson.core.JsonProcessingException
import org.joda.time.LocalDateTime
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.*

class ServerConnector(private val settingsManager: SettingsManager?) : Cloneable {

    private val allDevices = ArrayList<Device>()
    private val enabledDevices = ArrayList<Device>()
    private val disabledDevices = ArrayList<Device>()
    private var allDevicesFetchDate: LocalDateTime? = null
    private var enabledDevicesFetchDate: LocalDateTime? = null
    private var disabledDevicesFetchDate: LocalDateTime? = null
    private var serverStatus: ServerStatus? = null

    fun getDevices(filter: DeviceRequestFilter, callback: KotlinCallback<List<Device>>) {
        getDevices(filter, false, callback)
    }

    fun getDevices(
        filter: DeviceRequestFilter,
        alwaysFetch: Boolean,
        callback: KotlinCallback<List<Device>>
    ) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val cachePreCondition: Boolean = when (filter) {
            DeviceRequestFilter.ALL -> allDevicesFetchDate != null && allDevicesFetchDate!!.plusMinutes(5).isAfter(LocalDateTime.now())
            DeviceRequestFilter.ENABLED -> enabledDevicesFetchDate != null && enabledDevicesFetchDate!!.plusMinutes(5).isAfter(LocalDateTime.now())
            DeviceRequestFilter.DISABLED -> disabledDevicesFetchDate != null && disabledDevicesFetchDate!!.plusMinutes(5).isAfter(LocalDateTime.now())
            else -> false
        }

        if (cachePreCondition && !alwaysFetch) {
            logVerbose(TAG, "Used local cache to fetch devices...")

            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            when (filter) {
                DeviceRequestFilter.ALL -> callback.invoke(allDevices)
                DeviceRequestFilter.ENABLED -> callback.invoke(enabledDevices)
                DeviceRequestFilter.DISABLED -> callback.invoke(disabledDevices)
                else -> callback.invoke(ArrayList())
            }
        } else {
            logVerbose(TAG, "Used remote server to fetch devices...")

            CollectionResponseExecutor<Device>(
                ServerRequest.DEVICES,
                {
                    when (filter) {
                        DeviceRequestFilter.ALL -> {
                            allDevicesFetchDate = LocalDateTime.now()
                            allDevices.clear()
                            allDevices.addAll(it)
                        }
                        DeviceRequestFilter.ENABLED -> {
                            enabledDevicesFetchDate = LocalDateTime.now()
                            enabledDevices.clear()
                            enabledDevices.addAll(it)
                        }
                        DeviceRequestFilter.DISABLED -> {
                            disabledDevicesFetchDate = LocalDateTime.now()
                            disabledDevices.clear()
                            disabledDevices.addAll(it)
                        }
                    }

                    callback.invoke(it)
                },
                filter.filter
            ).execute()
        }
    }

    fun getUpdateMethods(deviceId: Long, callback: KotlinCallback<List<UpdateMethod>>) {
        CollectionResponseExecutor<UpdateMethod>(
            ServerRequest.UPDATE_METHODS,
            { updateMethods ->
                RootAccessChecker.checkRootAccess { hasRootAccess ->
                    if (hasRootAccess) {
                        callback.invoke(updateMethods.filter { it.supportsRootedDevice }.map { it.setRecommended(if (it.recommendedForNonRootedDevice) "1" else "0") })
                    } else {
                        callback.invoke(updateMethods.map { it.setRecommended(if (it.recommendedForNonRootedDevice) "1" else "0") })
                    }
                }
            },
            deviceId
        ).execute()
    }

    fun getAllUpdateMethods(callback: KotlinCallback<List<UpdateMethod>>) {
        CollectionResponseExecutor(ServerRequest.ALL_UPDATE_METHODS, callback).execute()
    }

    fun getUpdateData(
        online: Boolean,
        deviceId: Long,
        updateMethodId: Long,
        incrementalSystemVersion: String,
        callback: KotlinCallback<UpdateData?>,
        errorFunction: KotlinCallback<String?>
    ) {
        ObjectResponseExecutor<UpdateData?>(
            ServerRequest.UPDATE_DATA,
            {
                if (it?.information != null
                    && it.information == ApplicationData.UNABLE_TO_FIND_A_MORE_RECENT_BUILD
                    && it.isUpdateInformationAvailable
                    && it.systemIsUpToDate
                ) {
                    getMostRecentOxygenOTAUpdate(deviceId, updateMethodId, callback)
                } else if (!online) {
                    if (settingsManager!!.checkIfOfflineUpdateDataIsAvailable()) {
                        val updateData = UpdateData(
                            id = settingsManager.getPreference<Long?>(SettingsManager.PROPERTY_OFFLINE_ID, null),
                            versionNumber = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME, null),
                            description = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION, null),
                            downloadUrl = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_DOWNLOAD_URL, null),
                            downloadSize = settingsManager.getPreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, 0L),
                            filename = settingsManager.getPreference<String?>(SettingsManager.PROPERTY_OFFLINE_FILE_NAME, null),
                            updateInformationAvailable = settingsManager.getPreference(SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, false),
                            systemIsUpToDate = settingsManager.getPreference(SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE, false)
                        )

                        callback.invoke(updateData)
                    } else {
                        errorFunction.invoke(ApplicationData.NETWORK_CONNECTION_ERROR)
                    }
                } else {
                    callback.invoke(it)
                }
            },
            deviceId,
            updateMethodId,
            incrementalSystemVersion
        ).execute()
    }

    fun getInAppMessages(
        online: Boolean,
        callback: KotlinCallback<List<Banner>>,
        errorCallback: KotlinCallback<String?>
    ) {
        val inAppBars = ArrayList<Banner>()

        getServerStatus(online) { serverStatus ->
            getServerMessages(
                settingsManager!!.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L),
                settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)
            ) { serverMessages: List<ServerMessage>? ->
                // Add the "No connection" bar depending on the network status of the device.
                if (!online) {
                    inAppBars.add(object : Banner {
                        override fun getBannerText(context: Context) = context.getString(R.string.error_no_internet_connection)

                        override fun getColor(context: Context) = ContextCompat.getColor(context, R.color.colorError)

                        override fun getDrawableRes(context: Context) = R.drawable.error_outline
                    })
                }

                if (serverMessages != null && settingsManager.getPreference(SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES, true)) {
                    inAppBars.addAll(serverMessages)
                }

                val status = serverStatus.status
                if (status!!.isUserRecoverableError) {
                    inAppBars.add(serverStatus)
                }

                if (status.isNonRecoverableError) {
                    when (status) {
                        ServerStatus.Status.MAINTENANCE -> errorCallback.invoke(ApplicationData.SERVER_MAINTENANCE_ERROR)
                        ServerStatus.Status.OUTDATED -> errorCallback.invoke(ApplicationData.APP_OUTDATED_ERROR)
                        else -> {
                            // no-op
                        }
                    }
                }

                if (settingsManager.getPreference(SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !serverStatus.checkIfAppIsUpToDate()) {
                    inAppBars.add(object : Banner {
                        override fun getBannerText(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Html.fromHtml(String.format(context.getString(R.string.new_app_version), serverStatus.latestAppVersion), Html.FROM_HTML_MODE_LEGACY)
                        } else {
                            @Suppress("DEPRECATION")
                            Html.fromHtml(String.format(context.getString(R.string.new_app_version), serverStatus.latestAppVersion))
                        }

                        override fun getColor(context: Context) = ContextCompat.getColor(context, R.color.colorPositive)

                        override fun getDrawableRes(context: Context) = R.drawable.info
                    })
                }

                callback.invoke(inAppBars)
            }
        }
    }

    fun getInstallGuidePage(deviceId: Long, updateMethodId: Long, pageNumber: Int, callback: KotlinCallback<InstallGuidePage>?) {
        ObjectResponseExecutor(
            ServerRequest.INSTALL_GUIDE_PAGE,
            callback,
            deviceId,
            updateMethodId,
            pageNumber
        ).execute()
    }

    fun submitUpdateFile(filename: String, callback: KotlinCallback<ServerPostResult?>) {
        val postBody = JSONObject()
        try {
            postBody.put("filename", filename)
        } catch (e: JSONException) {
            val errorResult = ServerPostResult(
                false,
                "IN-APP ERROR (ServerConnector): Json parse error on input data $filename"
            )

            callback.invoke(errorResult)
        }
        ObjectResponseExecutor(ServerRequest.SUBMIT_UPDATE_FILE, postBody, callback).execute()
    }

    fun logRootInstall(rootInstall: RootInstall, callback: KotlinCallback<ServerPostResult>) {
        try {
            val installationData = JSONObject(objectMapper.writeValueAsString(rootInstall))
            ObjectResponseExecutor(ServerRequest.LOG_UPDATE_INSTALLATION, installationData, callback).execute()
        } catch (e: JSONException) {
            val errorResult = ServerPostResult(
                false,
                "IN-APP ERROR (ServerConnector): Json parse error on input data $rootInstall"
            )

            callback.invoke(errorResult)
        } catch (e: JsonProcessingException) {
            val errorResult = ServerPostResult(
                false,
                "IN-APP ERROR (ServerConnector): Json parse error on input data $rootInstall"
            )

            callback.invoke(errorResult)
        }
    }

    fun verifyPurchase(purchase: Purchase, amount: String?, purchaseType: PurchaseType, callback: KotlinCallback<ServerPostResult?>) {
        val purchaseData: JSONObject
        try {
            purchaseData = JSONObject(purchase.originalJson)
            purchaseData.put("purchaseType", purchaseType.toString())
            purchaseData.put("itemType", purchase.itemType)
            purchaseData.put("signature", purchase.signature)
            purchaseData.put("amount", amount)
        } catch (ignored: JSONException) {
            val result = ServerPostResult(
                false,
                "IN-APP ERROR (ServerConnector): JSON parse error on input data ${purchase.originalJson}"
            )

            callback.invoke(result)
            return
        }
        ObjectResponseExecutor(ServerRequest.VERIFY_PURCHASE, purchaseData, callback).execute()
    }

    fun markNewsItemAsRead(newsItemId: Long, callback: KotlinCallback<ServerPostResult>?) {
        val body = JSONObject()
        try {
            body.put("news_item_id", newsItemId)
        } catch (ignored: JSONException) {
        }
        ObjectResponseExecutor(ServerRequest.NEWS_READ, body, callback).execute()
    }

    private fun getMostRecentOxygenOTAUpdate(deviceId: Long, updateMethodId: Long, callback: KotlinCallback<UpdateData?>) {
        ObjectResponseExecutor(ServerRequest.MOST_RECENT_UPDATE_DATA, callback, deviceId, updateMethodId).execute()
    }

    fun getServerStatus(online: Boolean, callback: KotlinCallback<ServerStatus>) {
        if (serverStatus == null) {
            ObjectResponseExecutor<ServerStatus?>(
                ServerRequest.SERVER_STATUS,
                { serverStatus ->
                    var automaticInstallationEnabled = false
                    var pushNotificationsDelaySeconds = 1800

                    if (settingsManager != null) {
                        automaticInstallationEnabled = settingsManager.getPreference(SettingsManager.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, false)
                        pushNotificationsDelaySeconds = settingsManager.getPreference(SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, 1800)
                    }

                    if (serverStatus == null && online) {
                        this.serverStatus = ServerStatus(
                            ServerStatus.Status.UNREACHABLE,
                            BuildConfig.VERSION_NAME,
                            automaticInstallationEnabled,
                            pushNotificationsDelaySeconds
                        )
                    } else {
                        this.serverStatus = serverStatus ?: ServerStatus(
                            ServerStatus.Status.NORMAL,
                            BuildConfig.VERSION_NAME,
                            automaticInstallationEnabled,
                            pushNotificationsDelaySeconds
                        )
                    }

                    if (settingsManager != null) {
                        settingsManager.savePreference(SettingsManager.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, this.serverStatus!!.automaticInstallationEnabled)
                        settingsManager.savePreference(SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, this.serverStatus!!.pushNotificationDelaySeconds)
                    }

                    callback.invoke(this.serverStatus!!)
                }
            ).execute()
        } else {
            callback.invoke(serverStatus!!)
        }
    }

    private fun getServerMessages(
        deviceId: Long,
        updateMethodId: Long,
        callback: KotlinCallback<List<ServerMessage>>
    ) {
        CollectionResponseExecutor(ServerRequest.SERVER_MESSAGES, callback, deviceId, updateMethodId).execute()
    }

    fun getNews(
        context: Context?,
        deviceId: Long,
        updateMethodId: Long,
        callback: KotlinCallback<List<NewsItem>>
    ) {
        CollectionResponseExecutor<NewsItem>(
            ServerRequest.NEWS,
            {
                val databaseHelper = NewsDatabaseHelper(context)

                if (!it.isNullOrEmpty() && checkNetworkConnection(context)) {
                    databaseHelper.saveNewsItems(it)
                }

                callback.invoke(databaseHelper.allNewsItems)
                databaseHelper.close()
            },
            deviceId,
            updateMethodId
        ).execute()
    }

    fun getNewsItem(context: Context?, newsItemId: Long, callback: KotlinCallback<NewsItem?>) {
        ObjectResponseExecutor<NewsItem?>(ServerRequest.NEWS_ITEM, {
            val databaseHelper = NewsDatabaseHelper(context)

            if (it != null && checkNetworkConnection(context)) {
                databaseHelper.saveNewsItem(it)
            }

            callback.invoke(databaseHelper.getNewsItem(newsItemId))

            databaseHelper.close()
        }, newsItemId).execute()
    }

    private fun <T> findMultipleFromServerResponse(
        serverRequest: ServerRequest,
        body: JSONObject?,
        vararg params: Any
    ): List<T> {
        return try {
            val response = performServerRequest(serverRequest, body, params = *params)

            if (response.isNullOrEmpty()) {
                ArrayList()
            } else objectMapper.readValue(
                response,
                objectMapper.typeFactory.constructCollectionType(List::class.java, serverRequest.returnClass)
            )
        } catch (e: Exception) {
            logError(TAG, "JSON parse error", e)
            ArrayList()
        }
    }

    private fun <T> findOneFromServerResponse(serverRequest: ServerRequest, body: JSONObject?, vararg params: Any): T? {
        return try {
            val response = performServerRequest(serverRequest, body, params = *params)

            if (response.isNullOrEmpty()) {
                null
            } else {
                objectMapper.readValue(
                    response,
                    objectMapper.typeFactory.constructType(serverRequest.returnClass)
                )
            }
        } catch (e: Exception) {
            logError(TAG, "JSON parse error", e)
            null
        }
    }

    private fun performServerRequest(
        request: ServerRequest,
        body: JSONObject?,
        retryCount: Int = 0,
        vararg params: Any
    ): String? {
        return try {
            val requestUrl = request.getUrl(*params) ?: return null

            logVerbose(TAG, "")
            logVerbose(TAG, "Performing ${request.requestMethod} request to URL $requestUrl")
            logVerbose(TAG, "Timeout is set to ${request.timeOutInSeconds} seconds.")

            val urlConnection = requestUrl.openConnection() as HttpURLConnection
            val timeOutInMilliseconds = request.timeOutInSeconds * 1000

            //setup request
            urlConnection.setRequestProperty(USER_AGENT_TAG, ApplicationData.APP_USER_AGENT)
            urlConnection.requestMethod = request.requestMethod.toString()
            urlConnection.connectTimeout = timeOutInMilliseconds
            urlConnection.readTimeout = timeOutInMilliseconds

            if (body != null) {
                urlConnection.doOutput = true
                urlConnection.setRequestProperty("Accept", "application/json")

                val out = urlConnection.outputStream
                val outputBytes = body.toString().toByteArray()

                out.write(outputBytes)
                out.close()
            }

            val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
            var inputLine: String?
            val response = StringBuilder()

            while (reader.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }

            reader.close()

            val rawResponse = response.toString()
            logVerbose(TAG, "Response: $rawResponse")
            rawResponse
        } catch (e: Exception) {
            if (retryCount < 5) {
                performServerRequest(request, body, retryCount + 1, *params)
            } else {
                if (isNetworkError(e)) {
                    logWarning(TAG, NetworkException("Error performing request <${request.toString(params)}>."))
                } else {
                    logError(TAG, "Error performing request <${request.toString(params)}>", e)
                }
                null
            }
        }
    }

    public override fun clone(): ServerConnector {
        return try {
            super.clone() as ServerConnector
        } catch (e: CloneNotSupportedException) {
            logError(TAG, "Internal error cloning ServerConnector", e)
            ServerConnector(settingsManager)
        }
    }

    private inner class CollectionResponseExecutor<T> internal constructor(
        private val serverRequest: ServerRequest,
        private val body: JSONObject?,
        private val callback: KotlinCallback<List<T>>?,
        private vararg val params: Any
    ) : AsyncTask<Void?, Void?, List<T>>() {

        internal constructor(
            serverRequest: ServerRequest,
            callback: KotlinCallback<List<T>>?,
            vararg params: Any
        ) : this(serverRequest, null, callback, *params)

        override fun doInBackground(vararg voids: Void?): List<T> {
            return findMultipleFromServerResponse(serverRequest, body, *params)
        }

        override fun onPostExecute(results: List<T>) {
            callback?.invoke(results)
        }
    }

    private inner class ObjectResponseExecutor<E> internal constructor(
        private val serverRequest: ServerRequest,
        private val body: JSONObject?,
        private val callback: KotlinCallback<E>?,
        private vararg val params: Any
    ) : AsyncTask<Void?, Void?, E>() {

        internal constructor(
            serverRequest: ServerRequest,
            callback: KotlinCallback<E>?,
            vararg params: Any
        ) : this(serverRequest, null, callback, *params)

        override fun doInBackground(vararg voids: Void?): E? {
            return findOneFromServerResponse(serverRequest, body, *params)
        }

        override fun onPostExecute(result: E) {
            callback?.invoke(result)
        }
    }

    companion object {
        private const val USER_AGENT_TAG = "User-Agent"
        private const val TAG = "ServerConnector"
    }
}
