package com.arjanvlek.oxygenupdater.domain

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

class Device(private val id: Long, val name: String, productName: String) {

    @JsonProperty("product_names")
    var productNames: List<String>? = getProductNames(productName)

    fun setProductName(productName: String) {
        productNames = getProductNames(productName)
    }

    private fun getProductNames(productNameTemplate: String): List<String> {
        val productNames = productNameTemplate.trim { it <= ' ' }.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result = ArrayList<String>()

        for (productName in productNames) {
            result.add(productName.trim { it <= ' ' }) // Remove spaces after comma separation.
        }

        return result
    }
}
