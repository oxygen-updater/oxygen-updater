package com.oxygenupdater.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.oxygenupdater.BuildConfig

data class Device(
    override val id: Long,
    var enabled: Boolean,
    override val name: String?,

    @JsonIgnore
    private val productName: String,
) : SelectableModel {

    init {
        setProductName(productName)
    }

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
        return productNameTemplate.trim().split(",")
            // Remove spaces after comma separation.
            .map { productName -> productName.trim() }
    }

    companion object {
        private val IMAGE_URL_PREFIX = buildString(38) {
            append("https://")
            if (BuildConfig.DEBUG) append("test.")
            append("oxygenupdater.com/img/device/")
        }

        private const val IMAGE_URL_SUFFIX = "-min.png?v=1"

        fun constructImageUrl(deviceName: String) = IMAGE_URL_PREFIX +
                deviceName.split("(", limit = 2)[0].trim().replace(' ', '-').lowercase() +
                IMAGE_URL_SUFFIX
    }
}
