package com.arjanvlek.oxygenupdater.installation.manual;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstallGuidePage {

    private Long id;
    private Long deviceId;
    private Long updateMethodId;
    private Integer pageNumber;
    private String fileExtension;
    private String imageUrl;
    private Boolean useCustomImage;
    private String englishTitle;
    private String dutchTitle;
    private String englishText;
    private String dutchText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    @JsonProperty(value = "device_id")
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getUpdateMethodId() {
        return updateMethodId;
    }

    @JsonProperty(value = "update_method_id")
    public void setUpdateMethodId(Long updateMethodId) {
        this.updateMethodId = updateMethodId;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    @JsonProperty(value = "page_number")
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    @JsonProperty(value = "file_extension")
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty(value = "image_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getUseCustomImage() {
        return useCustomImage;
    }

    @JsonProperty(value = "use_custom_image")
    public void setUseCustomImage(String useCustomImage) {
        this.useCustomImage = useCustomImage != null && useCustomImage.equals("1");
    }

    public String getEnglishTitle() {
        return englishTitle;
    }

    @JsonProperty(value = "title_en")
    public void setEnglishTitle(String englishTitle) {
        this.englishTitle = englishTitle;
    }

    public String getDutchTitle() {
        return dutchTitle;
    }

    @JsonProperty(value = "title_nl")
    public void setDutchTitle(String dutchTitle) {
        this.dutchTitle = dutchTitle;
    }

    public String getEnglishText() {
        return englishText;
    }

    @JsonProperty(value = "text_en")
    public void setEnglishText(String englishText) {
        this.englishText = englishText;
    }

    public String getDutchText() {
        return dutchText;
    }

    @JsonProperty(value = "text_nl")
    public void setDutchText(String dutchText) {
        this.dutchText = dutchText;
    }
}
