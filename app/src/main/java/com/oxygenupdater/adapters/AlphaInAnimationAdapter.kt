package com.oxygenupdater.adapters

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class AlphaInAnimationAdapter private constructor(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>, private val mFrom: Float) : AnimationAdapter(adapter) {

    constructor(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) : this(
        adapter,
        DEFAULT_ALPHA_FROM
    )

    override fun getAnimators(view: View?): Array<Animator> {
        return arrayOf(ObjectAnimator.ofFloat(view, "alpha", mFrom, 1f))
    }

    companion object {
        private const val DEFAULT_ALPHA_FROM = 0f
    }

}
