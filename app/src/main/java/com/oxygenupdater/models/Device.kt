package com.oxygenupdater.models

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.CsvList
import com.oxygenupdater.internal.ForceBoolean
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable // required because we have a List field (productNames)
@JsonClass(generateAdapter = true)
data class Device(
    override val id: Long,
    override val name: String?,

    @Json(name = "product_names")
    @CsvList val productNames: List<String>,

    @ForceBoolean val enabled: Boolean,
) : SelectableModel {

    /**
     * Lazy computation to pick out the model number.
     *
     * Displayed in [com.oxygenupdater.ui.settings.DeviceChooser] via
     * [com.oxygenupdater.ui.dialogs.SelectableSheet] in a `LazyColumn`.
     */
    @Transient
    private var _subtitle: String? = null
    override val subtitle
        get() = _subtitle ?: productNames.let {
            var found: String? = null
            for (index in it.indices.reversed()) {
                val element = it[index]
                if (element.isEmpty()) continue

                // Does it look like a model number?
                // (starts with a letter, followed by numbers)
                if (element.matches("""[A-Z]+\d+""".toRegex())) {
                    found = element
                    break
                }
            }

            // Remove manufacturer names, if any
            return@let (found ?: it.lastOrNull())?.run {
                replace("oneplus", "", ignoreCase = true)
                    .replace("oppo", "", ignoreCase = true)
                    .replace("realme", "", ignoreCase = true)
                    .trim()
            }
        }.also { _subtitle = it }

    companion object {
        @VisibleForTesting
        val ImageUrlPrefix = buildString(37) {
            append(BuildConfig.SERVER_DOMAIN)
            append("img/device/")
        }

        @VisibleForTesting
        const val ImageUrlSuffix = "-min.png?v=1"

        fun constructImageUrl(deviceName: String) = ImageUrlPrefix +
                deviceName.split("(", limit = 2)[0].trim().replace(' ', '-').lowercase() +
                ImageUrlSuffix
    }
}
