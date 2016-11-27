package com.arjanvlek.oxygenupdater.notifications;

import com.arjanvlek.oxygenupdater.ApplicationContext;
import com.arjanvlek.oxygenupdater.Model.Device;
import com.arjanvlek.oxygenupdater.Model.UpdateMethod;

import java.util.List;

public class TopicSubscriptionData {

    private final ApplicationContext applicationContext;
    private final Long deviceId;
    private final Long updateMethodId;
    private List<Device> devices;
    private List<UpdateMethod> updateMethods;

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

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public List<UpdateMethod> getUpdateMethods() {
        return updateMethods;
    }

    public void setUpdateMethods(List<UpdateMethod> updateMethods) {
        this.updateMethods = updateMethods;
    }
}
