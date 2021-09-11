package com.oxygenupdater.adapters

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.oxygenupdater.internal.ViewHelper

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Suppress("unused")
abstract class AnimationAdapter internal constructor(
    val wrappedAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mDuration = 225
    private var mInterpolator: Interpolator = LinearInterpolator()
    private var mLastPosition = -1
    private var isFirstOnly = true

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = wrappedAdapter.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) = wrappedAdapter.onBindViewHolder(holder, position).also {
        val bindingAdapterPosition = holder.bindingAdapterPosition

        if (!isFirstOnly || bindingAdapterPosition > mLastPosition) {
            getAnimators(holder.itemView).forEach {
                it.setDuration(mDuration.toLong()).start()
                it.interpolator = mInterpolator
            }

            mLastPosition = bindingAdapterPosition
        } else {
            ViewHelper.clear(holder.itemView)
        }
    }

    override fun getItemViewType(position: Int) = wrappedAdapter.getItemViewType(position)

    override fun getItemId(position: Int) = wrappedAdapter.getItemId(position)

    override fun getItemCount() = wrappedAdapter.itemCount

    override fun onViewRecycled(
        holder: RecyclerView.ViewHolder
    ) = wrappedAdapter.onViewRecycled(holder).also {
        super.onViewRecycled(holder)
    }

    override fun registerAdapterDataObserver(
        observer: AdapterDataObserver
    ) = super.registerAdapterDataObserver(observer).also {
        wrappedAdapter.registerAdapterDataObserver(observer)
    }

    override fun unregisterAdapterDataObserver(
        observer: AdapterDataObserver
    ) = super.unregisterAdapterDataObserver(observer).also {
        wrappedAdapter.unregisterAdapterDataObserver(observer)
    }

    protected abstract fun getAnimators(view: View?): Array<Animator>

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
