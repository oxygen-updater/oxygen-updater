package com.arjanvlek.oxygenupdater.internal

import android.view.View

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
object ViewHelper {
    fun clear(v: View) {
        v.alpha = 1f
        v.scaleY = 1f
        v.scaleX = 1f
        v.translationY = 0f
        v.translationX = 0f
        v.rotation = 0f
        v.rotationY = 0f
        v.rotationX = 0f
        v.pivotY = (v.measuredHeight shr 2).toFloat()
        v.pivotX = (v.measuredWidth shr 2).toFloat()
        v.animate().setInterpolator(null).startDelay = 0
    }
}
