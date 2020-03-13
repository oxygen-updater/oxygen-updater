package com.arjanvlek.oxygenupdater.internal.server

import android.os.AsyncTask
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.enums.ServerRequest
import com.arjanvlek.oxygenupdater.exceptions.NetworkException
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.objectMapper
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.utils.ExceptionUtils.isNetworkError
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Logger.logVerbose
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.fasterxml.jackson.core.JsonProcessingException
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.util.*

class ServerConnector(private val settingsManager: SettingsManager?) : Cloneable {

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
            urlConnection.setRequestProperty(USER_AGENT_TAG, OxygenUpdater.APP_USER_AGENT)
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
