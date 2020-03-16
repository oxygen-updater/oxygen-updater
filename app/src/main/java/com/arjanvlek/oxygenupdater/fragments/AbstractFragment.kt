package com.arjanvlek.oxygenupdater.fragments

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import org.koin.android.ext.android.inject

abstract class AbstractFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {
    val settingsManager by inject<SettingsManager>()
}
