package com.arjanvlek.oxygenupdater.Model;


import com.arjanvlek.oxygenupdater.Support.Callback;

import java.util.List;

public class ServerResult {

    private Callback callback;
    private List<ServerMessage> serverMessages;
    private ServerStatus serverStatus;
    private OxygenOTAUpdate updateData;

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public List<ServerMessage> getServerMessages() {
        return serverMessages;
    }

    public void setServerMessages(List<ServerMessage> serverMessages) {
        this.serverMessages = serverMessages;
    }

    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public OxygenOTAUpdate getUpdateData() {
        return updateData;
    }

    public void setUpdateData(OxygenOTAUpdate updateData) {
        this.updateData = updateData;
    }
}
