package com.arjanvlek.oxygenupdater.Model;

import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateData {

    private Long id;
    private String versionNumber;
    private String otaVersionNumber;
    private String description;
    private String downloadUrl;
    private int downloadSize;
    private String filename;
    private String MD5Sum;
    private String information;
    private boolean updateInformationAvailable;
    private boolean systemIsUpToDate;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    @JsonProperty("version_number")
    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }


    public String getOtaVersionNumber() {
        return otaVersionNumber;
    }

    @JsonProperty("ota_version_number")
    public void setOtaVersionNumber(String otaVersionNumber) {
        this.otaVersionNumber = otaVersionNumber;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getDownloadUrl() {
        return downloadUrl;
    }

    @JsonProperty("download_url")
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }


    public int getDownloadSize() {
        return (downloadSize / 1048576 );
    }

    @JsonProperty("download_size")
    public void setDownloadSize(int downloadSize) {
        this.downloadSize = downloadSize;
    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    public String getMD5Sum() {
        return MD5Sum;
    }

    @JsonProperty("md5sum")
    public void setMD5Sum(String MD5Sum) {
        this.MD5Sum = MD5Sum;
    }


    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public boolean isUpdateInformationAvailable() {
        return updateInformationAvailable || (versionNumber != null);
    }

    @JsonProperty("update_information_available")
    public void setUpdateInformationAvailable(boolean updateInformationAvailable) {
        this.updateInformationAvailable = updateInformationAvailable;
    }

    public boolean isSystemIsUpToDateCheck(SettingsManager settingsManager) {
        //noinspection SimplifiableIfStatement
        if(settingsManager != null && settingsManager.getPreference(SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true)) {
            return systemIsUpToDate;
        } else {
            return false;
        }
    }

    public boolean isSystemIsUpToDate() {
        return systemIsUpToDate;
    }

    @JsonProperty("system_is_up_to_date")
    public void setSystemIsUpToDate(boolean systemIsUpToDate) {
        this.systemIsUpToDate = systemIsUpToDate;
    }

 }
