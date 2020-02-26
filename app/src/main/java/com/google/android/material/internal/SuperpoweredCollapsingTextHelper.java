/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.material.internal;

import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.math.MathUtils;
import androidx.core.text.TextDirectionHeuristicsCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.resources.CancelableFontCallback;
import com.google.android.material.resources.TextAppearance;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Helper class for rendering and animating collapsed title.
 *
 * @hide
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@RestrictTo(LIBRARY_GROUP)
@SuppressLint({"RestrictedApi", "RtlHardcoded"})
public final class SuperpoweredCollapsingTextHelper {

	// Pre-JB-MR2 doesn't support HW accelerated canvas scaled title so we will workaround it
	// by using our own texture
	@SuppressLint("ObsoleteSdkInt")
	private static final boolean USE_SCALING_TEXTURE = Build.VERSION.SDK_INT < 18;

	private static final boolean DEBUG_DRAW = false;
	@NonNull
	private static final Paint DEBUG_DRAW_PAINT;

	static {
		DEBUG_DRAW_PAINT = DEBUG_DRAW ? new Paint() : null;
		if (DEBUG_DRAW_PAINT != null) {
			DEBUG_DRAW_PAINT.setAntiAlias(true);
			DEBUG_DRAW_PAINT.setColor(Color.MAGENTA);
		}
	}

	private final View view;

	private boolean drawTitle;
	private float expandedFraction;

	@NonNull
	private final Rect expandedTitleBounds;
	@NonNull
	private final Rect expandedSubtitleBounds;
	@NonNull
	private final Rect collapsedBounds;
	@NonNull
	private final RectF currentTitleBounds;

	private int expandedTitleGravity = Gravity.CENTER_VERTICAL;
	private int expandedSubtitleGravity = Gravity.CENTER_VERTICAL;
	private int collapsedTitleGravity = Gravity.CENTER_VERTICAL;
	private int collapsedSubtitleGravity = Gravity.CENTER_VERTICAL;

	private float expandedTitleSize = 15;
	private float collapsedTitleSize = 15;
	private float expandedSubtitleSize = 15;
	private float collapsedSubtitleSize = 15;

	private ColorStateList expandedTitleColor;
	private ColorStateList collapsedTitleColor;
	private ColorStateList expandedSubtitleColor;
	private ColorStateList collapsedSubtitleColor;

	private float expandedTitleDrawY;
	private float expandedSubtitleDrawY;
	private float collapsedTitleDrawY;
	private float collapsedSubtitleDrawY;
	private float expandedTitleDrawX;
	private float expandedSubtitleDrawX;
	private float collapsedTitleDrawX;
	private float collapsedSubtitleDrawX;
	private float currentTitleDrawX;
	private float currentSubtitleDrawX;
	private float currentTitleDrawY;
	private float currentSubtitleDrawY;

	private Typeface collapsedTitleTypeface;
	private Typeface collapsedSubtitleTypeface;
	private Typeface expandedTitleTypeface;
	private Typeface expandedSubtitleTypeface;
	private Typeface currentTitleTypeface;
	private Typeface currentSubtitleTypeface;

	private CancelableFontCallback expandedTitleFontCallback;
	private CancelableFontCallback expandedSubtitleFontCallback;
	private CancelableFontCallback collapsedTitleFontCallback;
	private CancelableFontCallback collapsedSubtitleFontCallback;

	@Nullable
	private CharSequence title;
	@Nullable
	private CharSequence subtitle;
	@Nullable
	private CharSequence titleToDraw;
	@Nullable
	private CharSequence subtitleToDraw;
	@Nullable
	private CharSequence titleToDrawCollapsed;

	private boolean isRtl;
	private boolean useTexture;

	@Nullable
	private Bitmap expandedTitleTexture;
	@Nullable
	private Bitmap expandedSubtitleTexture;
	@Nullable
	private Bitmap collapsedTitleTexture;
	@Nullable
	private Bitmap collapsedSubtitleTexture;
	@Nullable
	private Bitmap crossSectionTitleTexture;
	private Paint titleTexturePaint;
	private Paint subtitleTexturePaint;

	private float titleScale;
	private float subtitleScale;
	private float currentTitleSize;
	private float currentSubtitleSize;
	private float collapsedTitleBlend;
	private float collapsedSubtitleBlend;
	private float expandedTitleBlend;
	private float expandedSubtitleBlend;

	private float expandedFirstLineDrawX;

	private int[] state;

	private boolean boundsChanged;

	@NonNull
	private final TextPaint titlePaint;
	@NonNull
	private final TextPaint titleTmpPaint;
	@NonNull
	private final TextPaint subtitlePaint;
	@NonNull
	private final TextPaint subtitleTmpPaint;

	private TimeInterpolator positionInterpolator;
	private TimeInterpolator textSizeInterpolator;

	private float collapsedTitleShadowRadius;
	private float collapsedTitleShadowDx;
	private float collapsedTitleShadowDy;
	private ColorStateList collapsedTitleShadowColor;

	private float expandedTitleShadowRadius;
	private float expandedTitleShadowDx;
	private float expandedTitleShadowDy;
	private ColorStateList expandedTitleShadowColor;

	private float collapsedSubtitleShadowRadius;
	private float collapsedSubtitleShadowDx;
	private float collapsedSubtitleShadowDy;
	private ColorStateList collapsedSubtitleShadowColor;

	private float expandedSubtitleShadowRadius;
	private float expandedSubtitleShadowDx;
	private float expandedSubtitleShadowDy;
	private ColorStateList expandedSubtitleShadowColor;

	private StaticLayout titleLayout;

	private int maxLines = 1;
	private float lineSpacingExtra = 0;
	private float lineSpacingMultiplier = 1;

	public SuperpoweredCollapsingTextHelper(View view) {
		this.view = view;

		titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
		titleTmpPaint = new TextPaint(titlePaint);
		subtitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
		subtitleTmpPaint = new TextPaint(subtitlePaint);

		collapsedBounds = new Rect();
		expandedTitleBounds = new Rect();
		expandedSubtitleBounds = new Rect();
		currentTitleBounds = new RectF();
	}

	public void setTextSizeInterpolator(TimeInterpolator interpolator) {
		textSizeInterpolator = interpolator;
		recalculate();
	}

	public void setPositionInterpolator(TimeInterpolator interpolator) {
		positionInterpolator = interpolator;
		recalculate();
	}

	public void setExpandedTitleSize(float textSize) {
		if (expandedTitleSize != textSize) {
			expandedTitleSize = textSize;
			recalculate();
		}
	}

	public void setExpandedSubtitleSize(float textSize) {
		if (expandedSubtitleSize != textSize) {
			expandedSubtitleSize = textSize;
			recalculate();
		}
	}

	public void setCollapsedTitleSize(float textSize) {
		if (collapsedTitleSize != textSize) {
			collapsedTitleSize = textSize;
			recalculate();
		}
	}

