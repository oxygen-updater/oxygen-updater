package com.arjanvlek.oxygenupdater.views

import android.animation.Animator
import android.animation.ObjectAnimator.ofFloat
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class AlphaInAnimationAdapter private constructor(adapter: Adapter<RecyclerView.ViewHolder>,
                                                  private val mFrom: Float) : AnimationAdapter(adapter) {

    constructor(adapter: Adapter<RecyclerView.ViewHolder>) : this(adapter, DEFAULT_ALPHA_FROM)

    override fun getAnimators(view: View): Array<Animator> {
        return arrayOf(ofFloat(view, "alpha", mFrom, 1f))
    }

    companion object {
        private const val DEFAULT_ALPHA_FROM = 0f
    }
}
