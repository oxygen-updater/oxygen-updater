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
	private boolean enabled;
	private String name;
	private List<String> productNames;

	/**
	 * Treat device as enabled by default, for backwards compatibility
	 *
	 * @param id          the device ID
	 * @param name        the device name
	 * @param productName the device name (ro.product.name)
	 */
	public Device(long id, String name, String productName) {
		this(id, true, name, productName);
	}

	public Device(long id, boolean enabled, String name, String productName) {
		this.id = id;
		this.enabled = enabled;
		this.name = name;
		productNames = getProductNames(productName);
	}

	@JsonProperty("enabled")
	public void setEnabled(String enabled) {
		this.enabled = enabled != null && enabled.equals("1");
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