	public void setCollapsedSubtitleSize(float textSize) {
		if (collapsedSubtitleSize != textSize) {
			collapsedSubtitleSize = textSize;
			recalculate();
		}
	}

	public void setExpandedTitleColor(ColorStateList textColor) {
		if (expandedTitleColor != textColor) {
			expandedTitleColor = textColor;
			recalculate();
		}
	}

	public void setExpandedSubtitleColor(ColorStateList textColor) {
		if (expandedSubtitleColor != textColor) {
			expandedSubtitleColor = textColor;
			recalculate();
		}
	}

	public void setCollapsedTitleColor(ColorStateList textColor) {
		if (collapsedTitleColor != textColor) {
			collapsedTitleColor = textColor;
			recalculate();
		}
	}

	public void setCollapsedSubtitleColor(ColorStateList textColor) {
		if (collapsedSubtitleColor != textColor) {
			collapsedSubtitleColor = textColor;
			recalculate();
		}
	}

	public void setExpandedTitleBounds(int left, int top, int right, int bottom) {
		if (!rectEquals(expandedTitleBounds, left, top, right, bottom)) {
			expandedTitleBounds.set(left, top, right, bottom);
			boundsChanged = true;
			onBoundsChanged();
		}
	}

	public void setExpandedSubtitleBounds(int left, int top, int right, int bottom) {
		if (!rectEquals(expandedSubtitleBounds, left, top, right, bottom)) {
			expandedSubtitleBounds.set(left, top, right, bottom);
			boundsChanged = true;
			onBoundsChanged();
		}
	}

	public void setExpandedTitleBounds(@NonNull Rect bounds) {
		setExpandedTitleBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
	}

	public void setExpandedSubtitleBounds(@NonNull Rect bounds) {
		setExpandedSubtitleBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
	}

	public void setCollapsedBounds(int left, int top, int right, int bottom) {
		if (!rectEquals(collapsedBounds, left, top, right, bottom)) {
			collapsedBounds.set(left, top, right, bottom);
			boundsChanged = true;
			onBoundsChanged();
		}
	}

	public void setCollapsedBounds(@NonNull Rect bounds) {
		setCollapsedBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
	}

	public void getCollapsedTitleActualBounds(@NonNull RectF bounds, int labelWidth, int textGravity) {
		isRtl = calculateIsRtl(title);

		bounds.left = getCollapsedTitleLeftBound(labelWidth, textGravity);
		bounds.top = collapsedBounds.top;
		bounds.right = getCollapsedTitleRightBound(bounds, labelWidth, textGravity);
		bounds.bottom = collapsedBounds.top + getCollapsedTitleHeight();
	}

	public void getCollapsedSubtitleActualBounds(RectF bounds) {
		boolean isRtl = calculateIsRtl(subtitle);

		bounds.left = !isRtl ? collapsedBounds.left : collapsedBounds.right - calculateCollapsedSubtitleWidth();
		bounds.top = collapsedBounds.top;
		bounds.right = !isRtl ? bounds.left + calculateCollapsedSubtitleWidth() : collapsedBounds.right;
		bounds.bottom = collapsedBounds.top + getCollapsedSubtitleHeight();
	}

	private float getCollapsedTitleLeftBound(int width, int gravity) {
		if ((gravity & Gravity.END) == Gravity.END || (gravity & Gravity.RIGHT) == Gravity.RIGHT) {
			return isRtl ? collapsedBounds.left : (collapsedBounds.right - calculateCollapsedTitleWidth());
		} else if (gravity == Gravity.CENTER) {
			return width / 2f - calculateCollapsedTitleWidth() / 2;
		} else {
			return isRtl ? (collapsedBounds.right - calculateCollapsedTitleWidth()) : collapsedBounds.left;
		}
	}

	private float getCollapsedTitleRightBound(@NonNull RectF bounds, int width, int gravity) {
		if ((gravity & Gravity.END) == Gravity.END || (gravity & Gravity.RIGHT) == Gravity.RIGHT) {
			return isRtl ? (bounds.left + calculateCollapsedTitleWidth()) : collapsedBounds.right;
		} else if (gravity == Gravity.CENTER) {
			return width / 2f + calculateCollapsedTitleWidth() / 2;
		} else {
			return isRtl ? collapsedBounds.right : (bounds.left + calculateCollapsedTitleWidth());
		}
	}

	public float calculateCollapsedTitleWidth() {
		if (title == null) {
			return 0;
		}

		getTitlePaintCollapsed(titleTmpPaint);
		return titleTmpPaint.measureText(title, 0, title.length());
	}

	public float calculateCollapsedSubtitleWidth() {
		if (subtitle == null) {
			return 0;
		}

		getSubtitlePaintCollapsed(subtitleTmpPaint);
		return subtitleTmpPaint.measureText(subtitle, 0, subtitle.length());
	}

	public float getExpandedTitleHeight() {
		getTitlePaintExpanded(titleTmpPaint);
		// Return expanded height measured from the baseline.
		return -titleTmpPaint.ascent();
	}

	public float getExpandedSubtitleHeight() {
		getSubtitlePaintExpanded(subtitleTmpPaint);
		// Return expanded height measured from the baseline.
		return -subtitleTmpPaint.ascent();
	}

	public float getCollapsedTitleHeight() {
		getTitlePaintCollapsed(titleTmpPaint);
		// Return collapsed height measured from the baseline.
		return -titleTmpPaint.ascent();
	}

	public float getCollapsedSubtitleHeight() {
		getSubtitlePaintCollapsed(subtitleTmpPaint);
		// Return collapsed height measured from the baseline.
		return -subtitleTmpPaint.ascent();
	}

	private void getTitlePaintExpanded(@NonNull TextPaint textPaint) {
		textPaint.setTextSize(expandedTitleSize);
		textPaint.setTypeface(expandedTitleTypeface);
	}

	private void getSubtitlePaintExpanded(@NonNull TextPaint titlePaint) {
		titlePaint.setTextSize(expandedSubtitleSize);
		titlePaint.setTypeface(expandedSubtitleTypeface);
	}

	private void getTitlePaintCollapsed(@NonNull TextPaint textPaint) {
		textPaint.setTextSize(collapsedTitleSize);
		textPaint.setTypeface(collapsedTitleTypeface);
	}

	private void getSubtitlePaintCollapsed(@NonNull TextPaint titlePaint) {
		titlePaint.setTextSize(collapsedSubtitleSize);
		titlePaint.setTypeface(collapsedSubtitleTypeface);
	}

	void onBoundsChanged() {
		drawTitle = collapsedBounds.width() > 0
				&& collapsedBounds.height() > 0
				&& expandedTitleBounds.width() > 0
				&& expandedTitleBounds.height() > 0
				&& expandedSubtitleBounds.width() > 0
				&& expandedSubtitleBounds.height() > 0;
	}

