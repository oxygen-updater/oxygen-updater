package com.arjanvlek.oxygenupdater.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerMessage {
    private long id;
    private String message;
    private String messageNl;
    private Long deviceId;
    private Long updateMethodId;
    private ServerMessagePriority priority;
    private boolean marquee;

    public enum ServerMessagePriority {
        LOW, MEDIUM, HIGH
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageNl() {
        return messageNl;
    }

    @JsonProperty("message_nl")
    public void setMessageNl(String messageNl) {
        this.messageNl = messageNl;
    }

    public long getDeviceId() {
        return deviceId;
    }

    @JsonProperty("device_id")
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public long getUpdateMethodId() {
        return updateMethodId;
    }

    @JsonProperty("update_method_id")
    public void setUpdateMethodId(Long updateMethodId) {
        this.updateMethodId = updateMethodId;
    }

    public ServerMessagePriority getPriority() {
        return priority;
    }

    public void setPriority(ServerMessagePriority priority) {
        this.priority = priority;
    }

    public boolean isMarquee() {
        return marquee;
    }

    public void setMarquee(String marquee) {
        this.marquee = marquee.equals("1");
    }

    public boolean isDeviceSpecific() {
        return deviceId != null;
    }

    public boolean isUpdateMethodSpecific() {
        return updateMethodId != null;
    }
}


