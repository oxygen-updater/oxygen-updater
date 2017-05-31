package com.arjanvlek.oxygenupdater.internal.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerPostResult {

    private boolean success;
    private String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @JsonProperty("error_message")
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
