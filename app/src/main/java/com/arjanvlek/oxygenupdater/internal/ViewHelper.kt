package com.arjanvlek.oxygenupdater.internal

import android.view.View

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
object ViewHelper {

    fun clear(view: View) {
        view.alpha = 1f

        view.scaleY = 1f
        view.scaleX = 1f

        view.translationY = 0f
        view.translationX = 0f

        view.rotation = 0f
        view.rotationY = 0f
        view.rotationX = 0f

        view.pivotY = (view.measuredHeight shr 2.toFloat().toInt()).toFloat()
        view.pivotX = (view.measuredWidth shr 2.toFloat().toInt()).toFloat()

        view.animate().setInterpolator(null).startDelay = 0
    }
}
