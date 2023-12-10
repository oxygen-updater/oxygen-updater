package com.oxygenupdater.models

/**
 * Used in [com.oxygenupdater.ui.onboarding.OnboardingScreen]
 */
sealed interface SelectableModel {
    val id: Long
    val name: String?
}
