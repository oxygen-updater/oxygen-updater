package com.arjanvlek.oxygenupdater.Support;

import com.arjanvlek.oxygenupdater.Model.InstallGuideData;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.arjanvlek.oxygenupdater.ApplicationContext.APP_USER_AGENT;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.ALL_UPDATE_METHODS;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.INSTALL_GUIDE;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.MOST_RECENT_UPDATE_DATA;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.DEVICES;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.SERVER_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.SERVER_STATUS;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.UPDATE_DATA;
import static com.arjanvlek.oxygenupdater.Support.ServerRequest.UPDATE_METHODS;

public class ServerConnector {

    public final static String SERVER_URL = "https://oxygenupdater.com/api/v1/";
    public final static String TEST_SERVER_URL = "https://oxygenupdater.com/test/api/v1/";
    public final static String USER_AGENT_TAG = "User-Agent";

    private ObjectMapper objectMapper;

    public ServerConnector() {
        this.objectMapper = new ObjectMapper();
    }

    public List<Device> getDevices() {
        return findMultipleFromServerResponse(fetchDataFromServer(DEVICES, 20), Device.class);
    }

    public OxygenOTAUpdate getOxygenOTAUpdate(Long deviceId, Long updateMethodId, String incrementalSystemVersion) {
        return findOneFromServerResponse(fetchDataFromServer(UPDATE_DATA, 15, deviceId.toString(), updateMethodId.toString(), incrementalSystemVersion), OxygenOTAUpdate.class);
    }

    public OxygenOTAUpdate getMostRecentOxygenOTAUpdate(Long deviceId, Long updateMethodId) {
        return findOneFromServerResponse(fetchDataFromServer(MOST_RECENT_UPDATE_DATA, 10, deviceId.toString(), updateMethodId.toString()), OxygenOTAUpdate.class);
    }

    public List<UpdateMethod> getAllUpdateMethods() {
        return findMultipleFromServerResponse(fetchDataFromServer(ALL_UPDATE_METHODS, 20), UpdateMethod.class);
    }

    public List<UpdateMethod> getUpdateMethods(Long deviceId) {
        return findMultipleFromServerResponse(fetchDataFromServer(UPDATE_METHODS, 20, deviceId.toString()), UpdateMethod.class);
    }

    public ServerStatus getServerStatus() {
        return findOneFromServerResponse(fetchDataFromServer(SERVER_STATUS, 10), ServerStatus.class);
    }

    public List<ServerMessage> getServerMessages(Long deviceId, Long updateMethodId) {
        return findMultipleFromServerResponse(fetchDataFromServer(SERVER_MESSAGES, 20, deviceId.toString(), updateMethodId.toString()), ServerMessage.class);
    }

    public InstallGuideData fetchInstallGuidePageFromServer(Long deviceId, Long updateMethodId, Integer pageNumber) {
        return findOneFromServerResponse(fetchDataFromServer(INSTALL_GUIDE, 10, deviceId.toString(), updateMethodId.toString(), pageNumber.toString()), InstallGuideData.class);
    }

    private <T> List<T> findMultipleFromServerResponse(String response, Class<T> returnClass) {
        try {
            return objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, returnClass));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private <T> T findOneFromServerResponse(String response, Class<T> returnClass) {
        try {
            return objectMapper.readValue(response, returnClass);
        } catch(Exception e) {
            return null;
        }
    }

    private String fetchDataFromServer(ServerRequest request, int timeoutInSeconds, String... params) {

        try {
            URL requestUrl = request.getURL(params);

            HttpURLConnection urlConnection = (HttpURLConnection) requestUrl.openConnection();

            int timeOutInMilliseconds = timeoutInSeconds * 1000;

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