	public void setExpandedTitleGravity(int gravity) {
		if ((gravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
			gravity |= GravityCompat.START;
		}

		if (expandedTitleGravity != gravity) {
			expandedTitleGravity = gravity;
			recalculate();
		}
	}

	public void setExpandedSubtitleGravity(int gravity) {
		if ((gravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
			gravity |= GravityCompat.START;
		}

		if (expandedSubtitleGravity != gravity) {
			expandedSubtitleGravity = gravity;
			recalculate();
		}
	}

	public int getExpandedTitleGravity() {
		return expandedTitleGravity;
	}

	public int getExpandedSubtitleGravity() {
		return expandedSubtitleGravity;
	}

	public void setCollapsedTitleGravity(int gravity) {
		if ((gravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
			gravity |= GravityCompat.START;
		}

		if (collapsedTitleGravity != gravity) {
			collapsedTitleGravity = gravity;
			recalculate();
		}
	}

	public void setCollapsedSubtitleGravity(int gravity) {
		if ((gravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
			gravity |= GravityCompat.START;
		}

		if (collapsedSubtitleGravity != gravity) {
			collapsedSubtitleGravity = gravity;
			recalculate();
		}
	}

	public int getCollapsedTitleGravity() {
		return collapsedTitleGravity;
	}

	public int getCollapsedSubtitleGravity() {
		return collapsedSubtitleGravity;
	}

	public void setExpandedTitleAppearance(int resId) {
		TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);

		if (textAppearance.textColor != null) {
			expandedTitleColor = textAppearance.textColor;
		}

		if (textAppearance.textSize != 0) {
			expandedTitleSize = textAppearance.textSize;
		}

		if (textAppearance.shadowColor != null) {
			expandedTitleShadowColor = textAppearance.shadowColor;
		}

		expandedTitleShadowDx = textAppearance.shadowDx;
		expandedTitleShadowDy = textAppearance.shadowDy;
		expandedTitleShadowRadius = textAppearance.shadowRadius;

		// Cancel pending async fetch, if any, and replace with a new one.
		if (expandedTitleFontCallback != null) {
			expandedTitleFontCallback.cancel();
		}

		expandedTitleFontCallback = new CancelableFontCallback(this::setExpandedTitleTypeface, textAppearance.getFallbackFont());
		textAppearance.getFontAsync(view.getContext(), expandedTitleFontCallback);

		recalculate();
	}

	public void setExpandedSubtitleAppearance(int resId) {
		TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);

		if (textAppearance.textColor != null) {
			expandedSubtitleColor = textAppearance.textColor;
		}

		if (textAppearance.textSize != 0) {
			expandedSubtitleSize = textAppearance.textSize;
		}

		if (textAppearance.shadowColor != null) {
			expandedSubtitleShadowColor = textAppearance.shadowColor;
		}

		expandedSubtitleShadowDx = textAppearance.shadowDx;
		expandedSubtitleShadowDy = textAppearance.shadowDy;
		expandedSubtitleShadowRadius = textAppearance.shadowRadius;

		// Cancel pending async fetch, if any, and replace with a new one.
		if (expandedSubtitleFontCallback != null) {
			expandedSubtitleFontCallback.cancel();
		}

		expandedSubtitleFontCallback = new CancelableFontCallback(this::setExpandedTitleTypeface, textAppearance.getFallbackFont());
		textAppearance.getFontAsync(view.getContext(), expandedSubtitleFontCallback);

		recalculate();
	}

	public void setCollapsedTitleAppearance(int resId) {
		TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);

		if (textAppearance.textColor != null) {
			collapsedTitleColor = textAppearance.textColor;
		}

		if (textAppearance.textSize != 0) {
			collapsedTitleSize = textAppearance.textSize;
		}

		if (textAppearance.shadowColor != null) {
			collapsedTitleShadowColor = textAppearance.shadowColor;
		}

		collapsedTitleShadowDx = textAppearance.shadowDx;
		collapsedTitleShadowDy = textAppearance.shadowDy;
		collapsedTitleShadowRadius = textAppearance.shadowRadius;

		// Cancel pending async fetch, if any, and replace with a new one.
		if (collapsedTitleFontCallback != null) {
			collapsedTitleFontCallback.cancel();
		}

		collapsedTitleFontCallback = new CancelableFontCallback(this::setCollapsedTitleTypeface, textAppearance.getFallbackFont());
		textAppearance.getFontAsync(view.getContext(), collapsedTitleFontCallback);

		recalculate();
	}

	public void setCollapsedSubtitleAppearance(int resId) {
		TextAppearance textAppearance = new TextAppearance(view.getContext(), resId);

		if (textAppearance.textColor != null) {
			collapsedSubtitleColor = textAppearance.textColor;
		}

		if (textAppearance.textSize != 0) {
			collapsedSubtitleSize = textAppearance.textSize;
		}

		if (textAppearance.shadowColor != null) {
			collapsedSubtitleShadowColor = textAppearance.shadowColor;
		}

		collapsedSubtitleShadowDx = textAppearance.shadowDx;
		collapsedSubtitleShadowDy = textAppearance.shadowDy;
		collapsedSubtitleShadowRadius = textAppearance.shadowRadius;

		// Cancel pending async fetch, if any, and replace with a new one.
		if (collapsedSubtitleFontCallback != null) {
			collapsedSubtitleFontCallback.cancel();
		}

		collapsedSubtitleFontCallback = new CancelableFontCallback(this::setCollapsedTitleTypeface, textAppearance.getFallbackFont());
		textAppearance.getFontAsync(view.getContext(), collapsedSubtitleFontCallback);

		recalculate();
	}

	public void setMaxLines(int maxLines) {
		if (maxLines != this.maxLines) {
			this.maxLines = maxLines;
			clearTexture();
			recalculate();
		}
	}

	public int getMaxLines() {
		return maxLines;
	}

	public void setLineSpacingExtra(float lineSpacingExtra) {
		if (lineSpacingExtra != this.lineSpacingExtra) {
			this.lineSpacingExtra = lineSpacingExtra;
			clearTexture();
			recalculate();
		}
	}

	public float getLineSpacingExtra() {
		return lineSpacingExtra;
	}

	public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
		if (lineSpacingMultiplier != this.lineSpacingMultiplier) {
			this.lineSpacingMultiplier = lineSpacingMultiplier;
			clearTexture();
			recalculate();
		}
	}

	public float getLineSpacingMultiplier() {
		return lineSpacingMultiplier;
	}

	public void setExpandedTitleTypeface(Typeface typeface) {
		if (setExpandedTitleTypefaceInternal(typeface)) {
			recalculate();
		}
	}

	public void setExpandedSubtitleTypeface(Typeface typeface) {
		if (setExpandedSubtitleTypefaceInternal(typeface)) {
			recalculate();
		}
	}

	public void setCollapsedTitleTypeface(Typeface typeface) {
		if (setCollapsedTitleTypefaceInternal(typeface)) {
			recalculate();
		}
	}

	public void setCollapsedSubtitleTypeface(Typeface typeface) {
		if (setCollapsedSubtitleTypefaceInternal(typeface)) {
			recalculate();
		}
	}

