package com.arjanvlek.oxygenupdater.views

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder

import com.arjanvlek.oxygenupdater.internal.ViewHelper

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
abstract class AnimationAdapter internal constructor(private val wrappedAdapter: Adapter<ViewHolder>) : Adapter<ViewHolder>() {
    private var mDuration = 225
    private var mInterpolator: Interpolator = LinearInterpolator()
    private var mLastPosition = -1

    private var isFirstOnly = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return wrappedAdapter.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        wrappedAdapter.onBindViewHolder(holder, position)

        val adapterPosition = holder.adapterPosition
        if (!isFirstOnly || adapterPosition > mLastPosition) {
            for (anim in getAnimators(holder.itemView)) {
                anim.setDuration(mDuration.toLong()).start()
                anim.interpolator = mInterpolator
            }
            mLastPosition = adapterPosition
        } else {
            ViewHelper.clear(holder.itemView)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return wrappedAdapter.getItemViewType(position)
    }

    override fun getItemId(position: Int): Long {
        return wrappedAdapter.getItemId(position)
    }

    override fun getItemCount(): Int {
        return wrappedAdapter.itemCount
    }

    override fun onViewRecycled(holder: ViewHolder) {
        wrappedAdapter.onViewRecycled(holder)
        super.onViewRecycled(holder)
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.registerAdapterDataObserver(observer)
        wrappedAdapter.registerAdapterDataObserver(observer)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        wrappedAdapter.unregisterAdapterDataObserver(observer)
    }

    protected abstract fun getAnimators(view: View): Array<Animator>

    fun setDuration(duration: Int) {
        mDuration = duration
    }

    fun setInterpolator(interpolator: Interpolator) {
        mInterpolator = interpolator
    }

    fun setStartPosition(start: Int) {
        mLastPosition = start
    }

    fun setFirstOnly(firstOnly: Boolean) {
        isFirstOnly = firstOnly
    }
}
