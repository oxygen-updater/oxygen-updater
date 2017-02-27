package com.arjanvlek.oxygenupdater.Server;



import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.InstallGuidePage;
import com.arjanvlek.oxygenupdater.Model.LogResult;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.UpdateData;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.support.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static com.arjanvlek.oxygenupdater.BuildConfig.SERVER_BASE_URL;
import static com.arjanvlek.oxygenupdater.Server.ServerRequest.RequestMethod.GET;
import static com.arjanvlek.oxygenupdater.Server.ServerRequest.RequestMethod.POST;

enum ServerRequest {

    DEVICES(SERVER_BASE_URL + "devices", 20, Device.class),
    UPDATE_METHODS(SERVER_BASE_URL + "updateMethods/%1d", 20, UpdateMethod.class),
    ALL_UPDATE_METHODS(SERVER_BASE_URL + "allUpdateMethods", 20, UpdateMethod.class),
    UPDATE_DATA(SERVER_BASE_URL + "updateData/%1d/%2d/%3$s", 15, UpdateData.class),
    MOST_RECENT_UPDATE_DATA(SERVER_BASE_URL + "mostRecentUpdateData/%1d/%2d", 10, UpdateData.class),
    INSTALL_GUIDE_PAGE(SERVER_BASE_URL + "installGuide/%1d/%2d/%3d", 10, InstallGuidePage.class),
    SERVER_STATUS(SERVER_BASE_URL + "serverStatus", 10, ServerStatus.class),
    SERVER_MESSAGES(SERVER_BASE_URL + "serverMessages/%1d/%2d", 20, ServerMessage.class),
    LOG(POST, SERVER_BASE_URL + "log", 20, LogResult.class);

    private final RequestMethod requestMethod;
    private final String url;
    private final int timeOutInSeconds;
    private final Class<?> returnClass;

    ServerRequest(String url, int timeOutInSeconds, Class<?> returnClass) {
        this(GET, url, timeOutInSeconds, returnClass);
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
            return new URL(String.format(this.url, params).replace(" ", ""));
        } catch (MalformedURLException e) {
            Logger.logError("ServerRequest", "Malformed URL: " + this.url);
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

}
