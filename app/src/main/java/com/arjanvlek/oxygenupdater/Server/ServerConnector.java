package com.arjanvlek.oxygenupdater.Server;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.InstallGuideData;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.arjanvlek.oxygenupdater.Support.Callback;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.arjanvlek.oxygenupdater.ApplicationContext.APP_USER_AGENT;

public class ServerConnector {

    private final static String USER_AGENT_TAG = "User-Agent";

    private ObjectMapper objectMapper;

    private List<Device> devices;
    private LocalDateTime deviceFetchDate;

    public ServerConnector() {
        this.objectMapper = new ObjectMapper();
    }

    public void getDevices(Callback<List<Device>> callback) {
        getDevices(false, callback);
    }

    public void getDevices(boolean alwaysFetch, Callback<List<Device>> callback) {
        if (devices != null && deviceFetchDate != null && deviceFetchDate.plusMinutes(5).isAfter(LocalDateTime.now()) && !alwaysFetch) {
            Log.v("ServerConnector", "Used local cache to fetch devices...");
            callback.onActionPerformed(devices);
        }

        else {
            Log.v("ServerConnector", "Used remote server to fetch devices...");
            new GetMultipleAsync<Device>(ServerRequest.DEVICES, (devices) -> {
                this.devices = devices;
                this.deviceFetchDate = LocalDateTime.now();
                callback.onActionPerformed(devices);
            }).execute();
        }
    }

    public void getUpdateMethods(@NonNull Long deviceId, Callback<List<UpdateMethod>> callback) {
        new GetMultipleAsync<>(ServerRequest.UPDATE_METHODS, callback, deviceId.toString()).execute();
    }

    public void getAllUpdateMethods(Callback<List<UpdateMethod>> callback) {
        new GetMultipleAsync<>(ServerRequest.ALL_UPDATE_METHODS, callback).execute();
    }

    public void getOxygenOTAUpdate(@NonNull Long deviceId, @NonNull Long updateMethodId, @NonNull String incrementalSystemVersion, Callback<OxygenOTAUpdate> callback) {
        new GetSingleAsync<>(ServerRequest.UPDATE_DATA, callback, deviceId.toString(), updateMethodId.toString(), incrementalSystemVersion).execute();
    }

    public void getMostRecentOxygenOTAUpdate(@NonNull Long deviceId, @NonNull Long updateMethodId, Callback<OxygenOTAUpdate> callback) {
        new GetSingleAsync<>(ServerRequest.MOST_RECENT_UPDATE_DATA, callback, deviceId.toString(), updateMethodId.toString()).execute();
    }

    public void getServerStatus(Callback<ServerStatus> callback) {
        new GetSingleAsync<>(ServerRequest.SERVER_STATUS, callback).execute();
    }

    public void getServerMessages(@NonNull Long deviceId, @NonNull Long updateMethodId, Callback<List<ServerMessage>> callback) {
        new GetMultipleAsync<>(ServerRequest.SERVER_MESSAGES, callback, deviceId.toString(), updateMethodId.toString()).execute();
    }

    public void fetchInstallGuidePageFromServer(@NonNull Long deviceId, @NonNull Long updateMethodId, @NonNull Integer pageNumber, Callback<InstallGuideData> callback) {
        new GetSingleAsync<>(ServerRequest.INSTALL_GUIDE, callback, deviceId.toString(), updateMethodId.toString(), pageNumber.toString()).execute();
    }

    private class GetMultipleAsync<T> extends AsyncTask<Void, Void, List<T>> {

        private final ServerRequest serverRequest;
        private final Callback<List<T>> callback;
        private final String[] params;

        GetMultipleAsync(ServerRequest serverRequest, Callback<List<T>> callback, String... params) {
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
            if(callback != null) callback.onActionPerformed(results);
        }
    }


    private class GetSingleAsync<E> extends AsyncTask<Void, Void, E> {

        private final ServerRequest serverRequest;
        private final Callback<E> callback;
        private final String[] params;

        GetSingleAsync(@NonNull ServerRequest serverRequest, @Nullable Callback<E> callback, String... params) {
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
            if(callback != null) callback.onActionPerformed(result);
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
            return response.toString();
        } catch (Exception e) {
            return null;
        }
    }
}

