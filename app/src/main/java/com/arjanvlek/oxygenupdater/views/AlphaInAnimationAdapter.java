package com.arjanvlek.oxygenupdater.views;

import android.animation.Animator;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView.Adapter;

import static android.animation.ObjectAnimator.ofFloat;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public class AlphaInAnimationAdapter extends AnimationAdapter {

	private static final float DEFAULT_ALPHA_FROM = 0f;
	private final float mFrom;

	public AlphaInAnimationAdapter(Adapter adapter) {
		this(adapter, DEFAULT_ALPHA_FROM);
	}

	@SuppressWarnings("unchecked")
	private AlphaInAnimationAdapter(Adapter adapter, float from) {
		super(adapter);
		mFrom = from;
	}

	@Override
	protected Animator[] getAnimators(View view) {
		return new Animator[]{ofFloat(view, "alpha", mFrom, 1f)};
	}
}