	public void setTitleTypefaces(Typeface typeface) {
		boolean collapsedFontChanged = setCollapsedTitleTypefaceInternal(typeface);
		boolean expandedFontChanged = setExpandedTitleTypefaceInternal(typeface);
		if (collapsedFontChanged || expandedFontChanged) {
			recalculate();
		}
	}

	public void setSubtitleTypefaces(Typeface typeface) {
		boolean collapsedFontChanged = setCollapsedSubtitleTypefaceInternal(typeface);
		boolean expandedFontChanged = setExpandedSubtitleTypefaceInternal(typeface);
		if (collapsedFontChanged || expandedFontChanged) {
			recalculate();
		}
	}

	@SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
	private boolean setExpandedTitleTypefaceInternal(Typeface typeface) {
		// Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
		// already updated one when async op comes back after a while.
		if (expandedTitleFontCallback != null) {
			expandedTitleFontCallback.cancel();
		}

		if (expandedTitleTypeface != typeface) {
			expandedTitleTypeface = typeface;
			return true;
		}

		return false;
	}

	@SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
	private boolean setExpandedSubtitleTypefaceInternal(Typeface typeface) {
		// Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
		// already updated one when async op comes back after a while.
		if (expandedSubtitleFontCallback != null) {
			expandedSubtitleFontCallback.cancel();
		}

		if (expandedSubtitleTypeface != typeface) {
			expandedSubtitleTypeface = typeface;
			return true;
		}

		return false;
	}

	@SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
	private boolean setCollapsedTitleTypefaceInternal(Typeface typeface) {
		// Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
		// already updated one when async op comes back after a while.
		if (collapsedTitleFontCallback != null) {
			collapsedTitleFontCallback.cancel();
		}

		if (collapsedTitleTypeface != typeface) {
			collapsedTitleTypeface = typeface;
			return true;
		}

		return false;
	}

	@SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
	private boolean setCollapsedSubtitleTypefaceInternal(Typeface typeface) {
		// Explicit Typeface setting cancels pending async fetch, if any, to avoid old font overriding
		// already updated one when async op comes back after a while.
		if (collapsedSubtitleFontCallback != null) {
			collapsedSubtitleFontCallback.cancel();
		}

		if (collapsedSubtitleTypeface != typeface) {
			collapsedSubtitleTypeface = typeface;
			return true;
		}

		return false;
	}

	public Typeface getExpandedTitleTypeface() {
		return expandedTitleTypeface != null ? expandedTitleTypeface : Typeface.DEFAULT;
	}

	public Typeface getExpandedSubtitleTypeface() {
		return expandedSubtitleTypeface != null ? expandedSubtitleTypeface : Typeface.DEFAULT;
	}

	public Typeface getCollapsedTitleTypeface() {
		return collapsedTitleTypeface != null ? collapsedTitleTypeface : Typeface.DEFAULT;
	}

	public Typeface getCollapsedSubtitleTypeface() {
		return collapsedSubtitleTypeface != null ? collapsedSubtitleTypeface : Typeface.DEFAULT;
	}

	/**
	 * Set the value indicating the current scroll value. This decides how much of the background will
	 * be displayed, as well as the title metrics/positioning.
	 *
	 * <p>A value of {@code 0.0} indicates that the layout is fully expanded. A value of {@code 1.0}
	 * indicates that the layout is fully collapsed.
	 */
	public void setExpansionFraction(float fraction) {
		fraction = MathUtils.clamp(fraction, 0f, 1f);

		if (fraction != expandedFraction) {
			expandedFraction = fraction;
			calculateCurrentOffsets();
		}
	}

	public final boolean setState(int[] state) {
		this.state = state;

		if (isStateful()) {
			recalculate();
			return true;
		}

		return false;
	}

	public final boolean isStateful() {
		return (collapsedTitleColor != null && collapsedTitleColor.isStateful())
				|| (expandedTitleColor != null && expandedTitleColor.isStateful());
	}

	public float getExpansionFraction() {
		return expandedFraction;
	}

	public float getExpandedTitleSize() {
		return expandedTitleSize;
	}

	public float getExpandedSubtitleSize() {
		return expandedSubtitleSize;
	}

	public float getCollapsedTitleSize() {
		return collapsedTitleSize;
	}

	public float getCollapsedSubtitleSize() {
		return collapsedSubtitleSize;
	}

	private void calculateCurrentOffsets() {
		calculateOffsets(expandedFraction);
	}

	private void calculateOffsets(float fraction) {
		interpolateBounds(fraction);

		currentTitleDrawX = lerp(expandedTitleDrawX, collapsedTitleDrawX, fraction, positionInterpolator);
		currentTitleDrawY = lerp(expandedTitleDrawY, collapsedTitleDrawY, fraction, positionInterpolator);
		currentSubtitleDrawX = lerp(expandedSubtitleDrawX, collapsedSubtitleDrawX, fraction, positionInterpolator);
		currentSubtitleDrawY = lerp(expandedSubtitleDrawY, collapsedSubtitleDrawY, fraction, positionInterpolator);

		setInterpolatedTitleSize(lerp(expandedTitleSize, collapsedTitleSize, fraction, textSizeInterpolator));
		setInterpolatedSubtitleSize(lerp(expandedSubtitleSize, collapsedSubtitleSize, fraction, textSizeInterpolator));

		setCollapsedTitleBlend(1 - lerp(0, 1, 1 - fraction, AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR));
		setExpandedTitleBlend(lerp(1, 0, fraction, AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR));

		if (collapsedTitleColor != expandedTitleColor) {
			// If the collapsed and expanded title colors are different, blend them based on the fraction
			titlePaint.setColor(blendColors(getCurrentExpandedTitleColor(), getCurrentCollapsedTitleColor(), fraction));
		} else {
			titlePaint.setColor(getCurrentCollapsedTitleColor());
		}

		titlePaint.setShadowLayer(
				lerp(expandedTitleShadowRadius, collapsedTitleShadowRadius, fraction, null),
				lerp(expandedTitleShadowDx, collapsedTitleShadowDx, fraction, null),
				lerp(expandedTitleShadowDy, collapsedTitleShadowDy, fraction, null),
				blendColors(getCurrentColor(expandedTitleShadowColor), getCurrentColor(collapsedTitleShadowColor), fraction)
		);

		if (collapsedSubtitleColor != expandedSubtitleColor) {
			// If the collapsed and expanded subtitle colors are different, blend them based on the fraction
			subtitlePaint.setColor(blendColors(getCurrentExpandedSubtitleColor(), getCurrentCollapsedSubtitleColor(), fraction));
		} else {
			subtitlePaint.setColor(getCurrentCollapsedSubtitleColor());
		}

		subtitlePaint.setShadowLayer(
				lerp(expandedSubtitleShadowRadius, collapsedSubtitleShadowRadius, fraction, null),
				lerp(expandedSubtitleShadowDx, collapsedSubtitleShadowDx, fraction, null),
				lerp(expandedSubtitleShadowDy, collapsedSubtitleShadowDy, fraction, null),
				blendColors(getCurrentColor(expandedSubtitleShadowColor), getCurrentColor(collapsedSubtitleShadowColor), fraction)
		);

		ViewCompat.postInvalidateOnAnimation(view);
	}

