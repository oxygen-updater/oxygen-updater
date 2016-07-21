package com.arjanvlek.oxygenupdater.notifications;

import com.arjanvlek.oxygenupdater.ApplicationContext;

public class TopicSubscriptionData {

    private final ApplicationContext applicationContext;
    private final Long deviceId;
    private final Long updateMethodId;

    public TopicSubscriptionData(ApplicationContext applicationContext, Long deviceId, Long updateMethodId) {
        this.applicationContext = applicationContext;
        this.deviceId = deviceId;
        this.updateMethodId = updateMethodId;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getUpdateMethodId() {
        return updateMethodId;
    }
}
