package com.arjanvlek.oxygenupdater.Server;


import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.InstallGuideData;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;

import java.net.MalformedURLException;
import java.net.URL;

import static com.arjanvlek.oxygenupdater.BuildConfig.SERVER_BASE_URL;

enum ServerRequest {

    ALL_UPDATE_METHODS {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "allUpdateMethods");
        }

        @Override
        int getTimeOutInSeconds() {
            return 20;
        }

        @Override
        Class<?> getReturnClass() {
            return UpdateMethod.class;
        }
    },
    DEVICES {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "devices");
        }

        @Override
        int getTimeOutInSeconds() {
            return 20;
        }

        @Override
        Class<?> getReturnClass() {
            return Device.class;
        }
    },
    INSTALL_GUIDE {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "installGuide/" + params[0] + "/" + params[1] + "/" + params[2]);
        }

        @Override
        int getTimeOutInSeconds() {
            return 10;
        }

        @Override
        Class<?> getReturnClass() {
            return InstallGuideData.class;
        }
    },
    UPDATE_METHODS {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "updateMethods/" + params[0]);
        }

        @Override
        int getTimeOutInSeconds() {
            return 20;
        }

        @Override
        Class<?> getReturnClass() {
            return UpdateMethod.class;
        }
    },
    UPDATE_DATA {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "updateData/" + params[0] + "/" + params[1] + "/" + params[2]);
        }

        @Override
        int getTimeOutInSeconds() {
            return 15;
        }

        @Override
        Class<?> getReturnClass() {
            return OxygenOTAUpdate.class;
        }
    },
    MOST_RECENT_UPDATE_DATA {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "mostRecentUpdateData/" + params[0] + "/" + params[1]);
        }

        @Override
        int getTimeOutInSeconds() {
            return 10;
        }

        @Override
        Class<?> getReturnClass() {
            return OxygenOTAUpdate.class;
        }
    },
    SERVER_STATUS {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "serverStatus");
        }

        @Override
        int getTimeOutInSeconds() {
            return 10;
        }

        @Override
        Class<?> getReturnClass() {
            return ServerStatus.class;
        }
    },
    SERVER_MESSAGES {
        @Override
        URL getURL(String... params) throws MalformedURLException {
            return new URL(SERVER_BASE_URL + "serverMessages/" + params[0] + "/" + params[1]);
        }

        @Override
        int getTimeOutInSeconds() {
            return 20;
        }

        @Override
        Class<?> getReturnClass() {
            return ServerMessage.class;
        }
    };

    abstract URL getURL(String...params) throws MalformedURLException;
    abstract int getTimeOutInSeconds();
    abstract Class<?> getReturnClass();
}
