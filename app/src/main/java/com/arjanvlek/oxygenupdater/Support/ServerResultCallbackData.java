package com.arjanvlek.oxygenupdater.Support;


import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;

import java.util.List;

public class ServerResultCallbackData {
    private final OxygenOTAUpdate oxygenOTAUpdate;
    private final boolean online;
    private final List<Banner> inAppBars;

    public ServerResultCallbackData(OxygenOTAUpdate oxygenOTAUpdate, boolean online, List<Banner> inAppBars) {
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
