package com.oxygenupdater.extensions

import android.content.res.ColorStateList
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.annotation.AnimRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Convenient extension to set a drawable and a tint for an [ImageView]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun ImageView.setImageResourceWithTint(
    @DrawableRes drawableResId: Int,
    @ColorRes colorResId: Int
) {
    setImageResource(drawableResId)
    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, colorResId))
}

/**
 * Convenient extension to set a drawable after an animation for an [ImageView]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun ImageView.setImageResourceWithAnimation(
    @DrawableRes drawableResId: Int,
    @AnimRes animResId: Int
) {
    startAnimation(AnimationUtils.loadAnimation(context, animResId))
    setImageResource(drawableResId)
}

/**
 * Convenient extension to set a drawable after an animation for an [ImageView],
 * as well as apply a tint
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun ImageView.setImageResourceWithAnimationAndTint(
    @DrawableRes drawableResId: Int,
    @AnimRes animResId: Int,
    @ColorRes colorResId: Int? = null
) {
    setImageResourceWithAnimation(drawableResId, animResId)
    imageTintList = if (colorResId == null) {
        null
    } else {
        ColorStateList.valueOf(ContextCompat.getColor(context!!, colorResId))
    }
}
