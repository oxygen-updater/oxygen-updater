package com.oxygenupdater.extensions

import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.annotation.AnimRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.oxygenupdater.R

/**
 * Convenient extension to set a drawable and a tint for an [ImageView]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun ImageView.setImageResourceWithTint(
    @DrawableRes drawableResId: Int,
    @ColorRes colorResId: Int
) {
    trySetImageResource(drawableResId)
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
    trySetImageResource(drawableResId)
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

/**
 * Fallback to [R.drawable.no_entry] if raster image somehow wasn't found. It
 * happens on older Android versions, probably 9 and below (API < 28).
 * Not sure what the cause is.
 */
private fun ImageView.trySetImageResource(
    @DrawableRes drawableResId: Int
) = try {
    setImageResource(drawableResId)
} catch (e: Resources.NotFoundException) {
    try {
        // Load a "no entry" sign to show that the image failed to load
        setImageResource(R.drawable.no_entry)
    } catch (e: Resources.NotFoundException) {
        // Ignore
    }
}
