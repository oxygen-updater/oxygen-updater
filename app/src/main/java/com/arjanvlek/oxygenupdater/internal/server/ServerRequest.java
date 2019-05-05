package com.arjanvlek.oxygenupdater.internal.server;


import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.UpdateMethod;
import com.arjanvlek.oxygenupdater.installation.manual.InstallGuidePage;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.news.NewsItem;
import com.arjanvlek.oxygenupdater.updateinformation.ServerMessage;
import com.arjanvlek.oxygenupdater.updateinformation.ServerStatus;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import static com.arjanvlek.oxygenupdater.BuildConfig.SERVER_BASE_URL;
import static com.arjanvlek.oxygenupdater.internal.server.ServerRequest.RequestMethod.GET;
import static com.arjanvlek.oxygenupdater.internal.server.ServerRequest.RequestMethod.POST;

enum ServerRequest {

    DEVICES("devices", Device.class),

    UPDATE_METHODS("updateMethods/%1d", UpdateMethod.class),
    ALL_UPDATE_METHODS("allUpdateMethods", UpdateMethod.class),

    UPDATE_DATA("updateData/%1d/%2d/%3$s", UpdateData.class),
    MOST_RECENT_UPDATE_DATA("mostRecentUpdateData/%1d/%2d", UpdateData.class),

    INSTALL_GUIDE_PAGE("installGuide/%1d/%2d/%3d", InstallGuidePage.class),

    SERVER_STATUS("serverStatus", ServerStatus.class),

    SERVER_MESSAGES("serverMessages/%1d/%2d", ServerMessage.class),

    NEWS("news/%1d/%2d", NewsItem.class),
    NEWS_ITEM("news-item/%1d", NewsItem.class),
    NEWS_READ(POST, "news-read", ServerPostResult.class),

    SUBMIT_UPDATE_FILE(POST, "submit-update-file", ServerPostResult.class),
    LOG_UPDATE_INSTALLATION(POST, "log-update-installation", ServerPostResult.class),

    VERIFY_PURCHASE(POST, "verify-purchase", 120, ServerPostResult.class);

    private final RequestMethod requestMethod;
    private final String url;
    private final int timeOutInSeconds;
    private final Class<?> returnClass;
    private static final int DEFAULT_TIMEOUT = 10;

    ServerRequest(String url, Class<?> returnClass) {
        this(GET, url, DEFAULT_TIMEOUT, returnClass);
    }

    ServerRequest(String url, int timeOutInSeconds, Class<?> returnClass) {
        this(GET, url, timeOutInSeconds, returnClass);
    }

    ServerRequest(RequestMethod requestMethod, String url, Class<?> returnClass) {
        this(requestMethod, url, DEFAULT_TIMEOUT, returnClass);
    }

    ServerRequest(RequestMethod requestMethod, String url, int timeOutInSeconds, Class<?> returnClass) {
        this.requestMethod = requestMethod;
        this.url = url;
        this.timeOutInSeconds = timeOutInSeconds;
        this.returnClass = returnClass;
    }

    RequestMethod getRequestMethod() {
        return this.requestMethod;
    }

    URL getUrl(Object... params) {
        try {
            return new URL(getUrlString(params));
        } catch (MalformedURLException e) {
            Logger.logError("ServerRequest", new OxygenUpdaterException("Malformed URL: " + this.url));
            return null;
        } catch (Exception e) {
            Logger.logError("ServerRequest", "Exception when parsing URL " + this.url + "  with parameters " + Arrays.toString(params), e);
            return null;
        }
    }

    int getTimeOutInSeconds() {
        return this.timeOutInSeconds;
    }

    Class<?> getReturnClass() {
        return this.returnClass;
    }

    enum RequestMethod {
        GET, POST, PUT, DELETE, PATCH
    }

    private String getUrlString(Object... params) {
        return String.format(Locale.US, SERVER_BASE_URL + this.url, params).replace(" ", "");
    }

    public String toString(Object... params) {
        return requestMethod + " " + getUrl(params);
    }

}