	@ColorInt
	private int getCurrentExpandedTitleColor() {
		return getCurrentColor(expandedTitleColor);
	}

	@ColorInt
	private int getCurrentExpandedSubtitleColor() {
		return getCurrentColor(expandedSubtitleColor);
	}

	@ColorInt
	public int getCurrentCollapsedTitleColor() {
		return getCurrentColor(collapsedTitleColor);
	}

	@ColorInt
	public int getCurrentCollapsedSubtitleColor() {
		return getCurrentColor(collapsedSubtitleColor);
	}

	@ColorInt
	private int getCurrentColor(@Nullable ColorStateList colorStateList) {
		if (colorStateList == null) {
			return 0;
		}

		if (state != null) {
			return colorStateList.getColorForState(state, 0);
		}

		return colorStateList.getDefaultColor();
	}

	private void calculateBaseOffsets() {
		float currentTitleSize = this.currentTitleSize;
		float currentSubtitleSize = this.currentSubtitleSize;
		boolean isTitleOnly = TextUtils.isEmpty(subtitle);

		// We then calculate the collapsed title size, using the same logic
		calculateUsingTitleSize(collapsedTitleSize);
		// We then calculate the collapsed subtitle size, using the same logic
		calculateUsingSubtitleSize(collapsedSubtitleSize);

		titleToDrawCollapsed = titleToDraw;

		float titleWidth = titleToDrawCollapsed != null
				? titlePaint.measureText(titleToDrawCollapsed, 0, titleToDrawCollapsed.length())
				: 0;
		float subtitleWidth = subtitleToDraw != null
				? subtitlePaint.measureText(subtitleToDraw, 0, subtitleToDraw.length())
				: 0;
		int collapsedAbsGravity = GravityCompat.getAbsoluteGravity(
				collapsedTitleGravity,
				isRtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR
		);

		float titleHeight = titleLayout != null ? titleLayout.getHeight() : 0;
		float titleOffset = titleHeight / 2;
		float subtitleHeight = subtitlePaint.descent() - subtitlePaint.ascent();
		float subtitleOffset = subtitleHeight / 2 - subtitlePaint.descent();

		if (isTitleOnly) {
			switch (collapsedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.BOTTOM:
					collapsedTitleDrawY = collapsedBounds.bottom - titleHeight;
					break;
				case Gravity.TOP:
					collapsedTitleDrawY = collapsedBounds.top;
					break;
				case Gravity.CENTER_VERTICAL:
				default:
					collapsedTitleDrawY = collapsedBounds.centerY() - titleOffset;
					break;
			}
		} else {
			float offset = (collapsedBounds.height() - (titleHeight + subtitleHeight)) / 3;
			collapsedTitleDrawY = collapsedBounds.top + offset;
			collapsedSubtitleDrawY = collapsedBounds.top + offset * 2 + titleHeight - subtitlePaint.ascent();
		}

		switch (collapsedAbsGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
			case Gravity.CENTER_HORIZONTAL:
				collapsedTitleDrawX = collapsedBounds.centerX() - (titleWidth / 2);
				collapsedSubtitleDrawX = collapsedBounds.centerX() - (subtitleWidth / 2);
				break;
			case Gravity.RIGHT:
				collapsedTitleDrawX = collapsedBounds.right - titleWidth;
				collapsedSubtitleDrawX = collapsedBounds.right - subtitleWidth;
				break;
			case Gravity.LEFT:
			default:
				collapsedTitleDrawX = collapsedBounds.left;
				collapsedSubtitleDrawX = collapsedBounds.left;
				break;
		}

		calculateUsingTitleSize(expandedTitleSize);
		calculateUsingSubtitleSize(expandedSubtitleSize);

		if (isRtl) {
			// fallback for RTL
			titleWidth = titleToDrawCollapsed != null
					? titlePaint.measureText(titleToDrawCollapsed, 0, titleToDrawCollapsed.length())
					: 0;
		} else {
			titleWidth = titleLayout != null ? titleLayout.getLineWidth(0) : 0;
		}

		subtitleWidth = subtitleToDraw != null ? subtitlePaint.measureText(subtitleToDraw, 0, subtitleToDraw.length()) : 0;

		expandedFirstLineDrawX = titleLayout != null ? titleLayout.getLineLeft(0) : 0;

		titleHeight = titleLayout != null ? titleLayout.getHeight() : 0;
		titleOffset = titleHeight;
		subtitleHeight = subtitlePaint.descent() - subtitlePaint.ascent();
		subtitleOffset = subtitleHeight / 2 - subtitlePaint.descent();

		int expandedAbsGravity = GravityCompat.getAbsoluteGravity(
				expandedTitleGravity,
				isRtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR
		);

		titleHeight = titleLayout != null ? titleLayout.getHeight() : 0;

		if (isTitleOnly) {
			switch (expandedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.BOTTOM:
					expandedTitleDrawY = expandedTitleBounds.bottom - titleHeight + titlePaint.descent();
					break;
				case Gravity.TOP:
					expandedTitleDrawY = expandedTitleBounds.top;
					break;
				case Gravity.CENTER_VERTICAL:
				default:
					expandedTitleDrawY = expandedTitleBounds.centerY() - titleHeight / 2;
					break;
			}
		} else {
			switch (expandedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.BOTTOM:
					expandedTitleDrawY = expandedTitleBounds.bottom - subtitleHeight - titleOffset;
					expandedSubtitleDrawY = expandedSubtitleBounds.bottom;
					break;
				case Gravity.TOP:
					expandedTitleDrawY = expandedTitleBounds.top - titlePaint.ascent();
					expandedSubtitleDrawY = expandedTitleDrawY + subtitleHeight + titleOffset;
					break;
				case Gravity.CENTER_VERTICAL:
				default:
					expandedTitleDrawY = expandedTitleBounds.centerY() + titleOffset;
					expandedSubtitleDrawY = expandedTitleDrawY + subtitleHeight + titleOffset;
					break;
			}
		}

		switch (expandedAbsGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
			case Gravity.CENTER_HORIZONTAL:
				expandedTitleDrawX = expandedTitleBounds.centerX() - (titleWidth / 2);
				expandedSubtitleDrawX = expandedSubtitleBounds.centerX() - (subtitleWidth / 2);
				break;
			case Gravity.RIGHT:
				expandedTitleDrawX = expandedTitleBounds.right - titleWidth;
				expandedSubtitleDrawX = expandedSubtitleBounds.right - subtitleWidth;
				break;
			case Gravity.LEFT:
			default:
				expandedTitleDrawX = expandedTitleBounds.left;
				expandedSubtitleDrawX = expandedSubtitleBounds.left;
				break;
		}

		// The bounds have changed so we need to clear the texture
		clearTexture();

		// Now reset the title size back to the original
		setInterpolatedTitleSize(currentTitleSize);
		// Now reset the subtitle size back to the original
		setInterpolatedSubtitleSize(currentSubtitleSize);
	}

