package com.arjanvlek.oxygenupdater.internal.server

import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.domain.Device
import com.arjanvlek.oxygenupdater.domain.UpdateMethod
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuidePage
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.news.NewsItem
import com.arjanvlek.oxygenupdater.updateinformation.ServerMessage
import com.arjanvlek.oxygenupdater.updateinformation.ServerStatus
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData
import java.net.MalformedURLException
import java.net.URL
import java.util.*

internal enum class ServerRequest(
    val requestMethod: RequestMethod,
    private val url: String,
    val timeOutInSeconds: Int,
    val returnClass: Class<*>?
) {
    DEVICES("devices/%1\$s", Device::class.java),
    UPDATE_METHODS("updateMethods/%1d", UpdateMethod::class.java),
    ALL_UPDATE_METHODS("allUpdateMethods", UpdateMethod::class.java),
    UPDATE_DATA("updateData/%1d/%2d/%3\$s", UpdateData::class.java),
    MOST_RECENT_UPDATE_DATA("mostRecentUpdateData/%1d/%2d", UpdateData::class.java),
    INSTALL_GUIDE_PAGE("installGuide/%1d/%2d/%3d", InstallGuidePage::class.java),
    SERVER_STATUS("serverStatus", ServerStatus::class.java),
    SERVER_MESSAGES("serverMessages/%1d/%2d", ServerMessage::class.java),
    NEWS("news/%1d/%2d", NewsItem::class.java),
    NEWS_ITEM("news-item/%1d", NewsItem::class.java),
    NEWS_READ(RequestMethod.POST, "news-read", ServerPostResult::class.java),
    SUBMIT_UPDATE_FILE(RequestMethod.POST, "submit-update-file", ServerPostResult::class.java),
    LOG_UPDATE_INSTALLATION(RequestMethod.POST, "log-update-installation", ServerPostResult::class.java),
    VERIFY_PURCHASE(RequestMethod.POST, "verify-purchase", 120, ServerPostResult::class.java);

    constructor(url: String, returnClass: Class<*>?) : this(
        url,
        DEFAULT_TIMEOUT,
        returnClass
    )

    constructor(url: String, timeOutInSeconds: Int, returnClass: Class<*>?) : this(
        RequestMethod.GET,
        url,
        timeOutInSeconds,
        returnClass
    )

    constructor(requestMethod: RequestMethod, url: String, returnClass: Class<*>?) : this(
        requestMethod,
        url,
        DEFAULT_TIMEOUT,
        returnClass
    )

    fun getUrl(vararg params: Any): URL? {
        return try {
            URL(getUrlString(*params))
        } catch (e: MalformedURLException) {
            logError("ServerRequest", OxygenUpdaterException("Malformed URL: $url"))
            null
        } catch (e: Exception) {
            logError("ServerRequest", "Exception when parsing URL $url  with parameters $params", e)
            null
        }
    }

    private fun getUrlString(vararg params: Any): String {
        return String.format(Locale.US, BuildConfig.SERVER_BASE_URL + url, *params).replace(" ", "")
    }

    fun toString(vararg params: Any): String {
        return requestMethod.toString() + " " + getUrl(*params)
    }

    @Suppress("unused")
    internal enum class RequestMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH
    }

    companion object {
        private const val DEFAULT_TIMEOUT = 10
    }

}
