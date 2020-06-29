package com.oxygenupdater.models

import com.oxygenupdater.adapters.ChooserOnboardingAdapter
import com.oxygenupdater.fragments.ChooserOnboardingFragment

/**
 * Mainly used in [ChooserOnboardingFragment] and [ChooserOnboardingAdapter]. Used to
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
interface SelectableModel {
    val id: Long
    val name: String?
}