	private void interpolateBounds(float fraction) {
		currentTitleBounds.left = lerp(expandedTitleBounds.left, collapsedBounds.left, fraction, positionInterpolator);
		currentTitleBounds.top = lerp(expandedTitleDrawY, collapsedTitleDrawY, fraction, positionInterpolator);
		currentTitleBounds.right = lerp(expandedTitleBounds.right, collapsedBounds.right, fraction, positionInterpolator);
		currentTitleBounds.bottom = lerp(expandedTitleBounds.bottom, collapsedBounds.bottom, fraction, positionInterpolator);
	}

	public void draw(@NonNull Canvas canvas) {
		int titleSaveCount = canvas.save();

		if (titleToDraw != null && drawTitle) {
			float titleX = currentTitleDrawX;
			float titleY = currentTitleDrawY;
			float subtitleX = currentSubtitleDrawX;
			float subtitleY = currentSubtitleDrawY;

			boolean drawTexture = useTexture
					&& expandedTitleTexture != null
					&& collapsedTitleTexture != null
					&& crossSectionTitleTexture != null;

			float titleAscent;
			float titleDescent;
			float subtitleAscent;
			float subtitleDescent;

			// Update the TextPaint to the current title size
			titlePaint.setTextSize(currentTitleSize);

			if (drawTexture) {
				titleAscent = 0;
				titleDescent = titleLayout.getHeight() * titleScale;
				subtitleAscent = subtitlePaint.ascent() * subtitleScale;
				subtitleDescent = subtitlePaint.descent() * subtitleScale;
			} else {
				titleAscent = titlePaint.ascent() * titleScale;
				titleDescent = titlePaint.descent() * titleScale;
				subtitleAscent = subtitlePaint.ascent() * subtitleScale;
				subtitleDescent = subtitlePaint.descent() * subtitleScale;
			}

			if (drawTexture) {
				titleY += titleAscent;
				subtitleY += subtitleAscent;
			}

			if (DEBUG_DRAW) {
				// Just a debug tool, which drawn a magenta rect in the title bounds
				canvas.drawRect(currentTitleBounds.left, titleY, currentTitleBounds.right, titleY + titleDescent, DEBUG_DRAW_PAINT);
			}

			// IMPORTANT: separate canvas save for subtitle
			int subtitleSaveCount = canvas.save();
			if (!TextUtils.isEmpty(subtitle)) {
				if (subtitleScale != 1f) {
					canvas.scale(subtitleScale, subtitleScale, subtitleX, subtitleY);
				}

				if (drawTexture) {
					// If we should use a texture, draw it instead of title
					canvas.drawBitmap(expandedSubtitleTexture, subtitleX, subtitleY, subtitleTexturePaint);
				} else {
					canvas.drawText(subtitleToDraw, 0, subtitleToDraw.length(), subtitleX, subtitleY, subtitlePaint);
				}

				canvas.restoreToCount(subtitleSaveCount);
			}

			if (titleScale != 1f) {
				canvas.scale(titleScale, titleScale, titleX, titleY);
			}

			// Compute where to draw titleLayout for this frame
			float currentExpandedTitleX = currentTitleDrawX + titleLayout.getLineLeft(0) - expandedFirstLineDrawX * 2;
			if (drawTexture) {
				// If we should use a texture, draw it instead of title
				if (isRtl) {
					// fallback for RTL: draw only collapsed title
					titleTexturePaint.setAlpha(255);
					canvas.drawBitmap(collapsedTitleTexture, titleX, titleY, titleTexturePaint);
				} else {
					// Expanded title
					titleTexturePaint.setAlpha((int) (expandedTitleBlend * 255));
					canvas.drawBitmap(expandedTitleTexture, currentExpandedTitleX, titleY, titleTexturePaint);
					// Collapsed title
					titleTexturePaint.setAlpha((int) (collapsedTitleBlend * 255));
					canvas.drawBitmap(collapsedTitleTexture, titleX, titleY, titleTexturePaint);
					// Cross-section between both texts (should stay at alpha = 255)
					titleTexturePaint.setAlpha(255);
					canvas.drawBitmap(crossSectionTitleTexture, titleX, titleY, titleTexturePaint);
				}
			} else {
				if (isRtl) {
					// fallback for RTL: draw only collapsed title
					canvas.drawText(titleToDrawCollapsed, 0, titleToDrawCollapsed.length(), titleX, titleY - titleAscent / titleScale, titlePaint);
				} else {
					int originalAlpha = titlePaint.getAlpha();

					// position expanded title appropriately
					canvas.translate(currentExpandedTitleX, titleY);

					// Expanded title
					titlePaint.setAlpha((int) (expandedTitleBlend * originalAlpha));
					titleLayout.draw(canvas);

					// position the overlays
					canvas.translate(titleX - currentExpandedTitleX, 0);

					// Collapsed title
					titlePaint.setAlpha((int) (collapsedTitleBlend * originalAlpha));
					canvas.drawText(titleToDrawCollapsed, 0, titleToDrawCollapsed.length(), 0, -titleAscent / titleScale, titlePaint);

					// Remove ellipsis for Cross-section animation
					String tmp = titleToDrawCollapsed.toString().trim();
					if (tmp.endsWith("\u2026")) {
						tmp = tmp.substring(0, tmp.length() - 1);
					}

					// Cross-section between both texts (should stay at original alpha)
					titlePaint.setAlpha(originalAlpha);
					canvas.drawText(tmp, 0, Math.min(titleLayout.getLineEnd(0), tmp.length()), 0, -titleAscent / titleScale, titlePaint);
				}
			}
		}

		canvas.restoreToCount(titleSaveCount);
	}

	private boolean calculateIsRtl(@NonNull CharSequence text) {
		boolean defaultIsRtl = ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
		return (defaultIsRtl
				? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
				: TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR).isRtl(text, 0, text.length());
	}

	private void setInterpolatedTitleSize(float textSize) {
		calculateUsingTitleSize(textSize);

		// Use our texture if the titleScale isn't 1.0
		useTexture = USE_SCALING_TEXTURE && titleScale != 1f;

		if (useTexture) {
			// Make sure we have an expanded texture if needed
			ensureExpandedTitleTexture();
			ensureCollapsedTitleTexture();
			ensureCrossSectionTitleTexture();
		}

		ViewCompat.postInvalidateOnAnimation(view);
	}

	private void setInterpolatedSubtitleSize(float size) {
		calculateUsingSubtitleSize(size);

		// Use our texture if the scale isn't 1.0
		useTexture = USE_SCALING_TEXTURE && subtitleScale != 1f;

		if (useTexture) {
			// Make sure we have an expanded texture if needed
			ensureExpandedSubtitleTexture();
		}

		ViewCompat.postInvalidateOnAnimation(view);
	}

