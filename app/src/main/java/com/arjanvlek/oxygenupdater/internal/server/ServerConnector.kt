package com.arjanvlek.oxygenupdater.internal.server

import android.content.Context
import android.os.AsyncTask
import android.text.Html
import androidx.core.content.ContextCompat
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.APP_OUTDATED_ERROR
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.APP_USER_AGENT
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NETWORK_CONNECTION_ERROR
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.SERVER_MAINTENANCE_ERROR
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.UNABLE_TO_FIND_A_MORE_RECENT_BUILD
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.Device
import com.arjanvlek.oxygenupdater.domain.UpdateMethod
import com.arjanvlek.oxygenupdater.installation.automatic.RootInstall
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuidePage
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.internal.root.RootAccessChecker
import com.arjanvlek.oxygenupdater.news.NewsDatabaseHelper
import com.arjanvlek.oxygenupdater.news.NewsItem
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_DOWNLOAD_URL
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_FILE_NAME
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_IS_UP_TO_DATE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_DESCRIPTION
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_OFFLINE_UPDATE_NAME
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_SHOW_APP_UPDATE_MESSAGES
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_SHOW_NEWS_MESSAGES
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD_ID
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseType
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.Purchase
import com.arjanvlek.oxygenupdater.updateinformation.Banner
import com.arjanvlek.oxygenupdater.updateinformation.ServerMessage
import com.arjanvlek.oxygenupdater.updateinformation.ServerStatus
import com.arjanvlek.oxygenupdater.updateinformation.ServerStatus.Status.NORMAL
import com.arjanvlek.oxygenupdater.updateinformation.ServerStatus.Status.UNREACHABLE
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import java8.util.function.Consumer
import java8.util.stream.Collectors
import java8.util.stream.StreamSupport
import org.joda.time.LocalDateTime
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.*

class ServerConnector(private val settingsManager: SettingsManager?) : Cloneable {

    private val objectMapper: ObjectMapper = ObjectMapper()

    private val devices: MutableList<Device>
    private var deviceFetchDate: LocalDateTime? = null
    private var serverStatus: ServerStatus? = null

    init {
        devices = ArrayList()
    }

    fun getDevices(callback: Consumer<List<Device>>) {
        getDevices(false, callback)
    }

    fun getDevices(alwaysFetch: Boolean, callback: Consumer<List<Device>>) {
        if (deviceFetchDate != null && deviceFetchDate!!.plusMinutes(5).isAfter(LocalDateTime.now()) && !alwaysFetch) {
            logVerbose(TAG, "Used local cache to fetch devices...")
            callback.accept(devices)
        } else {
            logVerbose(TAG, "Used remote server to fetch devices...")
            CollectionResponseExecutor<Device>(ServerRequest.DEVICES, Consumer { devices ->
                this.devices.clear()
                this.devices.addAll(devices)
                deviceFetchDate = LocalDateTime.now()
                callback.accept(devices)
            }).execute()
        }
    }

    fun getUpdateMethods(deviceId: Long, callback: Consumer<List<UpdateMethod>>) {
        CollectionResponseExecutor<UpdateMethod>(ServerRequest.UPDATE_METHODS, Consumer { updateMethods ->
            RootAccessChecker.checkRootAccess(Consumer { hasRootAccess ->
                if (hasRootAccess!!) {
                    callback.accept(StreamSupport.stream<UpdateMethod>(updateMethods)
                            .filter { it.isForRootedDevice }
                            .map { um -> um.setRecommended(if (um.isRecommendedWithRoot) "1" else "0") }
                            .collect(Collectors.toList()))
                } else {
                    callback.accept(StreamSupport.stream<UpdateMethod>(updateMethods)
                            .map { um -> um.setRecommended(if (um.isRecommendedWithoutRoot) "1" else "0") }
                            .collect(Collectors.toList()))
                }
            })
        }, deviceId).execute()
    }

    fun getAllUpdateMethods(callback: Consumer<List<UpdateMethod>>) {
        CollectionResponseExecutor(ServerRequest.ALL_UPDATE_METHODS, callback).execute()
    }

