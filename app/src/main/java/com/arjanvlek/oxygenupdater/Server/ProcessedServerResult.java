package com.arjanvlek.oxygenupdater.Server;


import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;

import java.util.List;

public class ProcessedServerResult {
    private final OxygenOTAUpdate oxygenOTAUpdate;
    private final boolean online;
    private final List<Banner> inAppBars;

    public ProcessedServerResult(OxygenOTAUpdate oxygenOTAUpdate, boolean online, List<Banner> inAppBars) {
        this.oxygenOTAUpdate = oxygenOTAUpdate;
        this.online = online;
        this.inAppBars = inAppBars;
    }

    public List<Banner> getInAppBars() {
        return inAppBars;
    }

    public boolean isOnline() {
        return online;
    }

    public OxygenOTAUpdate getOxygenOTAUpdate() {
        return oxygenOTAUpdate;
    }
}