	private void setCollapsedTitleBlend(float blend) {
		collapsedTitleBlend = blend;
		ViewCompat.postInvalidateOnAnimation(view);
	}

	private void setExpandedTitleBlend(float blend) {
		expandedTitleBlend = blend;
		ViewCompat.postInvalidateOnAnimation(view);
	}

	@SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
	private void calculateUsingTitleSize(float size) {
		if (title == null) {
			return;
		}

		float collapsedWidth = collapsedBounds.width();
		float expandedWidth = expandedTitleBounds.width();

		float availableWidth;
		float newTextSize;
		boolean updateDrawText = false;
		int maxLines;

		if (isClose(size, collapsedTitleSize)) {
			newTextSize = collapsedTitleSize;
			titleScale = 1f;

			if (currentTitleTypeface != collapsedTitleTypeface) {
				currentTitleTypeface = collapsedTitleTypeface;
				updateDrawText = true;
			}

			availableWidth = collapsedWidth;
			maxLines = 1;
		} else {
			newTextSize = expandedTitleSize;

			if (currentTitleTypeface != expandedTitleTypeface) {
				currentTitleTypeface = expandedTitleTypeface;
				updateDrawText = true;
			}

			if (isClose(size, expandedTitleSize)) {
				// If we're close to the expanded title size, snap to it and use a titleScale of 1
				titleScale = 1f;
			} else {
				// Else, we'll scale down from the expanded title size
				titleScale = size / expandedTitleSize;
			}

			availableWidth = expandedWidth;

			// fallback for RTL: draw only one line
			maxLines = isRtl ? 1 : this.maxLines;
		}

		if (availableWidth > 0) {
			updateDrawText = (currentTitleSize != newTextSize) || boundsChanged || updateDrawText;
			currentTitleSize = newTextSize;
			boundsChanged = false;
		}

		if (titleToDraw == null || updateDrawText) {
			titlePaint.setTextSize(currentTitleSize);
			titlePaint.setTypeface(currentTitleTypeface);

			// Use linear title scaling if we're scaling the canvas
			titlePaint.setLinearText(titleScale != 1f);

			CharSequence truncatedText;

			StaticLayout layout = new StaticLayout(
					title,
					titlePaint,
					(int) availableWidth,
					Layout.Alignment.ALIGN_NORMAL,
					lineSpacingMultiplier,
					lineSpacingExtra,
					false
			);

			if (layout.getLineCount() > maxLines) {
				int lastLine = maxLines - 1;

				CharSequence textBefore = lastLine > 0 ? title.subSequence(0, layout.getLineEnd(lastLine - 1)) : "";
				CharSequence lineText = title.subSequence(layout.getLineStart(lastLine), layout.getLineEnd(lastLine));

				// if last char in line is space, move it behind the ellipsis
				CharSequence lineEnd = "";
				if (lineText.charAt(lineText.length() - 1) == ' ') {
					lineEnd = lineText.subSequence(lineText.length() - 1, lineText.length());
					lineText = lineText.subSequence(0, lineText.length() - 1);
				}

				// insert ellipsis character
				lineText = TextUtils.concat(lineText, "\u2026", lineEnd);

				// if the title is too long, truncate it
				CharSequence truncatedLineText = TextUtils.ellipsize(
						lineText,
						titlePaint,
						availableWidth,
						TextUtils.TruncateAt.END
				);
				truncatedText = TextUtils.concat(textBefore, truncatedLineText);
			} else {
				truncatedText = title;
			}

			if (!TextUtils.equals(truncatedText, titleToDraw)) {
				titleToDraw = truncatedText;
				isRtl = calculateIsRtl(titleToDraw);
			}

			Layout.Alignment alignment;

			// Don't rectify gravity for RTL languages, Layout.Alignment does it already.
			switch (expandedTitleGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
				case Gravity.CENTER_HORIZONTAL:
					alignment = Layout.Alignment.ALIGN_CENTER;
					break;
				case Gravity.RIGHT:
				case Gravity.END:
					alignment = Layout.Alignment.ALIGN_OPPOSITE;
					break;
				case Gravity.LEFT:
				case Gravity.START:
				default:
					alignment = Layout.Alignment.ALIGN_NORMAL;
					break;
			}

			titleLayout = new StaticLayout(
					titleToDraw,
					titlePaint,
					(int) availableWidth,
					alignment,
					lineSpacingMultiplier,
					lineSpacingExtra,
					false
			);
		}
	}

	@SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
	private void calculateUsingSubtitleSize(float size) {
		if (subtitle == null) {
			return;
		}

		float collapsedWidth = collapsedBounds.width();
		float expandedWidth = expandedSubtitleBounds.width();

		float availableWidth;
		float newTextSize;
		boolean updateDrawText = false;

		if (isClose(size, collapsedSubtitleSize)) {
			newTextSize = collapsedSubtitleSize;
			subtitleScale = 1f;

			if (currentSubtitleTypeface != collapsedSubtitleTypeface) {
				currentSubtitleTypeface = collapsedSubtitleTypeface;
				updateDrawText = true;
			}

			availableWidth = collapsedWidth;
		} else {
			newTextSize = expandedSubtitleSize;

			if (currentSubtitleTypeface != expandedSubtitleTypeface) {
				currentSubtitleTypeface = expandedSubtitleTypeface;
				updateDrawText = true;
			}

			if (isClose(size, expandedSubtitleSize)) {
				// If we're close to the expanded title size, snap to it and use a scale of 1
				subtitleScale = 1f;
			} else {
				// Else, we'll scale down from the expanded title size
				subtitleScale = size / expandedSubtitleSize;
			}

			float subtitleSizeRatio = collapsedSubtitleSize / expandedSubtitleSize;
			// This is the size of the expanded bounds when it is scaled to match the
			// collapsed title size
			float scaledDownWidth = expandedWidth * subtitleSizeRatio;

			if (scaledDownWidth > collapsedWidth) {
				// If the scaled down size is larger than the actual collapsed width, we need to
				// cap the available width so that when the expanded title scales down, it matches
				// the collapsed width
				availableWidth = Math.min(collapsedWidth / subtitleSizeRatio, expandedWidth);
			} else {
				// Otherwise we'll just use the expanded width
				availableWidth = expandedWidth;
			}
		}

		if (availableWidth > 0) {
			updateDrawText = (currentSubtitleSize != newTextSize) || boundsChanged || updateDrawText;
			currentSubtitleSize = newTextSize;
			boundsChanged = false;
		}

		if (subtitleToDraw == null || updateDrawText) {
			subtitlePaint.setTextSize(currentSubtitleSize);
			subtitlePaint.setTypeface(currentSubtitleTypeface);
			// Use linear title scaling if we're scaling the canvas
			subtitlePaint.setLinearText(subtitleScale != 1f);

			// If we don't currently have title to draw, or the title size has changed, ellipsize...
			CharSequence text = TextUtils.ellipsize(
					subtitle,
					subtitlePaint,
					availableWidth,
					TextUtils.TruncateAt.END
			);

			if (!TextUtils.equals(text, subtitleToDraw)) {
				subtitleToDraw = text;
				isRtl = calculateIsRtl(subtitleToDraw);
			}
		}
	}

