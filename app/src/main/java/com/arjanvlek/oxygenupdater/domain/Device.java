package com.arjanvlek.oxygenupdater.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Device {

    private long id;
    private String name;
    private String productName;

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

    public Device() {}

    public Device(long id, String name, String productName) {
        this.id = id;
        this.name = name;
        this.productName = productName;
    }
}
