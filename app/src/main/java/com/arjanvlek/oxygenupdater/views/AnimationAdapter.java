package com.arjanvlek.oxygenupdater.views;

import android.animation.Animator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.arjanvlek.oxygenupdater.internal.ViewHelper;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@SuppressWarnings("unused")
public abstract class AnimationAdapter extends Adapter<ViewHolder> {

	private final Adapter<ViewHolder> mAdapter;
	private int mDuration = 225;
	private Interpolator mInterpolator = new LinearInterpolator();
	private int mLastPosition = -1;

	private boolean isFirstOnly = true;

	AnimationAdapter(Adapter<ViewHolder> adapter) {
		mAdapter = adapter;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return mAdapter.onCreateViewHolder(parent, viewType);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		mAdapter.onBindViewHolder(holder, position);

		int adapterPosition = holder.getAdapterPosition();
		if (!isFirstOnly || adapterPosition > mLastPosition) {
			for (Animator anim : getAnimators(holder.itemView)) {
				anim.setDuration(mDuration).start();
				anim.setInterpolator(mInterpolator);
			}
			mLastPosition = adapterPosition;
		} else {
			ViewHelper.clear(holder.itemView);
		}
	}

	@Override
	public int getItemViewType(int position) {
		return mAdapter.getItemViewType(position);
	}

	@Override
	public long getItemId(int position) {
		return mAdapter.getItemId(position);
	}

	@Override
	public int getItemCount() {
		return mAdapter.getItemCount();
	}

	@Override
	public void onViewRecycled(@NonNull ViewHolder holder) {
		mAdapter.onViewRecycled(holder);
		super.onViewRecycled(holder);
	}

	@Override
	public void registerAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
		super.registerAdapterDataObserver(observer);
		mAdapter.registerAdapterDataObserver(observer);
	}

	@Override
	public void unregisterAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
		super.unregisterAdapterDataObserver(observer);
		mAdapter.unregisterAdapterDataObserver(observer);
	}

	protected abstract Animator[] getAnimators(View view);

	public void setDuration(int duration) {
		mDuration = duration;
	}

	public void setInterpolator(Interpolator interpolator) {
		mInterpolator = interpolator;
	}

	public void setStartPosition(int start) {
		mLastPosition = start;
	}

	public void setFirstOnly(boolean firstOnly) {
		isFirstOnly = firstOnly;
	}

	public Adapter<ViewHolder> getWrappedAdapter() {
		return mAdapter;
	}
}
