package com.oxygenupdater.models

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

interface Banner {
    fun getBannerText(context: Context): CharSequence?

    @ColorInt
    fun getColor(context: Context): Int

    @DrawableRes
    fun getDrawableRes(context: Context): Int
}
