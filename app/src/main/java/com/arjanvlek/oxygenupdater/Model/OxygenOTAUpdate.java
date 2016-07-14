package com.arjanvlek.oxygenupdater.Model;

import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OxygenOTAUpdate {

    private int size;
    private String downloadUrl;
    private String fileName;
    private String description;
    private String name;
    private String MD5Sum;
    private String information;
    private boolean updateInformationAvailable;
    private boolean systemIsUpToDate;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    @JsonProperty("download_url")
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    @JsonProperty("filename")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public boolean isUpdateInformationAvailable() {
        return updateInformationAvailable || (name != null);
    }

    @JsonProperty("update_information_available")
    public void setUpdateInformationAvailable(boolean updateInformationAvailable) {
        this.updateInformationAvailable = updateInformationAvailable;
    }

    public boolean isSystemIsUpToDate(SettingsManager settingsManager) {
        //noinspection SimplifiableIfStatement
        if(settingsManager != null && settingsManager.showIfSystemIsUpToDate()) {
            return systemIsUpToDate;
        } else {
            return false;
        }
    }

    public boolean isSystemIsUpToDateCheck() {
        return systemIsUpToDate;
    }

    @JsonProperty("system_is_up_to_date")
    public void setSystemIsUpToDate(boolean systemIsUpToDate) {
        this.systemIsUpToDate = systemIsUpToDate;
    }

    public String getMD5Sum() {
        return MD5Sum;
    }

    @JsonProperty("md5sum")
    public void setMD5Sum(String MD5Sum) {
        this.MD5Sum = MD5Sum;
    }
}
