package com.arjanvlek.oxygenupdater.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Device {

	private long id;
	private String name;
	private List<String> productNames;

	public Device(long id, String name, String productName) {
		this.id = id;
		this.name = name;
		productNames = getProductNames(productName);
	}

	@JsonProperty("product_names")
	public void setProductName(String productName) {
		productNames = getProductNames(productName);
	}

	private List<String> getProductNames(String productNameTemplate) {
		String[] productNames = productNameTemplate.trim().split(",");
		List<String> result = new ArrayList<>();

		for (String productName : productNames) {
			result.add(productName.trim()); // Remove spaces after comma separation.
		}

		return result;
	}
}
