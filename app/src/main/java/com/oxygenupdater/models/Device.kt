package com.oxygenupdater.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class Device(
    override val id: Long,
    var enabled: Boolean,
    override val name: String?,

    @JsonIgnore
    private val productName: String
) : SelectableModel {

    lateinit var productNames: List<String>

    /**
     * Treat device as enabled by default, for backwards compatibility
     *
     * @param id          the device ID
     * @param name        the device name
     * @param productName the device name (ro.product.name)
     */
    constructor(id: Long, name: String?, productName: String) : this(id, true, name, productName)

    @JsonProperty("product_names")
    fun setProductName(productName: String) {
        productNames = getProductNames(productName)
    }

    private fun getProductNames(productNameTemplate: String): List<String> {
        return productNameTemplate.trim { it <= ' ' }.split(",")
            // Remove spaces after comma separation.
            .map { productName -> productName.trim { it <= ' ' } }
    }

    init {
        setProductName(productName)
    }
}
