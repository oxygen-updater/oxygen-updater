package com.arjanvlek.oxygenupdater.extensions

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Convenient extension to set a drawable and a tint for an [ImageView]
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
fun ImageView.setImageResourceWithTint(@DrawableRes drawableResId: Int, @ColorRes colorResId: Int) {
    setImageResource(drawableResId)
    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, colorResId))
}