    fun getUpdateData(online: Boolean, deviceId: Long, updateMethodId: Long,
                      incrementalSystemVersion: String, callback: Consumer<UpdateData>, errorFunction: Consumer<String>) {

        ObjectResponseExecutor(ServerRequest.UPDATE_DATA, Consumer<UpdateData> {
            var updateData = it
            if (updateData?.information != null
                    && updateData.information == UNABLE_TO_FIND_A_MORE_RECENT_BUILD
                    && updateData.isUpdateInformationAvailable
                    && updateData.isSystemIsUpToDate) {
                getMostRecentOxygenOTAUpdate(deviceId, updateMethodId, callback)
            } else if (!online) {
                if (settingsManager!!.checkIfOfflineUpdateDataIsAvailable()) {
                    updateData = UpdateData()
                    updateData.id = settingsManager.getPreference(PROPERTY_OFFLINE_ID, 0L)
                    updateData.versionNumber = settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_NAME, "")
                    updateData.downloadSize = settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE, 0L)
                    updateData.description = settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION, "")
                    updateData.downloadUrl = settingsManager.getPreference(PROPERTY_OFFLINE_DOWNLOAD_URL, "")
                    updateData.isUpdateInformationAvailable = settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE, false)
                    updateData.filename = settingsManager.getPreference(PROPERTY_OFFLINE_FILE_NAME, "")
                    updateData.isSystemIsUpToDate = settingsManager.getPreference(PROPERTY_OFFLINE_IS_UP_TO_DATE, false)
                    callback.accept(updateData)
                } else {
                    errorFunction.accept(NETWORK_CONNECTION_ERROR)
                }
            } else {
                callback.accept(updateData)
            }
        }, deviceId, updateMethodId, incrementalSystemVersion).execute()
    }

    fun getInAppMessages(online: Boolean, callback: Consumer<List<Banner>>, errorCallback: Consumer<String>) {
        val inAppBars = ArrayList<Banner>()

        getServerStatus(online, Consumer { serverStatus ->
            getServerMessages(settingsManager!!.getPreference(PROPERTY_DEVICE_ID, -1L),
                    settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L),
                    Consumer { serverMessages ->
                        // Add the "No connection" bar depending on the network status of the device.
                        if (!online) {
                            inAppBars.add(object : Banner {
                                override fun getBannerText(context: Context): String {
                                    return context.getString(R.string.error_no_internet_connection)
                                }

                                override fun getColor(context: Context): Int {
                                    // since the primary color is red, use that to match the app bar
                                    return ContextCompat.getColor(context, R.color.colorPrimary)
                                }
                            })
                        }

                        if (serverMessages != null && settingsManager.getPreference(PROPERTY_SHOW_NEWS_MESSAGES, true)) {
                            inAppBars.addAll(serverMessages)
                        }

                        val status = serverStatus.status

                        if (status!!.isUserRecoverableError) {
                            inAppBars.add(serverStatus)
                        }

                        if (status.isNonRecoverableError) {
                            when (status) {
                                ServerStatus.Status.MAINTENANCE -> errorCallback.accept(SERVER_MAINTENANCE_ERROR)
                                ServerStatus.Status.OUTDATED -> errorCallback.accept(APP_OUTDATED_ERROR)
                            }
                        }

                        if (settingsManager.getPreference(PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !serverStatus.checkIfAppIsUpToDate()) {
                            inAppBars.add(object : Banner {

                                override fun getBannerText(context: Context): CharSequence {
                                    return Html.fromHtml(String.format(context.getString(R.string.new_app_version), serverStatus.latestAppVersion))
                                }

                                override fun getColor(context: Context): Int {
                                    return ContextCompat.getColor(context, R.color.colorPositive)
                                }
                            })
                        }
                        callback.accept(inAppBars)
                    })
        })
    }

    fun getInstallGuidePage(deviceId: Long, updateMethodId: Long, pageNumber: Int, callback: Consumer<InstallGuidePage>) {
        ObjectResponseExecutor(ServerRequest.INSTALL_GUIDE_PAGE, callback, deviceId, updateMethodId, pageNumber).execute()
    }

    fun submitUpdateFile(filename: String, callback: Consumer<ServerPostResult>) {
        val postBody = JSONObject()
        try {
            postBody.put("filename", filename)
        } catch (e: JSONException) {
            val errorResult = ServerPostResult()
            errorResult.isSuccess = false
            errorResult.errorMessage = "IN-APP ERROR (ServerConnector): Json parse error on input data $filename"
            callback.accept(errorResult)
        }

        ObjectResponseExecutor(ServerRequest.SUBMIT_UPDATE_FILE, postBody, callback).execute()
    }

    fun logRootInstall(rootInstall: RootInstall, callback: Consumer<ServerPostResult>) {

        try {
            val installationData = JSONObject(objectMapper.writeValueAsString(rootInstall))

            ObjectResponseExecutor(ServerRequest.LOG_UPDATE_INSTALLATION, installationData, callback).execute()
        } catch (e: JSONException) {
            val errorResult = ServerPostResult()
            errorResult.isSuccess = false
            errorResult.errorMessage = "IN-APP ERROR (ServerConnector): Json parse error on input data " + rootInstall
                    .toString()
            callback.accept(errorResult)
        } catch (e: JsonProcessingException) {
            val errorResult = ServerPostResult()
            errorResult.isSuccess = false
            errorResult.errorMessage = "IN-APP ERROR (ServerConnector): Json parse error on input data $rootInstall"
            callback.accept(errorResult)
        }

    }

    fun verifyPurchase(purchase: Purchase, amount: String, purchaseType: PurchaseType, callback: Consumer<ServerPostResult>) {
        val purchaseData: JSONObject

        try {
            purchaseData = JSONObject(purchase.originalJson)
            purchaseData.put("purchaseType", purchaseType.toString())
            purchaseData.put("itemType", purchase.itemType)
            purchaseData.put("signature", purchase.signature)
            purchaseData.put("amount", amount)
        } catch (ignored: JSONException) {
            val result = ServerPostResult()
            result.isSuccess = false
            result.errorMessage = "IN-APP ERROR (ServerConnector): JSON parse error on input data " + purchase.originalJson
            callback.accept(result)
            return
        }

        ObjectResponseExecutor(ServerRequest.VERIFY_PURCHASE, purchaseData, callback).execute()
    }

    fun markNewsItemAsRead(newsItemId: Long, callback: Consumer<ServerPostResult>) {
        val body = JSONObject()

        try {
            body.put("news_item_id", newsItemId)
        } catch (ignored: JSONException) {

        }

        ObjectResponseExecutor(ServerRequest.NEWS_READ, body, callback).execute()
    }

    private fun getMostRecentOxygenOTAUpdate(deviceId: Long, updateMethodId: Long, callback: Consumer<UpdateData>) {
        ObjectResponseExecutor(ServerRequest.MOST_RECENT_UPDATE_DATA, callback, deviceId, updateMethodId).execute()
    }

    fun getServerStatus(online: Boolean, callback: Consumer<ServerStatus>) {
        if (serverStatus == null) {
            ObjectResponseExecutor<ServerStatus>(ServerRequest.SERVER_STATUS, Consumer { serverStatus ->
                var automaticInstallationEnabled = false
                var pushNotificationsDelaySeconds = 1800

                if (settingsManager != null) {
                    automaticInstallationEnabled = settingsManager.getPreference(PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, false)
                    pushNotificationsDelaySeconds = settingsManager.getPreference(PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, 1800)
                }

                if (serverStatus == null && online) {
                    this.serverStatus = ServerStatus(UNREACHABLE, BuildConfig.VERSION_NAME, automaticInstallationEnabled, pushNotificationsDelaySeconds)
                } else {
                    this.serverStatus = serverStatus
                            ?: ServerStatus(NORMAL, BuildConfig.VERSION_NAME, automaticInstallationEnabled, pushNotificationsDelaySeconds)
                }

                if (settingsManager != null) {
                    settingsManager.savePreference(PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED, this.serverStatus!!.isAutomaticInstallationEnabled)
                    settingsManager.savePreference(PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, this.serverStatus!!.pushNotificationDelaySeconds)
                }

                callback.accept(this.serverStatus)
            }).execute()
        } else {
            callback.accept(serverStatus)
        }
    }

    private fun getServerMessages(deviceId: Long, updateMethodId: Long, callback: Consumer<List<ServerMessage>>) {
        CollectionResponseExecutor(ServerRequest.SERVER_MESSAGES, callback, deviceId, updateMethodId)
                .execute()
    }

    fun getNews(context: Context, deviceId: Long, updateMethodId: Long, callback: Consumer<List<NewsItem>>) {
        CollectionResponseExecutor<NewsItem>(ServerRequest.NEWS, Consumer { newsItems ->
            val databaseHelper = NewsDatabaseHelper(context)

            if (newsItems != null && newsItems.isNotEmpty() && Utils.checkNetworkConnection(context)) {
                databaseHelper.saveNewsItems(newsItems)
            }

            callback.accept(databaseHelper.allNewsItems)

            databaseHelper.close()
        }, deviceId, updateMethodId).execute()
    }

    fun getNewsItem(context: Context, newsItemId: Long, callback: Consumer<NewsItem>) {

        ObjectResponseExecutor<NewsItem>(ServerRequest.NEWS_ITEM, Consumer { newsItem ->
            val databaseHelper = NewsDatabaseHelper(context)

            if (newsItem != null && Utils.checkNetworkConnection(context)) {
                databaseHelper.saveNewsItem(newsItem)
            }

            callback.accept(databaseHelper.getNewsItem(newsItemId))

            databaseHelper.close()
        }, newsItemId).execute()
    }

    private fun <T> findMultipleFromServerResponse(serverRequest: ServerRequest, body: JSONObject?,
                                                   vararg params: Any): List<T> {
        return try {
            val response = performServerRequest(serverRequest, body, *params)
            if (response == null || response.isEmpty()) {
                ArrayList()
            } else objectMapper.readValue(response, objectMapper.typeFactory
                    .constructCollectionType(List::class.java, serverRequest.returnClass))

        } catch (e: Exception) {
            logError(TAG, "JSON parse error", e)
            ArrayList()
        }

    }

    private fun <T> findOneFromServerResponse(serverRequest: ServerRequest, body: JSONObject?, vararg params: Any): T? {
        return try {
            val response = performServerRequest(serverRequest, body, *params)
            if (response == null || response.isEmpty()) {
                null
            } else objectMapper.readValue<T>(response, objectMapper.typeFactory.constructType(serverRequest.returnClass))

        } catch (e: Exception) {
            logError(TAG, "JSON parse error", e)
            null
        }

    }

    private fun performServerRequest(request: ServerRequest, body: JSONObject?, vararg params: Any): String? {
        return performServerRequest(request, body, 0, *params)
    }

    private fun performServerRequest(request: ServerRequest, body: JSONObject?, retryCount: Int,
                                     vararg params: Any): String? {

        try {
            val requestUrl = request.getUrl(*params) ?: return null

            logVerbose(TAG, "")
            logVerbose(TAG, "Performing " + request.requestMethod
                    .toString() + " request to URL " + requestUrl.toString())
            logVerbose(TAG, "Timeout is set to " + request.timeOutInSeconds + " seconds.")

            val urlConnection = requestUrl.openConnection() as HttpURLConnection

            val timeOutInMilliseconds = request.timeOutInSeconds * 1000

            //setup request
            urlConnection.setRequestProperty(USER_AGENT_TAG, APP_USER_AGENT)
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
            val response = StringBuilder()

            var inputLine = reader.readLine()
            while (inputLine != null) {
                response.append(inputLine)
                inputLine = reader.readLine()
            }

            reader.close()
            val rawResponse = response.toString()
            logVerbose(TAG, "Response: $rawResponse")
            return rawResponse
        } catch (e: Exception) {
            return if (retryCount < 5) {
                performServerRequest(request, body, retryCount + 1, *params)
            } else {
                if (ExceptionUtils.isNetworkError(e)) {
                    logWarning(TAG, NetworkException("Error performing request <" +
                            request.toString(*params) + ">."))
                } else {
                    logError(TAG, "Error performing request <" + request.toString(*params) + ">", e)
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

    private inner class CollectionResponseExecutor<T> internal constructor(private val serverRequest: ServerRequest, private val body: JSONObject?, private val callback: Consumer<List<T>>?, vararg params: Any) : AsyncTask<Void, Void, List<T>>() {
        private val params: Array<Any> = arrayOf(params)

        internal constructor(serverRequest: ServerRequest, callback: Consumer<List<T>>, vararg params: Any) : this(serverRequest, null, callback, *params) {}

        override fun doInBackground(vararg voids: Void): List<T> {
            return findMultipleFromServerResponse(serverRequest, body, *params)
        }

        override fun onPostExecute(results: List<T>) {
            callback?.accept(results)
        }
    }

    private inner class ObjectResponseExecutor<E> internal constructor(private val serverRequest: ServerRequest, private val body: JSONObject?, private val callback: Consumer<E>?, vararg params: Any) : AsyncTask<Void, Void, E>() {
        private val params: Array<Any> = arrayOf(params)

        internal constructor(serverRequest: ServerRequest, callback: Consumer<E>?, vararg params: Any) : this(serverRequest, null, callback, *params) {}

        override fun doInBackground(vararg voids: Void): E? {
            return findOneFromServerResponse<E>(serverRequest, body, *params)
        }

        override fun onPostExecute(result: E) {
            callback?.accept(result)
        }

    }

    companion object {
        private val USER_AGENT_TAG = "User-Agent"
        private val TAG = "ServerConnector"
    }
}

