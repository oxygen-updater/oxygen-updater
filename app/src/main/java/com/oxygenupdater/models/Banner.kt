package com.oxygenupdater.models

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

// TODO(compose): consider removing entirely
interface Banner {
    // TODO(compose): remove context requirement and replace with stringResource
    fun getBannerText(context: Context): CharSequence?

    // TODO(compose): remove context requirement and replace with MaterialTheme.colors
    @ColorInt
    fun getColor(context: Context): Int

    // TODO(compose): remove context requirement
    @DrawableRes
    fun getDrawableRes(context: Context): Int
}
