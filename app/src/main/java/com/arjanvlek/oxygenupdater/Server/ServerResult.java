package com.arjanvlek.oxygenupdater.Server;

import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;

import java.util.List;

public class ServerResult {

    private List<ServerMessage> serverMessages;
    private ServerStatus serverStatus;
    private OxygenOTAUpdate updateData;

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
