package com.arjanvlek.oxygenupdater.Server;


import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;

import java.util.List;

public class ProcessedServerResult {
    private final OxygenOTAUpdate oxygenOTAUpdate;
    private final boolean online;
    private final List<Banner> serverMessageBars;

    public ProcessedServerResult(OxygenOTAUpdate oxygenOTAUpdate, boolean online, List<Banner> serverMessageBars) {
        this.oxygenOTAUpdate = oxygenOTAUpdate;
        this.online = online;
        this.serverMessageBars = serverMessageBars;
    }

    public List<Banner> getServerMessageBars() {
        return serverMessageBars;
    }

    public boolean isOnline() {
        return online;
    }

    public OxygenOTAUpdate getOxygenOTAUpdate() {
        return oxygenOTAUpdate;
    }
}
