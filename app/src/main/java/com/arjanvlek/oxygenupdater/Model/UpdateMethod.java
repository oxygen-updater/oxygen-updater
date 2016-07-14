package com.arjanvlek.oxygenupdater.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateMethod {

    private long id;
    private String updateMethod;
    private String updateMethodNl;
    private boolean recommended;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonProperty("update_method")
    public String getUpdateMethod() {
        return updateMethod;
    }

    @JsonProperty("update_method")
    public void setUpdateMethod(String updateMethod) {
        this.updateMethod = updateMethod;
    }

    @JsonProperty("update_method_nl")
    public String getUpdateMethodNl() {
        return updateMethodNl;
    }

    @JsonProperty("update_method_nl")
    public void setUpdateMethodNl(String updateMethodNl) {
        this.updateMethodNl = updateMethodNl;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(String recommended) {
        this.recommended = (recommended != null && recommended.equals("1"));
    }
}
