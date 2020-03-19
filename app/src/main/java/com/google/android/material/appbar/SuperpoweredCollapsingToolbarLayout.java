package com.google.android.material.appbar;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.math.MathUtils;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arjanvlek.oxygenupdater.R;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.internal.DescendantOffsetUtils;
import com.google.android.material.internal.SuperpoweredCollapsingTextHelper;
import com.google.android.material.internal.ThemeEnforcement;

import org.jetbrains.annotations.NotNull;

import static com.google.android.material.theme.overlay.MaterialThemeOverlay.wrap;

/**
 * This class is a combination of <a href="https://github.com/hendraanggrian/collapsingtoolbarlayout-subtitle">collapsingtoolbarlayout-subtitle</a>
 * and <a href="https://github.com/opacapp/multiline-collapsingtoolbar">multiline-collapsingtoolbar</a>.
 * <p>
 * Note: multiline-collapsingtoolbar is not being maintained, and is based on Android Support library v27.
 * However, the developers of this library are trying to get their modifications merged into the official Material Components library.
 * See <a href="https://github.com/material-components/material-components-android/pull/413">this PR</a> for more details.
 * <p>
 * This class is mostly based on code from <a href="https://github.com/hendraanggrian/collapsingtoolbarlayout-subtitle/blob/master/collapsingtoolbarlayout-subtitle/src/com/google/android/material/appbar/SubtitleCollapsingToolbarLayout.java">SubtitleCollapsingToolbarLayout.java</a>, but I've taken inspiration from <a href="https://github.com/material-components/material-components-android/blob/217380e5c0b44727c9f451ae48faf6dfb96c5214/lib/java/com/google/android/material/appbar/CollapsingToolbarLayout.java">CollapsingToolbarLayout.java</a> as well
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@SuppressWarnings("unused")
@SuppressLint("RestrictedApi")
public class SuperpoweredCollapsingToolbarLayout extends FrameLayout {

	private static final int DEF_STYLE_RES = R.style.Widget_Design_SuperpoweredCollapsingToolbar;
	private static final int DEFAULT_SCRIM_ANIMATION_DURATION = 600;

	private final Rect tmpRect = new Rect();

	private boolean refreshToolbar = true;
	private boolean collapsingTitleEnabled;
	private boolean collapsingSubtitleEnabled;
	private boolean drawCollapsingTitle;
	private boolean scrimsAreShown;
	private boolean multiline;

	private long scrimAnimationDuration;

	private int toolbarId;
	private int scrimAlpha;
	private int scrimVisibleHeightTrigger = -1;
	private int expandedTitleMarginStart;
	private int expandedSubtitleMarginStart;
	private int expandedTitleMarginTop;
	private int expandedSubtitleMarginTop;
	private int expandedTitleMarginEnd;
	private int expandedSubtitleMarginEnd;
	private int expandedTitleMarginBottom;
	private int expandedSubtitleMarginBottom;

	int currentOffset;

	@Nullable
	private Drawable contentScrim;

	@Nullable
	Drawable statusBarScrim;

	@Nullable
	private Toolbar toolbar;

	@Nullable
	private View toolbarDirectChild;
	private View dummyView;

	@NonNull
	final SuperpoweredCollapsingTextHelper collapsingTextHelper;

	private AppBarLayout.OnOffsetChangedListener onOffsetChangedListener;

	private ValueAnimator scrimAnimator;

	@Nullable
	WindowInsetsCompat lastInsets;

	public SuperpoweredCollapsingToolbarLayout(@NonNull Context context) {
		this(context, null);
	}

	public SuperpoweredCollapsingToolbarLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SuperpoweredCollapsingToolbarLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(wrap(context, attrs, defStyleAttr, DEF_STYLE_RES), attrs, defStyleAttr);
		// Ensure we are using the correctly themed context rather than the context that was passed in.
		context = getContext();

		collapsingTextHelper = new SuperpoweredCollapsingTextHelper(this);
		collapsingTextHelper.setTextSizeInterpolator(AnimationUtils.DECELERATE_INTERPOLATOR);

		TypedArray a = ThemeEnforcement.obtainStyledAttributes(
				context,
				attrs,
				R.styleable.SuperpoweredCollapsingToolbarLayout,
				defStyleAttr,
				DEF_STYLE_RES
		);

		collapsingTextHelper.setExpandedTitleGravity(a.getInt(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleGravity,
				GravityCompat.START | Gravity.BOTTOM)
		);
		collapsingTextHelper.setCollapsedTitleGravity(a.getInt(R.styleable.SuperpoweredCollapsingToolbarLayout_collapsedTitleGravity,
				GravityCompat.START | Gravity.CENTER_VERTICAL)
		);

		expandedTitleMarginStart = expandedTitleMarginTop = expandedTitleMarginEnd = expandedTitleMarginBottom = a.getDimensionPixelSize(
				R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMargin,
				0
		);

		expandedSubtitleMarginStart = expandedSubtitleMarginTop = expandedSubtitleMarginEnd = expandedSubtitleMarginBottom = a.getDimensionPixelSize(
				R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMargin,
				0
		);

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginStart)) {
			expandedTitleMarginStart = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginStart, 0);
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginStart)) {
			expandedSubtitleMarginStart = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginStart, 0);
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginEnd)) {
			expandedTitleMarginEnd = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginEnd, 0);
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginEnd)) {
			expandedSubtitleMarginEnd = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginEnd, 0);
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginTop)) {
			expandedTitleMarginTop = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginTop, 0);
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginTop)) {
			expandedSubtitleMarginTop = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginTop, 0);
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginBottom)) {
			expandedTitleMarginBottom = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleMarginBottom, 0);
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginBottom)) {
			expandedSubtitleMarginBottom = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleMarginBottom, 0);
		}

		collapsingTitleEnabled = a.getBoolean(R.styleable.SuperpoweredCollapsingToolbarLayout_titleEnabled, true);
		collapsingSubtitleEnabled = a.getBoolean(R.styleable.SuperpoweredCollapsingToolbarLayout_subtitleEnabled, true);

		setTitle(a.getText(R.styleable.SuperpoweredCollapsingToolbarLayout_title));
		setSubtitle(a.getText(R.styleable.SuperpoweredCollapsingToolbarLayout_subtitle));

		setMultiline(a.getBoolean(R.styleable.SuperpoweredCollapsingToolbarLayout_multiline, false));

		// First load the default text appearances
		collapsingTextHelper.setExpandedTitleAppearance(R.style.TextAppearance_Design_SuperpoweredCollapsingToolbar_ExpandedTitle);
		collapsingTextHelper.setExpandedSubtitleAppearance(R.style.TextAppearance_Design_SuperpoweredCollapsingToolbar_ExpandedSubtitle);
		collapsingTextHelper.setCollapsedTitleAppearance(R.style.TextAppearance_Design_SuperpoweredCollapsingToolbar_CollapsedTitle);
		collapsingTextHelper.setCollapsedSubtitleAppearance(R.style.TextAppearance_Design_SuperpoweredCollapsingToolbar_CollapsedSubtitle);

		// Now overlay any custom text appearances
		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleTextAppearance)) {
			collapsingTextHelper.setExpandedTitleAppearance(a.getResourceId(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedTitleTextAppearance, 0));
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleTextAppearance)) {
			collapsingTextHelper.setExpandedSubtitleAppearance(a.getResourceId(R.styleable.SuperpoweredCollapsingToolbarLayout_expandedSubtitleTextAppearance, 0));
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_collapsedTitleTextAppearance)) {
			collapsingTextHelper.setCollapsedTitleAppearance(a.getResourceId(R.styleable.SuperpoweredCollapsingToolbarLayout_collapsedTitleTextAppearance, 0));
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_collapsedSubtitleTextAppearance)) {
			collapsingTextHelper.setCollapsedSubtitleAppearance(a.getResourceId(R.styleable.SuperpoweredCollapsingToolbarLayout_collapsedSubtitleTextAppearance, 0));
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_maxLines)) {
			collapsingTextHelper.setMaxLines(a.getInt(R.styleable.SuperpoweredCollapsingToolbarLayout_maxLines, 1));
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_lineSpacingExtra)) {
			collapsingTextHelper.setLineSpacingExtra(a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_lineSpacingExtra, 0));
		}

		if (a.hasValue(R.styleable.SuperpoweredCollapsingToolbarLayout_lineSpacingMultiplier)) {
			collapsingTextHelper.setLineSpacingMultiplier(a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_lineSpacingMultiplier, 1));
		}

		scrimVisibleHeightTrigger = a.getDimensionPixelSize(R.styleable.SuperpoweredCollapsingToolbarLayout_scrimVisibleHeightTrigger, -1);

		scrimAnimationDuration = a.getInt(
				R.styleable.SuperpoweredCollapsingToolbarLayout_scrimAnimationDuration,
				DEFAULT_SCRIM_ANIMATION_DURATION
		);

		setContentScrim(a.getDrawable(R.styleable.SuperpoweredCollapsingToolbarLayout_contentScrim));
		setStatusBarScrim(a.getDrawable(R.styleable.SuperpoweredCollapsingToolbarLayout_statusBarScrim));

		toolbarId = a.getResourceId(R.styleable.SuperpoweredCollapsingToolbarLayout_toolbarId, -1);

		a.recycle();

		setWillNotDraw(false);

		ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> onWindowInsetChanged(insets));
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		// Add an OnOffsetChangedListener if possible
		ViewParent parent = getParent();
		if (parent instanceof AppBarLayout) {
			// Copy over from the ABL whether we should fit system windows
			setFitsSystemWindows(ViewCompat.getFitsSystemWindows((View) parent));

			if (onOffsetChangedListener == null) {
				onOffsetChangedListener = new OffsetUpdateListener();
			}

			((AppBarLayout) parent).addOnOffsetChangedListener(onOffsetChangedListener);

			// We're attached, so lets request an inset dispatch
			ViewCompat.requestApplyInsets(this);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		// Remove our OnOffsetChangedListener if possible and it exists
		ViewParent parent = getParent();
		if (onOffsetChangedListener != null && parent instanceof AppBarLayout) {
			((AppBarLayout) parent).removeOnOffsetChangedListener(onOffsetChangedListener);
		}

		super.onDetachedFromWindow();
	}

	WindowInsetsCompat onWindowInsetChanged(WindowInsetsCompat insets) {
		WindowInsetsCompat newInsets = null;

		if (ViewCompat.getFitsSystemWindows(this)) {
			// If we're set to fit system windows, keep the insets
			newInsets = insets;
		}

		// If our insets have changed, keep them and invalidate the scroll ranges...
		if (!ObjectsCompat.equals(lastInsets, newInsets)) {
			lastInsets = newInsets;
			requestLayout();
		}

		// Consume the insets. This is done so that child views with fitSystemWindows=true do not
		// get the default padding functionality from View
		return insets.consumeSystemWindowInsets();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		super.draw(canvas);

		// If we don't have a toolbar, the scrim will be not be drawn in drawChild() below.
		// Instead, we draw it here, before our collapsing text.
		ensureToolbar();
		if (toolbar == null && contentScrim != null && scrimAlpha > 0) {
			contentScrim.mutate().setAlpha(scrimAlpha);
			contentScrim.draw(canvas);
		}

		// Let the collapsing text helper draw its text
		if (collapsingTitleEnabled && drawCollapsingTitle) {
			// TODO: implement non-multiline drawing
			if (multiline) {
				collapsingTextHelper.draw(canvas);
			} else {
				collapsingTextHelper.draw(canvas);
			}
		}

		// Now draw the status bar scrim
		if (statusBarScrim != null && scrimAlpha > 0) {
			int topInset = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;
			if (topInset > 0) {
				statusBarScrim.setBounds(0, -currentOffset, getWidth(), topInset - currentOffset);
				statusBarScrim.mutate().setAlpha(scrimAlpha);
				statusBarScrim.draw(canvas);
			}
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		// This is a little weird. Our scrim needs to be behind the Toolbar (if it is present),
		// but in front of any other children which are behind it. To do this we intercept the
		// drawChild() call, and draw our scrim just before the Toolbar is drawn
		boolean invalidated = false;

		if (contentScrim != null && scrimAlpha > 0 && isToolbarChild(child)) {
			contentScrim.mutate().setAlpha(scrimAlpha);
			contentScrim.draw(canvas);
			invalidated = true;
		}

		return super.drawChild(canvas, child, drawingTime) || invalidated;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		if (contentScrim != null) {
			contentScrim.setBounds(0, 0, w, h);
		}
	}

	private void ensureToolbar() {
		if (!refreshToolbar) {
			return;
		}

		// First clear out the current Toolbar
		toolbar = null;
		toolbarDirectChild = null;

		if (toolbarId != -1) {
			// If we have an ID set, try and find it and it's direct parent to us
			toolbar = findViewById(toolbarId);

			if (toolbar != null) {
				toolbarDirectChild = findDirectChild(toolbar);
			}
		}

		if (toolbar == null) {
			// If we don't have an ID, or couldn't find a Toolbar with the correct ID, try and find
			// one from our direct children
			Toolbar toolbar = null;

			for (int i = 0, count = getChildCount(); i < count; i++) {
				View child = getChildAt(i);

				if (child instanceof Toolbar) {
					toolbar = (Toolbar) child;
					break;
				}
			}

			this.toolbar = toolbar;
		}

		updateDummyView();
		refreshToolbar = false;
	}

	private boolean isToolbarChild(View child) {
		return (toolbarDirectChild == null || toolbarDirectChild == this)
				? child == toolbar
				: child == toolbarDirectChild;
	}

	/**
	 * Returns the direct child of this layout, which itself is the ancestor of the given view.
	 */
	@NonNull
	private View findDirectChild(View descendant) {
		View directChild = descendant;

		for (ViewParent p = descendant.getParent(); p != this && p != null; p = p.getParent()) {
			if (p instanceof View) {
				directChild = (View) p;
			}
		}

		return directChild;
	}

	private void updateDummyView() {
		if (!collapsingTitleEnabled && dummyView != null) {
			// If we have a dummy view and we have our title disabled, remove it from its parent
			ViewParent parent = dummyView.getParent();

			if (parent instanceof ViewGroup) {
				((ViewGroup) parent).removeView(dummyView);
			}
		}

		if (collapsingTitleEnabled && toolbar != null) {
			if (dummyView == null) {
				dummyView = new View(getContext());
			}

			if (dummyView.getParent() == null) {
				toolbar.addView(dummyView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		ensureToolbar();
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int mode = MeasureSpec.getMode(heightMeasureSpec);
		int topInset = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;
		if (mode == MeasureSpec.UNSPECIFIED && topInset > 0) {
			// If we have a top inset and we're set to wrap_content height we need to make sure
			// we add the top inset to our height, therefore we re-measure
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() + topInset, MeasureSpec.EXACTLY);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if (lastInsets != null) {
			// Shift down any views which are not set to fit system windows
			int insetTop = lastInsets.getSystemWindowInsetTop();
			for (int i = 0, z = getChildCount(); i < z; i++) {
				View child = getChildAt(i);

				if (!ViewCompat.getFitsSystemWindows(child)) {
					if (child.getTop() < insetTop) {
						// If the child isn't set to fit system windows but is drawing within
						// the inset offset it down
						ViewCompat.offsetTopAndBottom(child, insetTop);
					}
				}
			}
		}

		// Update our child view offset helpers so that they track the correct layout coordinates
		for (int i = 0, z = getChildCount(); i < z; i++) {
			getViewOffsetHelper(getChildAt(i)).onViewLayout();
		}

		// Update the collapsed bounds by getting its transformed bounds
		if (collapsingTitleEnabled && dummyView != null) {
			// We only draw the title if the dummy view is being displayed (Toolbar removes
			// views if there is no space)
			drawCollapsingTitle = ViewCompat.isAttachedToWindow(dummyView) && dummyView.getVisibility() == VISIBLE;

			if (drawCollapsingTitle) {
				boolean isRtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;

				// Update the collapsed bounds
				int maxOffset = getMaxOffsetForPinChild(toolbarDirectChild != null ? toolbarDirectChild : toolbar);

				DescendantOffsetUtils.getDescendantRect(this, dummyView, tmpRect);

				collapsingTextHelper.setCollapsedBounds(
						tmpRect.left + (isRtl ? toolbar.getTitleMarginEnd() : toolbar.getTitleMarginStart()),
						tmpRect.top + maxOffset + toolbar.getTitleMarginTop(),
						tmpRect.right + (isRtl ? toolbar.getTitleMarginStart() : toolbar.getTitleMarginEnd()),
						tmpRect.bottom + maxOffset - toolbar.getTitleMarginBottom()
				);

				// Update the expanded title bounds
				collapsingTextHelper.setExpandedTitleBounds(
						isRtl ? expandedTitleMarginEnd : expandedTitleMarginStart,
						tmpRect.top + expandedTitleMarginTop,
						right - left - (isRtl ? expandedTitleMarginStart : expandedTitleMarginEnd),
						bottom - top - expandedTitleMarginBottom
				);

				// Update the expanded subtitle bounds
				collapsingTextHelper.setExpandedSubtitleBounds(
						isRtl ? expandedSubtitleMarginEnd : expandedSubtitleMarginStart,
						tmpRect.top + expandedSubtitleMarginTop,
						right - left - (isRtl ? expandedSubtitleMarginStart : expandedSubtitleMarginEnd),
						bottom - top - expandedSubtitleMarginBottom
				);

				// Now recalculate using the new bounds
				collapsingTextHelper.recalculate();
			}
		}

		// Set our minimum height to enable proper AppBarLayout collapsing
		if (toolbar != null) {
			if (collapsingTitleEnabled && TextUtils.isEmpty(collapsingTextHelper.getTitle())) {
				// If we do not currently have a title, try and grab it from the Toolbar
				setTitle(toolbar.getTitle());
			}

			if (collapsingSubtitleEnabled && TextUtils.isEmpty(collapsingTextHelper.getSubtitle())) {
				// If we do not currently have a subtitle, try and grab it from the Toolbar
				setSubtitle(toolbar.getSubtitle());
			}

			if (toolbarDirectChild == null || toolbarDirectChild == this) {
				setMinimumHeight(getHeightWithMargins(toolbar));
			} else {
				setMinimumHeight(getHeightWithMargins(toolbarDirectChild));
			}
		}

		updateScrimVisibility();

		// Apply any view offsets, this should be done at the very end of layout
		for (int i = 0, z = getChildCount(); i < z; i++) {
			getViewOffsetHelper(getChildAt(i)).applyOffsets();
		}
	}

	private static int getHeightWithMargins(@NonNull View view) {
		ViewGroup.LayoutParams lp = view.getLayoutParams();

		if (lp instanceof MarginLayoutParams) {
			MarginLayoutParams mlp = (MarginLayoutParams) lp;
			return view.getHeight() + mlp.topMargin + mlp.bottomMargin;
		}

		return view.getHeight();
	}

	static ViewOffsetHelper getViewOffsetHelper(View view) {
		ViewOffsetHelper offsetHelper = (ViewOffsetHelper) view.getTag(R.id.view_offset_helper);

		if (offsetHelper == null) {
			offsetHelper = new ViewOffsetHelper(view);
			view.setTag(R.id.view_offset_helper, offsetHelper);
		}

		return offsetHelper;
	}

	/**
	 * Sets the title to be displayed by this view, if enabled.
	 *
	 * @see #setTitleEnabled(boolean)
	 * @see #getTitle()
	 */
	public void setTitle(@Nullable CharSequence title) {
		collapsingTextHelper.setTitle(title);
		updateContentDescriptionFromTitle();
	}

	/**
	 * Sets the subtitle to be displayed by this view, if enabled.
	 *
	 * @see #setSubtitleEnabled(boolean)
	 * @see #getSubtitle()
	 */
	public void setSubtitle(@Nullable CharSequence subtitle) {
		collapsingTextHelper.setSubtitle(subtitle);
		updateContentDescriptionFromSubtitle();
	}

	/**
	 * Returns the title currently being displayed by this view. If the title is not enabled, then
	 * this will return {@code null}.
	 */
	@Nullable
	public CharSequence getTitle() {
		return collapsingTitleEnabled ? collapsingTextHelper.getTitle() : null;
	}

	/**
	 * Returns the subtitle currently being displayed by this view. If the subtitle is not enabled, then
	 * this will return {@code null}.
	 */
	@Nullable
	public CharSequence getSubtitle() {
		return collapsingSubtitleEnabled ? collapsingTextHelper.getSubtitle() : null;
	}

	/**
	 * Sets whether this view should display its own title.
	 * <p>
	 * <p>The title displayed by this view will shrink and grow based on the scroll offset.
	 *
	 * @see #setTitle(CharSequence)
	 * @see #isTitleEnabled()
	 */
	public void setTitleEnabled(boolean enabled) {
		if (enabled != collapsingTitleEnabled) {
			collapsingTitleEnabled = enabled;
			updateContentDescriptionFromTitle();
			updateDummyView();
			requestLayout();
		}
	}

	/**
	 * Sets whether this view should display its own subtitle.
	 * <p>
	 * <p>The subtitle displayed by this view will shrink and grow based on the scroll offset.
	 *
	 * @see #setSubtitle(CharSequence)
	 * @see #isSubtitleEnabled()
	 */
	public void setSubtitleEnabled(boolean enabled) {
		if (enabled != collapsingSubtitleEnabled) {
			collapsingSubtitleEnabled = enabled;
			updateContentDescriptionFromSubtitle();
			updateDummyView();
			requestLayout();
		}
	}

	/**
	 * Returns whether this view is currently displaying its own title.
	 *
	 * @see #setTitleEnabled(boolean)
	 */
	public boolean isTitleEnabled() {
		return collapsingTitleEnabled;
	}

	/**
	 * Returns whether this view is currently displaying its own subtitle.
	 *
	 * @see #setSubtitleEnabled(boolean)
	 */
	public boolean isSubtitleEnabled() {
		return collapsingSubtitleEnabled;
	}

	/**
	 * Set whether the content scrim and/or status bar scrim should be shown or not. Any change in the
	 * vertical scroll may overwrite this value. Any visibility change will be animated if this view
	 * has already been laid out.
	 *
	 * @param shown whether the scrims should be shown
	 *
	 * @see #getStatusBarScrim()
	 * @see #getContentScrim()
	 */
	public void setScrimsShown(boolean shown) {
		setScrimsShown(shown, ViewCompat.isLaidOut(this) && !isInEditMode());
	}

	/**
	 * Set whether the content scrim and/or status bar scrim should be shown or not. Any change in the
	 * vertical scroll may overwrite this value.
	 *
	 * @param shown   whether the scrims should be shown
	 * @param animate whether to animate the visibility change
	 *
	 * @see #getStatusBarScrim()
	 * @see #getContentScrim()
	 */
	public void setScrimsShown(boolean shown, boolean animate) {
		if (scrimsAreShown != shown) {
			if (animate) {
				animateScrim(shown ? 0xFF : 0x0);
			} else {
				setScrimAlpha(shown ? 0xFF : 0x0);
			}

			scrimsAreShown = shown;
		}
	}

	private void animateScrim(int targetAlpha) {
		ensureToolbar();

		if (scrimAnimator == null) {
			scrimAnimator = new ValueAnimator();
			scrimAnimator.setDuration(scrimAnimationDuration);
			scrimAnimator.setInterpolator(targetAlpha > scrimAlpha
					? AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
					: AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
			);
			scrimAnimator.addUpdateListener(animator -> setScrimAlpha((int) animator.getAnimatedValue()));
		} else if (scrimAnimator.isRunning()) {
			scrimAnimator.cancel();
		}

		scrimAnimator.setIntValues(scrimAlpha, targetAlpha);
		scrimAnimator.start();
	}

	void setScrimAlpha(int alpha) {
		if (alpha != scrimAlpha) {
			Drawable contentScrim = this.contentScrim;
			if (contentScrim != null && toolbar != null) {
				ViewCompat.postInvalidateOnAnimation(toolbar);
			}

			scrimAlpha = alpha;
			ViewCompat.postInvalidateOnAnimation(SuperpoweredCollapsingToolbarLayout.this);
		}
	}

	int getScrimAlpha() {
		return scrimAlpha;
	}

	/**
	 * Set the drawable to use for the content scrim from resources. Providing null will disable the
	 * scrim functionality.
	 *
	 * @param drawable the drawable to display
	 *
	 * @see #getContentScrim()
	 */
	public void setContentScrim(@Nullable Drawable drawable) {
		if (contentScrim != drawable) {
			if (contentScrim != null) {
				contentScrim.setCallback(null);
			}

			contentScrim = drawable != null ? drawable.mutate() : null;
			if (contentScrim != null) {
				contentScrim.setBounds(0, 0, getWidth(), getHeight());
				contentScrim.setCallback(this);
				contentScrim.setAlpha(scrimAlpha);
			}

			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	/**
	 * Set the color to use for the content scrim.
	 *
	 * @param color the color to display
	 *
	 * @see #getContentScrim()
	 */
	public void setContentScrimColor(@ColorInt int color) {
		setContentScrim(new ColorDrawable(color));
	}

	/**
	 * Set the drawable to use for the content scrim from resources.
	 *
	 * @param resId drawable resource id
	 *
	 * @see #getContentScrim()
	 */
	public void setContentScrimResource(@DrawableRes int resId) {
		setContentScrim(ContextCompat.getDrawable(getContext(), resId));
	}

	/**
	 * Returns the drawable which is used for the foreground scrim.
	 *
	 * @see #setContentScrim(Drawable)
	 */
	@Nullable
	public Drawable getContentScrim() {
		return contentScrim;
	}

	/**
	 * Set the drawable to use for the status bar scrim from resources. Providing null will disable
	 * the scrim functionality.
	 * <p>
	 * <p>This scrim is only shown when we have been given a top system inset.
	 *
	 * @param drawable the drawable to display
	 *
	 * @see #getStatusBarScrim()
	 */
	public void setStatusBarScrim(@Nullable Drawable drawable) {
		if (statusBarScrim != drawable) {
			if (statusBarScrim != null) {
				statusBarScrim.setCallback(null);
			}

			statusBarScrim = drawable != null ? drawable.mutate() : null;

			if (statusBarScrim != null) {
				if (statusBarScrim.isStateful()) {
					statusBarScrim.setState(getDrawableState());
				}

				DrawableCompat.setLayoutDirection(statusBarScrim, ViewCompat.getLayoutDirection(this));
				statusBarScrim.setVisible(getVisibility() == VISIBLE, false);
				statusBarScrim.setCallback(this);
				statusBarScrim.setAlpha(scrimAlpha);
			}

			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();

		int[] state = getDrawableState();
		boolean changed = false;

		Drawable d = statusBarScrim;
		if (d != null && d.isStateful()) {
			//noinspection ConstantConditions
			changed |= d.setState(state);
		}
		d = contentScrim;

		if (d != null && d.isStateful()) {
			changed |= d.setState(state);
		}

		//noinspection ConstantConditions
		if (collapsingTextHelper != null) {
			changed |= collapsingTextHelper.setState(state);
		}

		if (changed) {
			invalidate();
		}
	}

	@Override
	protected boolean verifyDrawable(@NotNull Drawable who) {
		return super.verifyDrawable(who) || who == contentScrim || who == statusBarScrim;
	}

	@Override
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);

		boolean visible = visibility == VISIBLE;
		if (statusBarScrim != null && statusBarScrim.isVisible() != visible) {
			statusBarScrim.setVisible(visible, false);
		}

		if (contentScrim != null && contentScrim.isVisible() != visible) {
			contentScrim.setVisible(visible, false);
		}
	}

	/**
	 * Set the color to use for the status bar scrim.
	 * <p>
	 * <p>This scrim is only shown when we have been given a top system inset.
	 *
	 * @param color the color to display
	 *
	 * @see #getStatusBarScrim()
	 */
	public void setStatusBarScrimColor(@ColorInt int color) {
		setStatusBarScrim(new ColorDrawable(color));
	}

	/**
	 * Set the drawable to use for the content scrim from resources.
	 *
	 * @param resId drawable resource id
	 *
	 * @see #getStatusBarScrim()
	 */
	public void setStatusBarScrimResource(@DrawableRes int resId) {
		setStatusBarScrim(ContextCompat.getDrawable(getContext(), resId));
	}

	/**
	 * Returns the drawable which is used for the status bar scrim.
	 *
	 * @see #setStatusBarScrim(Drawable)
	 */
	@Nullable
	public Drawable getStatusBarScrim() {
		return statusBarScrim;
	}

	/**
	 * Sets the text color and size for the expanded title from the specified TextAppearance resource.
	 */
	public void setExpandedTitleTextAppearance(@StyleRes int resId) {
		collapsingTextHelper.setExpandedTitleAppearance(resId);
	}

	/**
	 * Sets the text color and size for the expanded subtitle from the specified TextAppearance resource.
	 */
	public void setExpandedSubtitleTextAppearance(@StyleRes int resId) {
		collapsingTextHelper.setExpandedSubtitleAppearance(resId);
	}

	/**
	 * Sets the text color and size for the collapsed title from the specified TextAppearance
	 * resource.
	 */
	public void setCollapsedTitleTextAppearance(@StyleRes int resId) {
		collapsingTextHelper.setCollapsedTitleAppearance(resId);
	}

	/**
	 * Sets the text color and size for the collapsed title from the specified TextAppearance
	 * resource.
	 */
	public void setCollapsedSubtitleTextAppearance(@StyleRes int resId) {
		collapsingTextHelper.setCollapsedSubtitleAppearance(resId);
	}

	/**
	 * Sets the text color of the expanded title.
	 *
	 * @param color The new text color in ARGB format
	 */
	public void setExpandedTitleTextColor(@ColorInt int color) {
		setExpandedTitleTextColor(ColorStateList.valueOf(color));
	}

	/**
	 * Sets the text color of the expanded title.
	 *
	 * @param color The new text color in ARGB format
	 */
	public void setExpandedSubtitleTextColor(@ColorInt int color) {
		setExpandedSubtitleTextColor(ColorStateList.valueOf(color));
	}

	/**
	 * Sets the text colors of the expanded title.
	 *
	 * @param colors ColorStateList containing the new text colors
	 */
	public void setExpandedTitleTextColor(@NonNull ColorStateList colors) {
		collapsingTextHelper.setExpandedTitleColor(colors);
	}

	/**
	 * Sets the text colors of the expanded title.
	 *
	 * @param colors ColorStateList containing the new text colors
	 */
	public void setExpandedSubtitleTextColor(@NonNull ColorStateList colors) {
		collapsingTextHelper.setExpandedSubtitleColor(colors);
	}

	/**
	 * Sets the text color of the collapsed title.
	 *
	 * @param color The new text color in ARGB format
	 */
	public void setCollapsedTitleTextColor(@ColorInt int color) {
		setCollapsedTitleTextColor(ColorStateList.valueOf(color));
	}

	/**
	 * Sets the text color of the collapsed title.
	 *
	 * @param color The new text color in ARGB format
	 */
	public void setCollapsedSubtitleTextColor(@ColorInt int color) {
		setCollapsedSubtitleTextColor(ColorStateList.valueOf(color));
	}

	/**
	 * Sets the text colors of the collapsed title.
	 *
	 * @param colors ColorStateList containing the new text colors
	 */
	public void setCollapsedTitleTextColor(@NonNull ColorStateList colors) {
		collapsingTextHelper.setCollapsedTitleColor(colors);
	}

	/**
	 * Sets the text colors of the collapsed title.
	 *
	 * @param colors ColorStateList containing the new text colors
	 */
	public void setCollapsedSubtitleTextColor(@NonNull ColorStateList colors) {
		collapsingTextHelper.setCollapsedSubtitleColor(colors);
	}

	/**
	 * Sets the horizontal alignment of the expanded title and the vertical gravity that will be used
	 * when there is extra space in the expanded bounds beyond what is required for the title itself.
	 */
	public void setExpandedTitleGravity(int gravity) {
		collapsingTextHelper.setExpandedTitleGravity(gravity);
	}

	/**
	 * Sets the horizontal alignment of the expanded subtitle and the vertical gravity that will be used
	 * when there is extra space in the expanded bounds beyond what is required for the subtitle itself.
	 */
	public void setExpandedSubtitleGravity(int gravity) {
		collapsingTextHelper.setExpandedSubtitleGravity(gravity);
	}

	/**
	 * Returns the horizontal and vertical alignment for title when expanded.
	 */
	public int getExpandedTitleGravity() {
		return collapsingTextHelper.getExpandedTitleGravity();
	}

	/**
	 * Returns the horizontal and vertical alignment for subtitle when expanded.
	 */
	public int getExpandedSubtitleGravity() {
		return collapsingTextHelper.getExpandedSubtitleGravity();
	}

	/**
	 * Sets the horizontal alignment of the collapsed title and the vertical gravity that will be used
	 * when there is extra space in the collapsed bounds beyond what is required for the title itself.
	 */
	public void setCollapsedTitleGravity(int gravity) {
		collapsingTextHelper.setCollapsedTitleGravity(gravity);
	}

	/**
	 * Sets the horizontal alignment of the collapsed subtitle and the vertical gravity that will be used
	 * when there is extra space in the collapsed bounds beyond what is required for the subtitle itself.
	 */
	public void setCollapsedSubtitleGravity(int gravity) {
		collapsingTextHelper.setCollapsedSubtitleGravity(gravity);
	}

	/**
	 * Returns the horizontal and vertical alignment for title when collapsed.
	 */
	public int getCollapsedTitleGravity() {
		return collapsingTextHelper.getCollapsedTitleGravity();
	}

	/**
	 * Returns the horizontal and vertical alignment for subtitle when collapsed.
	 */
	public int getCollapsedSubtitleGravity() {
		return collapsingTextHelper.getCollapsedSubtitleGravity();
	}

	/**
	 * Set the typeface to use for the expanded title.
	 *
	 * @param typeface typeface to use, or {@code null} to use the default.
	 */
	public void setExpandedTitleTypeface(@Nullable Typeface typeface) {
		collapsingTextHelper.setExpandedTitleTypeface(typeface);
	}

	/**
	 * Returns the typeface used for the expanded title.
	 */
	@NonNull
	public Typeface getExpandedTitleTypeface() {
		return collapsingTextHelper.getExpandedTitleTypeface();
	}

	/**
	 * Set the typeface to use for the expanded subtitle.
	 *
	 * @param typeface typeface to use, or {@code null} to use the default.
	 */
	public void setExpandedSubtitleTypeface(@Nullable Typeface typeface) {
		collapsingTextHelper.setExpandedSubtitleTypeface(typeface);
	}

	/**
	 * Returns the typeface used for the expanded subtitle.
	 */
	@NonNull
	public Typeface getExpandedSubtitleTypeface() {
		return collapsingTextHelper.getExpandedSubtitleTypeface();
	}

	/**
	 * Set the typeface to use for the collapsed title.
	 *
	 * @param typeface typeface to use, or {@code null} to use the default.
	 */
	public void setCollapsedTitleTypeface(@Nullable Typeface typeface) {
		collapsingTextHelper.setCollapsedTitleTypeface(typeface);
	}

	/**
	 * Set the typeface to use for the collapsed subtitle.
	 *
	 * @param typeface typeface to use, or {@code null} to use the default.
	 */
	public void setCollapsedSubtitleTypeface(@Nullable Typeface typeface) {
		collapsingTextHelper.setCollapsedSubtitleTypeface(typeface);
	}

	/**
	 * Returns the typeface used for the collapsed title.
	 */
	@NonNull
	public Typeface getCollapsedTitleTypeface() {
		return collapsingTextHelper.getCollapsedTitleTypeface();
	}

	/**
	 * Returns the typeface used for the collapsed subtitle.
	 */
	@NonNull
	public Typeface getCollapsedSubtitleTypeface() {
		return collapsingTextHelper.getCollapsedSubtitleTypeface();
	}

	/**
	 * Sets the expanded title margins.
	 *
	 * @param start  the starting title margin in pixels
	 * @param top    the top title margin in pixels
	 * @param end    the ending title margin in pixels
	 * @param bottom the bottom title margin in pixels
	 *
	 * @see #getExpandedTitleMarginStart()
	 * @see #getExpandedTitleMarginTop()
	 * @see #getExpandedTitleMarginEnd()
	 * @see #getExpandedTitleMarginBottom()
	 */
	public void setExpandedTitleMargin(int start, int top, int end, int bottom) {
		expandedTitleMarginStart = start;
		expandedTitleMarginTop = top;
		expandedTitleMarginEnd = end;
		expandedTitleMarginBottom = bottom;
		requestLayout();
	}

	/**
	 * Sets the expanded subtitle margins.
	 *
	 * @param start  the starting subtitle margin in pixels
	 * @param top    the top subtitle margin in pixels
	 * @param end    the ending subtitle margin in pixels
	 * @param bottom the bottom subtitle margin in pixels
	 *
	 * @see #getExpandedSubtitleMarginStart()
	 * @see #getExpandedSubtitleMarginTop()
	 * @see #getExpandedSubtitleMarginEnd()
	 * @see #getExpandedSubtitleMarginBottom()
	 */
	public void setExpandedSubtitleMargin(int start, int top, int end, int bottom) {
		expandedSubtitleMarginStart = start;
		expandedSubtitleMarginTop = top;
		expandedSubtitleMarginEnd = end;
		expandedSubtitleMarginBottom = bottom;
		requestLayout();
	}

	/**
	 * Sets the starting expanded title margin in pixels.
	 *
	 * @param margin the starting title margin in pixels
	 *
	 * @see #getExpandedTitleMarginStart()
	 */
	public void setExpandedTitleMarginStart(int margin) {
		expandedTitleMarginStart = margin;
		requestLayout();
	}

	/**
	 * Sets the starting expanded subtitle margin in pixels.
	 *
	 * @param margin the starting subtitle margin in pixels
	 *
	 * @see #getExpandedSubtitleMarginStart()
	 */
	public void setExpandedSubtitleMarginStart(int margin) {
		expandedSubtitleMarginStart = margin;
		requestLayout();
	}

	/**
	 * @return the starting expanded title margin in pixels
	 *
	 * @see #setExpandedTitleMarginStart(int)
	 */
	public int getExpandedTitleMarginStart() {
		return expandedTitleMarginStart;
	}

	/**
	 * @return the starting expanded subtitle margin in pixels
	 *
	 * @see #setExpandedSubtitleMarginStart(int)
	 */
	public int getExpandedSubtitleMarginStart() {
		return expandedSubtitleMarginStart;
	}

	/**
	 * Sets the top expanded title margin in pixels.
	 *
	 * @param margin the top title margin in pixels
	 *
	 * @see #getExpandedTitleMarginTop()
	 */
	public void setExpandedTitleMarginTop(int margin) {
		expandedTitleMarginTop = margin;
		requestLayout();
	}

	/**
	 * Sets the top expanded subtitle margin in pixels.
	 *
	 * @param margin the top subtitle margin in pixels
	 *
	 * @see #getExpandedSubtitleMarginTop()
	 */
	public void setExpandedSubtitleMarginTop(int margin) {
		expandedSubtitleMarginTop = margin;
		requestLayout();
	}

	/**
	 * @return the top expanded title margin in pixels
	 *
	 * @see #setExpandedTitleMarginTop(int)
	 */
	public int getExpandedTitleMarginTop() {
		return expandedTitleMarginTop;
	}

	/**
	 * @return the top expanded subtitle margin in pixels
	 *
	 * @see #setExpandedSubtitleMarginTop(int)
	 */
	public int getExpandedSubtitleMarginTop() {
		return expandedSubtitleMarginTop;
	}

	/**
	 * Sets the ending expanded title margin in pixels.
	 *
	 * @param margin the ending title margin in pixels
	 *
	 * @see #getExpandedTitleMarginEnd()
	 */
	public void setExpandedTitleMarginEnd(int margin) {
		expandedTitleMarginEnd = margin;
		requestLayout();
	}

	/**
	 * Sets the ending expanded subtitle margin in pixels.
	 *
	 * @param margin the ending subtitle margin in pixels
	 *
	 * @see #getExpandedSubtitleMarginEnd()
	 */
	public void setExpandedSubtitleMarginEnd(int margin) {
		expandedSubtitleMarginEnd = margin;
		requestLayout();
	}

	/**
	 * @return the ending expanded title margin in pixels
	 *
	 * @see #setExpandedTitleMarginEnd(int)
	 */
	public int getExpandedTitleMarginEnd() {
		return expandedTitleMarginEnd;
	}

	/**
	 * @return the ending expanded subtitle margin in pixels
	 *
	 * @see #setExpandedSubtitleMarginEnd(int)
	 */
	public int getExpandedSubtitleMarginEnd() {
		return expandedSubtitleMarginEnd;
	}

	/**
	 * Sets the bottom expanded title margin in pixels.
	 *
	 * @param margin the bottom title margin in pixels
	 *
	 * @see #getExpandedTitleMarginBottom()
	 */
	public void setExpandedTitleMarginBottom(int margin) {
		expandedTitleMarginBottom = margin;
		requestLayout();
	}

	/**
	 * Sets the bottom expanded subtitle margin in pixels.
	 *
	 * @param margin the bottom subtitle margin in pixels
	 *
	 * @see #getExpandedSubtitleMarginBottom()
	 */
	public void setExpandedSubtitleMarginBottom(int margin) {
		expandedSubtitleMarginBottom = margin;
		requestLayout();
	}

	/**
	 * @return the bottom expanded title margin in pixels
	 *
	 * @see #setExpandedTitleMarginBottom(int)
	 */
	public int getExpandedTitleMarginBottom() {
		return expandedTitleMarginBottom;
	}

	/**
	 * @return the bottom expanded subtitle margin in pixels
	 *
	 * @see #setExpandedSubtitleMarginBottom(int)
	 */
	public int getExpandedSubtitleMarginBottom() {
		return expandedSubtitleMarginBottom;
	}

	/**
	 * Enables support for multiple lines in the expanded state
	 */
	public void setMultiline(boolean multiline) {
		this.multiline = multiline;
	}

	/**
	 * Gets whether support for multiple lines in the expanded state is enabled
	 */
	public boolean isMultiline() {
		return multiline;
	}

	/**
	 * Sets the maximum number of lines to display in the expanded state.
	 * Requires {@link .setMultiline}(true) to take effect.
	 */
	public void setMaxLines(int maxLines) {
		collapsingTextHelper.setMaxLines(maxLines);
	}

	/**
	 * Gets the maximum number of lines to display in the expanded state.
	 */
	public int getMaxLines() {
		return collapsingTextHelper.getMaxLines();
	}

	/**
	 * Set line spacing extra applied to each line in the expanded state.
	 * Requires {@link .setMultiline}(true) to take effect.
	 */
	void setLineSpacingExtra(float lineSpacingExtra) {
		collapsingTextHelper.setLineSpacingExtra(lineSpacingExtra);
	}

	/**
	 * Gets the line spacing extra applied to each line in the expanded state.
	 */
	float getLineSpacingExtra() {
		return collapsingTextHelper.getLineSpacingExtra();
	}

	/**
	 * Sets the line spacing multiplier applied to each line in the expanded state.
	 * Requires {@link .setMultiline}(true) to take effect.
	 */
	void setLineSpacingMultiplier(float lineSpacingMultiplier) {
		collapsingTextHelper.setLineSpacingMultiplier(lineSpacingMultiplier);
	}

	/**
	 * Gets the line spacing multiplier applied to each line in the expanded state.
	 */
	float getLineSpacingMultiplier() {
		return collapsingTextHelper.getLineSpacingMultiplier();
	}

	/**
	 * Set the amount of visible height in pixels used to define when to trigger a scrim visibility
	 * change.
	 * <p>
	 * <p>If the visible height of this view is less than the given value, the scrims will be made
	 * visible, otherwise they are hidden.
	 *
	 * @param height value in pixels used to define when to trigger a scrim visibility change
	 */
	public void setScrimVisibleHeightTrigger(@IntRange(from = 0) int height) {
		if (scrimVisibleHeightTrigger != height) {
			scrimVisibleHeightTrigger = height;
			// Update the scrim visibility
			updateScrimVisibility();
		}
	}

	/**
	 * Returns the amount of visible height in pixels used to define when to trigger a scrim
	 * visibility change.
	 *
	 * @see #setScrimVisibleHeightTrigger(int)
	 */
	public int getScrimVisibleHeightTrigger() {
		if (scrimVisibleHeightTrigger >= 0) {
			// If we have one explicitly set, return it
			return scrimVisibleHeightTrigger;
		}

		// Otherwise we'll use the default computed value
		int insetTop = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;

		int minHeight = ViewCompat.getMinimumHeight(this);
		if (minHeight > 0) {
			// If we have a minHeight set, lets use 2 * minHeight (capped at our height)
			return Math.min((minHeight * 2) + insetTop, getHeight());
		}

		// If we reach here then we don't have a min height set. Instead we'll take a
		// guess at 1/3 of our height being visible
		return getHeight() / 3;
	}

	/**
	 * Set the duration used for scrim visibility animations.
	 *
	 * @param duration the duration to use in milliseconds
	 */
	public void setScrimAnimationDuration(@IntRange(from = 0) long duration) {
		scrimAnimationDuration = duration;
	}

	/**
	 * Returns the duration in milliseconds used for scrim visibility animations.
	 */
	public long getScrimAnimationDuration() {
		return scrimAnimationDuration;
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
	}

	@Override
	public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected FrameLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@SuppressWarnings("WeakerAccess")
	public static class LayoutParams extends CollapsingToolbarLayout.LayoutParams {
		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(int width, int height, int gravity) {
			super(width, height, gravity);
		}

		public LayoutParams(ViewGroup.LayoutParams p) {
			super(p);
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
		}

		@RequiresApi(19)
		public LayoutParams(FrameLayout.LayoutParams source) {
			super(source);
		}
	}

	/**
	 * Show or hide the scrims if needed
	 */
	final void updateScrimVisibility() {
		if (contentScrim != null || statusBarScrim != null) {
			setScrimsShown(getHeight() + currentOffset < getScrimVisibleHeightTrigger());
		}
	}

	final int getMaxOffsetForPinChild(View child) {
		ViewOffsetHelper offsetHelper = getViewOffsetHelper(child);
		LayoutParams lp = (LayoutParams) child.getLayoutParams();
		return getHeight() - offsetHelper.getLayoutTop() - child.getHeight() - lp.bottomMargin;
	}

	private void updateContentDescriptionFromTitle() {
		// Set this layout's contentDescription to match the title if it's shown by CollapsingTextHelper
		setContentDescription(getTitle());
	}

	private void updateContentDescriptionFromSubtitle() {
		// Set this layout's contentDescription to match the title - subtitle if it's shown by CollapsingTextHelper
		setContentDescription(getTitle() + " - " + getSubtitle());
	}

	private class OffsetUpdateListener implements AppBarLayout.OnOffsetChangedListener {
		OffsetUpdateListener() {
		}

		@Override
		public void onOffsetChanged(AppBarLayout layout, int verticalOffset) {
			currentOffset = verticalOffset;

			int insetTop = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;

			for (int i = 0, z = getChildCount(); i < z; i++) {
				View child = getChildAt(i);
				LayoutParams lp = (LayoutParams) child.getLayoutParams();
				ViewOffsetHelper offsetHelper = getViewOffsetHelper(child);

				switch (lp.getCollapseMode()) {
					case LayoutParams.COLLAPSE_MODE_PIN:
						offsetHelper.setTopAndBottomOffset(MathUtils.clamp(-verticalOffset, 0, getMaxOffsetForPinChild(child)));
						break;
					case LayoutParams.COLLAPSE_MODE_PARALLAX:
						offsetHelper.setTopAndBottomOffset(Math.round(-verticalOffset * lp.getParallaxMultiplier()));
						break;
					case LayoutParams.COLLAPSE_MODE_OFF:
					default:
						break;
				}
			}

			// Show or hide the scrims if needed
			updateScrimVisibility();

			if (statusBarScrim != null && insetTop > 0) {
				ViewCompat.postInvalidateOnAnimation(SuperpoweredCollapsingToolbarLayout.this);
			}

			// Update the collapsing text's fraction
			int expandRange = getHeight() - ViewCompat.getMinimumHeight(SuperpoweredCollapsingToolbarLayout.this) - insetTop;
			collapsingTextHelper.setExpansionFraction(Math.abs(verticalOffset) / (float) expandRange);
		}
	}
}