	private void ensureExpandedTitleTexture() {
		if (expandedTitleTexture != null || expandedTitleBounds.isEmpty() || TextUtils.isEmpty(titleToDraw)) {
			return;
		}

		calculateOffsets(0f);

		int w = titleLayout.getWidth();
		int h = titleLayout.getHeight();

		if (w <= 0 || h <= 0) {
			return; // If the width or height are 0, return
		}

		expandedTitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		Canvas c = new Canvas(expandedTitleTexture);
		titleLayout.draw(c);

		if (titleTexturePaint == null) {
			// Make sure we have a paint
			titleTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		}
	}

	private void ensureExpandedSubtitleTexture() {
		if (expandedSubtitleTexture != null || expandedSubtitleBounds.isEmpty() || TextUtils.isEmpty(subtitleToDraw)) {
			return;
		}

		calculateOffsets(0f);

		int w = Math.round(subtitlePaint.measureText(subtitleToDraw, 0, subtitleToDraw.length()));
		int h = Math.round(subtitlePaint.descent() - subtitlePaint.ascent());

		if (w <= 0 || h <= 0) {
			return; // If the width or height are 0, return
		}

		expandedSubtitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		Canvas c = new Canvas(expandedSubtitleTexture);
		c.drawText(subtitleToDraw, 0, subtitleToDraw.length(), 0, h - subtitlePaint.descent(), subtitlePaint);

		if (subtitleTexturePaint == null) {
			// Make sure we have a paint
			subtitleTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		}
	}

	private void ensureCollapsedTitleTexture() {
		if (collapsedTitleTexture != null || collapsedBounds.isEmpty() || TextUtils.isEmpty(titleToDraw)) {
			return;
		}

		calculateOffsets(0f);

		int w = Math.round(titlePaint.measureText(titleToDraw, 0, titleToDraw.length()));
		int h = Math.round(titlePaint.descent() - titlePaint.ascent());

		if (w <= 0 && h <= 0) {
			return; // If the width or height are 0, return
		}

		collapsedTitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		Canvas c = new Canvas(collapsedTitleTexture);
		c.drawText(titleToDrawCollapsed, 0, titleToDrawCollapsed.length(), 0, -titlePaint.ascent() / titleScale, titlePaint);

		if (titleTexturePaint == null) {
			// Make sure we have a paint
			titleTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		}
	}

	private void ensureCrossSectionTitleTexture() {
		if (crossSectionTitleTexture != null || collapsedBounds.isEmpty() || TextUtils.isEmpty(titleToDraw)) {
			return;
		}

		calculateOffsets(0f);

		int w = Math.round(titlePaint.measureText(titleToDraw, titleLayout.getLineStart(0), titleLayout.getLineEnd(0)));
		int h = Math.round(titlePaint.descent() - titlePaint.ascent());

		if (w <= 0 && h <= 0) {
			return; // If the width or height are 0, return
		}

		crossSectionTitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(crossSectionTitleTexture);

		String tmp = titleToDrawCollapsed.toString().trim();
		if (tmp.endsWith("\u2026")) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}

		c.drawText(tmp, 0, Math.min(titleLayout.getLineEnd(0), tmp.length()), 0, -titlePaint.ascent() / titleScale, titlePaint);

		if (titleTexturePaint == null) {
			// Make sure we have a paint
			titleTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		}
	}

	public void recalculate() {
		if (view.getHeight() > 0 && view.getWidth() > 0) {
			// If we've already been laid out, calculate everything now otherwise we'll wait
			// until a layout
			calculateBaseOffsets();
			calculateCurrentOffsets();
		}
	}

	/**
	 * Set the title to display
	 *
	 * @param title the title
	 */
	public void setTitle(@Nullable CharSequence title) {
		if (title == null || !TextUtils.equals(this.title, title)) {
			this.title = title;
			titleToDraw = null;
			clearTexture();
			recalculate();
		}
	}

	/**
	 * Set the subtitle to display
	 *
	 * @param subtitle the subtitle
	 */
	public void setSubtitle(@Nullable CharSequence subtitle) {
		if (subtitle == null || !subtitle.equals(this.subtitle)) {
			this.subtitle = subtitle;
			subtitleToDraw = null;
			clearTexture();
			recalculate();
		}
	}

	@Nullable
	public CharSequence getTitle() {
		return title;
	}

	@Nullable
	public CharSequence getSubtitle() {
		return subtitle;
	}

	private void clearTexture() {
		if (expandedTitleTexture != null) {
			expandedTitleTexture.recycle();
			expandedTitleTexture = null;
		}

		if (expandedSubtitleTexture != null) {
			expandedSubtitleTexture.recycle();
			expandedSubtitleTexture = null;
		}

		if (collapsedTitleTexture != null) {
			collapsedTitleTexture.recycle();
			collapsedTitleTexture = null;
		}

		if (collapsedSubtitleTexture != null) {
			collapsedSubtitleTexture.recycle();
			collapsedSubtitleTexture = null;
		}

		if (crossSectionTitleTexture != null) {
			crossSectionTitleTexture.recycle();
			crossSectionTitleTexture = null;
		}
	}

	/**
	 * Returns true if {@code value} is 'close' to it's closest decimal value. Close is currently
	 * defined as it's difference being < 0.001.
	 */
	private static boolean isClose(float value, float targetValue) {
		return Math.abs(value - targetValue) < 0.001f;
	}

	public ColorStateList getExpandedTitleColor() {
		return expandedTitleColor;
	}

	public ColorStateList getExpandedSubtitleColor() {
		return expandedSubtitleColor;
	}

	public ColorStateList getCollapsedTitleColor() {
		return collapsedTitleColor;
	}

	public ColorStateList getCollapsedSubtitleColor() {
		return collapsedSubtitleColor;
	}

	/**
	 * Blend {@code color1} and {@code color2} using the given ratio.
	 *
	 * @param ratio of which to blend. 0.0 will return {@code color1}, 0.5 will give an even blend,
	 *              1.0 will return {@code color2}.
	 */
	private static int blendColors(int color1, int color2, float ratio) {
		float inverseRatio = 1f - ratio;
		float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
		float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
		float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
		float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
		return Color.argb((int) a, (int) r, (int) g, (int) b);
	}

	private static float lerp(float startValue, float endValue, float fraction, @Nullable TimeInterpolator interpolator) {
		if (interpolator != null) {
			fraction = interpolator.getInterpolation(fraction);
		}

		return AnimationUtils.lerp(startValue, endValue, fraction);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean rectEquals(@NonNull Rect r, int left, int top, int right, int bottom) {
		return !(r.left != left || r.top != top || r.right != right || r.bottom != bottom);
	}
}
