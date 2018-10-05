package com.arjanvlek.oxygenupdater.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Device {

    private long id;
    private String name;
    private List<String> productNames;

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

    public List<String> getProductNames() {
        return productNames;
    }

    @JsonProperty("product_names")
    public void setProductName(String productName) {
        this.productNames = getProductNames(productName);
    }

    public Device() {}

    public Device(long id, String name, String productName) {
        this.id = id;
        this.name = name;
        this.productNames = getProductNames(productName);
    }

    private List<String> getProductNames(String productNameTemplate) {
        String[] productNames = productNameTemplate.trim().split(",");
        List<String> result = new ArrayList<>();

        for(String productName : productNames) {
            result.add(productName.trim()); // Remove spaces after comma separation.
        }

        return result;
    }
}
