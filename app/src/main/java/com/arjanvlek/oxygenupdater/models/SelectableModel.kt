package com.arjanvlek.oxygenupdater.models

import com.arjanvlek.oxygenupdater.adapters.ChooserOnboardingAdapter
import com.arjanvlek.oxygenupdater.setupwizard.ChooserOnboardingFragment

/**
 * Mainly used in [ChooserOnboardingFragment] and [ChooserOnboardingAdapter]. Used to
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
interface SelectableModel {
    val id: Long
    val name: String?
}
