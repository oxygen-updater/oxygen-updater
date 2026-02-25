package com.oxygenupdater.models

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.internal.CsvList
import com.oxygenupdater.internal.ForceBoolean
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Immutable // required because we have a List field (productNames)
data class Device(
    override val id: Long = 0,
    override val name: String? = null,

    @JsonNames("product_names")
    val productNames: CsvList = listOf(),

    val enabled: ForceBoolean = true, // most are enabled, reasonable default
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
