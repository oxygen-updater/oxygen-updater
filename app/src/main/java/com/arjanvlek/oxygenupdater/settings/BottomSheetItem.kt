package com.arjanvlek.oxygenupdater.settings

import lombok.Builder
import lombok.Getter
import lombok.Setter

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@Getter
@Setter
@Builder
class BottomSheetItem {

    var title: String? = null
        set(title) {
            field = this.title
        }

    var subtitle: String? = null
        set(subtitle) {
            field = this.subtitle
        }

    var value: String? = null
        set(value) {
            field = this.value
        }

    var secondaryValue: Any? = null
        set(secondaryValue) {
            field = this.secondaryValue
        }
}
