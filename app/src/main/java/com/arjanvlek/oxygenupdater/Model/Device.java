package com.arjanvlek.oxygenupdater.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Device {

    private long id;
    private String name;
    private String productName;
    private String chipSet;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("product_name")
    public String getProductName() {
        return productName;
    }

    @JsonProperty("product_name")
    public void setProductName(String productName) {
        this.productName = productName;
    }

    @JsonProperty("chip_set")
    public String getChipSet() {
        return chipSet;
    }

    @JsonProperty("chip_set")
    public void setChipSet(String chipSet) {
        this.chipSet = chipSet;
    }
}
