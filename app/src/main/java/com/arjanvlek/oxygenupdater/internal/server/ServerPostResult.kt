package com.arjanvlek.oxygenupdater.internal.server

import com.fasterxml.jackson.annotation.JsonProperty

import lombok.Getter
import lombok.Setter

@Getter
@Setter
class ServerPostResult {

    var isSuccess: Boolean = false
        set(success) {
            field = isSuccess
        }

    @JsonProperty("error_message")
    var errorMessage: String? = null
        set(errorMessage) {
            field = this.errorMessage
        }
}
