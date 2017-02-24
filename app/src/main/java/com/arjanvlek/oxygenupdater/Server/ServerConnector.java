package com.arjanvlek.oxygenupdater.Server;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.InstallGuideData;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.NetworkConnectionManager;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java8.util.function.Consumer;

import static com.arjanvlek.oxygenupdater.ApplicationContext.APP_OUTDATED_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationContext.APP_USER_AGENT;
import static com.arjanvlek.oxygenupdater.ApplicationContext.NETWORK_CONNECTION_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationContext.SERVER_MAINTENANCE_ERROR;
import static com.arjanvlek.oxygenupdater.ApplicationContext.UNABLE_TO_FIND_A_MORE_RECENT_BUILD;
import static com.arjanvlek.oxygenupdater.Model.ServerStatus.Status.NORMAL;
import static com.arjanvlek.oxygenupdater.Model.ServerStatus.Status.UNREACHABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_FILE_NAME;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_IS_UP_TO_DATE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class ServerConnector {

    private final static String USER_AGENT_TAG = "User-Agent";
    private static final String TAG = "ServerConnector";

    private final ObjectMapper objectMapper;
    private final SettingsManager settingsManager;

    private final List<Device> devices;
    private LocalDateTime deviceFetchDate;

    public ServerConnector(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        this.objectMapper = new ObjectMapper();
        this.devices = new ArrayList<>();
    }

    public void getDevices(Consumer<List<Device>> callback) {
        getDevices(false, callback);
    }

    public void getDevices(boolean alwaysFetch, Consumer<List<Device>> callback) {
        if (deviceFetchDate != null && deviceFetchDate.plusMinutes(5).isAfter(LocalDateTime.now()) && !alwaysFetch) {
            Log.v(TAG, "Used local cache to fetch devices...");
            callback.accept(devices);
        }

        else {
            Log.v(TAG, "Used remote server to fetch devices...");
            new GetMultipleAsync<Device>(ServerRequest.DEVICES, (devices) -> {
                this.devices.clear();
                this.devices.addAll(devices);
                this.deviceFetchDate = LocalDateTime.now();
                callback.accept(devices);
            }).execute();
        }
    }

    public void getUpdateMethods(@NonNull Long deviceId, Consumer<List<UpdateMethod>> callback) {
        new GetMultipleAsync<>(ServerRequest.UPDATE_METHODS, callback, deviceId.toString()).execute();
    }

    public void getAllUpdateMethods(Consumer<List<UpdateMethod>> callback) {
        new GetMultipleAsync<>(ServerRequest.ALL_UPDATE_METHODS, callback).execute();
    }

    public void getUpdateData(boolean online, @NonNull Long deviceId, @NonNull Long updateMethodId, @NonNull String incrementalSystemVersion, Consumer<OxygenOTAUpdate> callback, Consumer<String> errorFunction) {

        new GetSingleAsync<>(ServerRequest.UPDATE_DATA, new Consumer<OxygenOTAUpdate>() {
            @Override
            public void accept(OxygenOTAUpdate oxygenOTAUpdate) {
                if (oxygenOTAUpdate != null && oxygenOTAUpdate.getInformation() != null && oxygenOTAUpdate.getInformation().equals(UNABLE_TO_FIND_A_MORE_RECENT_BUILD) && oxygenOTAUpdate.isUpdateInformationAvailable() && oxygenOTAUpdate.isSystemIsUpToDate()) {
                    getMostRecentOxygenOTAUpdate(deviceId, updateMethodId, callback);
                } else if(!online) {
                    if (settingsManager.checkIfCacheIsAvailable()) {
                        oxygenOTAUpdate = new OxygenOTAUpdate();
                        oxygenOTAUpdate.setVersionNumber(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_NAME));
                        oxygenOTAUpdate.setDownloadSize(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE));
                        oxygenOTAUpdate.setDescription(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION));
                        oxygenOTAUpdate.setUpdateInformationAvailable(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE));
                        oxygenOTAUpdate.setFilename(settingsManager.getPreference(PROPERTY_OFFLINE_FILE_NAME));
                        oxygenOTAUpdate.setSystemIsUpToDate(settingsManager.getPreference(PROPERTY_OFFLINE_IS_UP_TO_DATE, false));
                        callback.accept(oxygenOTAUpdate);
                    } else {
                        errorFunction.accept(NETWORK_CONNECTION_ERROR);
                    }
                } else {
                    callback.accept(oxygenOTAUpdate);
                }
            }
        }, deviceId.toString(), updateMethodId.toString(), incrementalSystemVersion).execute();
    }

    public void getInAppMessages(boolean online, Consumer<List<Banner>> callback, Consumer<String> errorCallback) {
        List<Banner> inAppBars = new ArrayList<>();

        getServerStatus((serverStatus) -> getServerMessages(settingsManager.getPreference(PROPERTY_DEVICE_ID), settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID), (serverMessages) -> {
            // Add the "No connection" bar depending on the network status of the device.
            if (!online) {
                inAppBars.add(new Banner() {
                    @Override
                    public String getBannerText(Context context) {
                        return context.getString(R.string.error_no_internet_connection);
                    }

                    @Override
                    public int getColor(Context context) {
                        return ContextCompat.getColor(context, R.color.holo_red_light);
                    }
                });
            }

            if (serverMessages != null && settingsManager.getPreference(PROPERTY_SHOW_NEWS_MESSAGES, true)) {
                inAppBars.addAll(serverMessages);
            }

            final ServerStatus finalServerStatus;

            if (serverStatus == null && online) {
                finalServerStatus = new ServerStatus(UNREACHABLE, BuildConfig.VERSION_NAME);
            } else {
                finalServerStatus = serverStatus != null ? serverStatus : new ServerStatus(NORMAL, BuildConfig.VERSION_NAME);
            }

            ServerStatus.Status status = finalServerStatus.getStatus();

            if (status.isUserRecoverableError()) {
                inAppBars.add(finalServerStatus);
            }

            if (status.isNonRecoverableError()) {
                switch (status) {
                    case MAINTENANCE:
                        errorCallback.accept(SERVER_MAINTENANCE_ERROR);
                        break;
                    case OUTDATED:
                        errorCallback.accept(APP_OUTDATED_ERROR);
                        break;
                }
            }

            if (settingsManager.getPreference(PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !finalServerStatus.checkIfAppIsUpToDate()) {
                inAppBars.add(new Banner() {

                    @Override
                    public CharSequence getBannerText(Context context) {
                        //noinspection deprecation Suggested fix requires API level 24, which is too new for this app, or an ugly if-else statement.
                        return Html.fromHtml(String.format(context.getString(R.string.new_app_version), finalServerStatus.getLatestAppVersion()));
                    }

                    @Override
                    public int getColor(Context context) {
                        return ContextCompat.getColor(context, R.color.holo_green_light);
                    }
                });
            }
            callback.accept(inAppBars);
        }));
    }

    public void getInstallGuidePage(@NonNull Long deviceId, @NonNull Long updateMethodId, @NonNull Integer pageNumber, Consumer<InstallGuideData> callback) {
        new GetSingleAsync<>(ServerRequest.INSTALL_GUIDE, callback, deviceId.toString(), updateMethodId.toString(), pageNumber.toString()).execute();
    }

    private void getMostRecentOxygenOTAUpdate(@NonNull Long deviceId, @NonNull Long updateMethodId, Consumer<OxygenOTAUpdate> callback) {
        new GetSingleAsync<>(ServerRequest.MOST_RECENT_UPDATE_DATA, callback, deviceId.toString(), updateMethodId.toString()).execute();
    }

    private void getServerStatus(Consumer<ServerStatus> callback) {
        new GetSingleAsync<>(ServerRequest.SERVER_STATUS, callback).execute();
    }

    private void getServerMessages(@NonNull Long deviceId, @NonNull Long updateMethodId, Consumer<List<ServerMessage>> callback) {
        new GetMultipleAsync<>(ServerRequest.SERVER_MESSAGES, callback, deviceId.toString(), updateMethodId.toString()).execute();
    }

    private class GetMultipleAsync<T> extends AsyncTask<Void, Void, List<T>> {

        private final ServerRequest serverRequest;
        private final Consumer<List<T>> callback;
        private final String[] params;

        GetMultipleAsync(ServerRequest serverRequest, Consumer<List<T>> callback, String... params) {
            this.serverRequest = serverRequest;
            this.params = params;
            this.callback = callback;
        }

        @Override
        protected List<T> doInBackground(Void... voids) {
            return findMultipleFromServerResponse(serverRequest, params);
        }

        @Override
        protected void onPostExecute(List<T> results) {
            if(callback != null) callback.accept(results);
        }
    }


    private class GetSingleAsync<E> extends AsyncTask<Void, Void, E> {

        private final ServerRequest serverRequest;
        private final Consumer<E> callback;
        private final String[] params;

        GetSingleAsync(@NonNull ServerRequest serverRequest, @Nullable Consumer<E> callback, String... params) {
            this.serverRequest = serverRequest;
            this.params = params;
            this.callback = callback;
        }

        @Override
        protected E doInBackground(Void... voids) {
            return findOneFromServerResponse(serverRequest, params);
        }

        @Override
        protected void onPostExecute(E result) {
            if(callback != null) callback.accept(result);
        }

    }

    private <T> List<T> findMultipleFromServerResponse(ServerRequest serverRequest, String... params) {
        try {
            return objectMapper.readValue(fetchDataFromServer(serverRequest, params), objectMapper.getTypeFactory().constructCollectionType(List.class, serverRequest.getReturnClass()));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private <T> T findOneFromServerResponse(ServerRequest serverRequest, String... params) {
        try {
            return objectMapper.readValue(fetchDataFromServer(serverRequest, params), objectMapper.getTypeFactory().constructType(serverRequest.getReturnClass()));
        } catch(Exception e) {
            return null;
        }
    }

    private String fetchDataFromServer(ServerRequest request, String... params) {

        try {
            URL requestUrl = request.getURL(params);

            Log.v(TAG, "Performing GET request to URL " + requestUrl.toString());
            Log.v(TAG, "Timeout is set to "  + request.getTimeOutInSeconds() + " seconds.");

            HttpURLConnection urlConnection = (HttpURLConnection) requestUrl.openConnection();

            int timeOutInMilliseconds = request.getTimeOutInSeconds() * 1000;

            //setup request
            urlConnection.setRequestProperty(USER_AGENT_TAG, APP_USER_AGENT);
            urlConnection.setConnectTimeout(timeOutInMilliseconds);
            urlConnection.setReadTimeout(timeOutInMilliseconds);

            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            String rawResponse = response.toString();
            Log.v(TAG, "Response: " + rawResponse);
            return rawResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

